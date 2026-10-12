package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.FrameCodec;
import com.panita.enriquecraft.discord.protocol.InFrame;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.Protocol;
import com.panita.enriquecraft.discord.transport.BridgeTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * The server's connection to the Panita Bot. It opens the line, says hello, waits for the welcome, keeps the line up (it
 * comes back by itself, a bit later each time it fails) and holds on to a few game lines while it is down. It never blocks
 * the server and never throws at it: the bridge being down only means the bridge is off. All state lives under one lock;
 * what the bot sends is handed to the consumer outside of it.
 */
public final class BridgeConnection implements FrameSink {

    public enum State {
        /** Nothing to connect with or to: disabled, no address, no key, or the server has not started. */
        OFF,
        CONNECTING,
        CONNECTED,
        /** The line is down and the next try is scheduled. */
        WAITING,
        /** The bot speaks another protocol; nothing is tried until the connection is asked to start again. */
        INCOMPATIBLE
    }

    /**
     * @param channels the chat channels the bot bridges, once connected
     * @param sent     frames sent to the bot
     * @param received frames received from the bot
     * @param dropped  frames that were thrown away: too old, no room, or invalid
     */
    public record Status(State state, boolean enabled, String url, boolean keyFound, Optional<Instant> since,
                         List<String> channels, long sent, long received, long dropped, Optional<String> lastError) {
    }

    private record Buffered(String text, Instant at) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeConnection.class);
    private static final Duration WATCH_EVERY = Duration.ofSeconds(30);
    /** The bot pings every 30 seconds, so a line that has been quiet this long is dead. */
    private static final Duration SILENCE_LIMIT = Duration.ofSeconds(90);
    private static final Duration BUFFER_FOR = Duration.ofSeconds(60);
    private static final int BUFFER_MAX = 100;

    private final BridgeTransport transport;
    private final Scheduler scheduler;
    private final Supplier<Instant> clock;
    private final DoubleSupplier random;
    private final BooleanSupplier enabled;
    private final Supplier<String> url;
    private final Supplier<Optional<String>> key;
    private final Supplier<Optional<OutFrame.Hello>> hello;
    private final Consumer<InFrame> frames;

    private final Object lock = new Object();
    private final Deque<Buffered> buffer = new ArrayDeque<>();
    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private State state = State.OFF;
    private int generation;
    private int attempt;
    private Instant since;
    private Instant lastActivity;
    private List<String> channels = List.of();
    private Optional<String> lastError = Optional.empty();
    private Scheduler.Handle retry;
    private Scheduler.Handle watchdog;
    private volatile CompletableFuture<Void> lastSend;

    /**
     * @param hello  what to say to the bot, empty while the server has not started
     * @param frames receives what the bot asks for; called on a network thread
     */
    public BridgeConnection(BridgeTransport transport, Scheduler scheduler, Supplier<Instant> clock, DoubleSupplier random,
                            BooleanSupplier enabled, Supplier<String> url, Supplier<Optional<String>> key,
                            Supplier<Optional<OutFrame.Hello>> hello, Consumer<InFrame> frames) {
        this.transport = transport;
        this.scheduler = scheduler;
        this.clock = clock;
        this.random = random;
        this.enabled = enabled;
        this.url = url;
        this.key = key;
        this.hello = hello;
        this.frames = frames;
    }

    /** Connects, unless it already is or is about to. Returns at once. */
    public void start() {
        synchronized (lock) {
            if (state == State.CONNECTING || state == State.CONNECTED || state == State.WAITING) {
                return;
            }
            begin();
        }
    }

    /** Drops the line and connects again at once, forgetting how often it failed. */
    public void reconnect() {
        synchronized (lock) {
            reset();
            attempt = 0;
            begin();
        }
    }

    /** Closes the line for good. Called when the server stops, after giving what was just sent a moment to get through. */
    public void stop() {
        CompletableFuture<Void> pending = lastSend;
        if (pending != null) {
            try {
                pending.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                // A send that fails or takes too long is not worth holding the server back for.
            }
        }
        synchronized (lock) {
            reset();
            buffer.clear();
            state = State.OFF;
        }
    }

    /**
     * Whether the bot bridges this chat channel, as far as it last said. It stays true while the line is down, so lines written
     * during a short break are held and not lost.
     */
    @Override
    public boolean bridges(String channel) {
        synchronized (lock) {
            return state != State.OFF && state != State.INCOMPATIBLE && channels.contains(channel);
        }
    }

    public boolean connected() {
        synchronized (lock) {
            return state == State.CONNECTED;
        }
    }

    public Status status() {
        synchronized (lock) {
            return new Status(state, enabled.getAsBoolean(), url.get(), key.get().isPresent(), Optional.ofNullable(since), channels,
                    sent.get(), received.get(), dropped.get(), lastError);
        }
    }

    /**
     * Sends a frame to the bot. Chat lines and events wait for a while if the line is down; anything else is dropped.
     */
    @Override
    public void send(OutFrame frame) {
        String text = FrameCodec.encode(frame);
        synchronized (lock) {
            if (state == State.CONNECTED) {
                transmit(text);
            } else if (frame instanceof OutFrame.Chat || frame instanceof OutFrame.Event) {
                buffer.addLast(new Buffered(text, clock.get()));
                while (buffer.size() > BUFFER_MAX) {
                    buffer.removeFirst();
                    dropped.incrementAndGet();
                }
            } else {
                dropped.incrementAndGet();
            }
        }
    }

    // Everything below runs with the lock held, except the listener, which takes it itself.

    private void reset() {
        generation++;
        cancel(retry);
        cancel(watchdog);
        retry = null;
        watchdog = null;
        since = null;
        channels = List.of();
        transport.close();
    }

    private static void cancel(Scheduler.Handle handle) {
        if (handle != null) {
            handle.cancel();
        }
    }

    private void begin() {
        Optional<String> secret = key.get();
        Optional<OutFrame.Hello> greeting = hello.get();
        String address = url.get();
        if (!enabled.getAsBoolean() || address.isBlank() || secret.isEmpty() || greeting.isEmpty()) {
            state = State.OFF;
            return;
        }
        URI uri;
        try {
            uri = URI.create(address);
        } catch (IllegalArgumentException e) {
            state = State.OFF;
            lastError = Optional.of("the address is not valid");
            return;
        }
        int mine = ++generation;
        state = State.CONNECTING;
        lastActivity = clock.get();
        transport.connect(uri, secret.get(), new Line(mine, greeting.get())).whenComplete((ok, error) -> {
            if (error != null) {
                lost(mine, "could not connect (" + error.getClass().getSimpleName() + ")");
            }
        });
    }

    private void transmit(String text) {
        CompletableFuture<Void> result = transport.send(text);
        lastSend = result;
        result.whenComplete((ok, error) -> (error == null ? sent : dropped).incrementAndGet());
    }

    private void lost(int mine, String reason) {
        synchronized (lock) {
            if (mine != generation) {
                return;
            }
            cancel(watchdog);
            watchdog = null;
            since = null;
            if (state != State.INCOMPATIBLE) {
                lastError = Optional.of(reason);
            }
            if (state == State.OFF || state == State.INCOMPATIBLE) {
                return;
            }
            state = State.WAITING;
            Duration wait = Backoff.delay(attempt++, random.getAsDouble());
            LOGGER.info("The bridge to the Panita Bot is down ({}); trying again in {} s", reason, wait.toSeconds());
            retry = scheduler.schedule(wait, this::retryNow);
        }
    }

    private void retryNow() {
        synchronized (lock) {
            if (state == State.WAITING) {
                begin();
            }
        }
    }

    private void watch(int mine) {
        synchronized (lock) {
            if (mine != generation) {
                return;
            }
            if (Duration.between(lastActivity, clock.get()).compareTo(SILENCE_LIMIT) > 0) {
                transport.close();
                lost(mine, "the bot stopped answering");
                return;
            }
            watchdog = scheduler.schedule(WATCH_EVERY, () -> watch(mine));
        }
    }

    private void welcome(int mine, InFrame.Welcome welcome) {
        synchronized (lock) {
            if (mine != generation) {
                return;
            }
            if (welcome.protocol() != Protocol.VERSION) {
                state = State.INCOMPATIBLE;
                lastError = Optional.of("the bot speaks protocol " + welcome.protocol() + " and this mod " + Protocol.VERSION);
                cancel(watchdog);
                transport.close();
                return;
            }
            state = State.CONNECTED;
            since = clock.get();
            channels = welcome.channels();
            attempt = 0;
            lastError = Optional.empty();
            LOGGER.info("Connected to the Panita Bot; it bridges {}", channels);
            flush();
        }
    }

    private void flush() {
        Instant now = clock.get();
        while (!buffer.isEmpty()) {
            Buffered waiting = buffer.removeFirst();
            if (Duration.between(waiting.at(), now).compareTo(BUFFER_FOR) > 0) {
                dropped.incrementAndGet();
            } else {
                transmit(waiting.text());
            }
        }
    }

    /** The callbacks of one attempt. Anything from an attempt that was replaced is ignored. */
    private final class Line implements BridgeTransport.Listener {

        private final int mine;
        private final OutFrame.Hello greeting;

        Line(int mine, OutFrame.Hello greeting) {
            this.mine = mine;
            this.greeting = greeting;
        }

        @Override
        public void onOpen() {
            synchronized (lock) {
                if (mine != generation) {
                    return;
                }
                lastActivity = clock.get();
                transmit(FrameCodec.encode(greeting));
                watchdog = scheduler.schedule(WATCH_EVERY, () -> watch(mine));
            }
        }

        @Override
        public void onText(String text) {
            synchronized (lock) {
                if (mine != generation) {
                    return;
                }
                lastActivity = clock.get();
            }
            FrameCodec.Decoded decoded = FrameCodec.decode(text);
            switch (decoded) {
                case FrameCodec.Decoded.Invalid invalid -> {
                    dropped.incrementAndGet();
                    synchronized (lock) {
                        lastError = Optional.of("the bot sent an invalid frame (" + invalid.reason() + ")");
                    }
                }
                case FrameCodec.Decoded.Ignored ignored -> {
                }
                case FrameCodec.Decoded.Frame frame -> {
                    received.incrementAndGet();
                    handle(frame.frame());
                }
            }
        }

        private void handle(InFrame frame) {
            switch (frame) {
                case InFrame.Welcome welcome -> welcome(mine, welcome);
                case InFrame.Error error -> {
                    synchronized (lock) {
                        lastError = Optional.of("the bot said: " + error.code());
                    }
                }
                case InFrame.Chat chat -> deliver(frame);
                case InFrame.Query query -> deliver(frame);
            }
        }

        private void deliver(InFrame frame) {
            synchronized (lock) {
                if (mine != generation || state != State.CONNECTED) {
                    return;
                }
            }
            try {
                frames.accept(frame);
            } catch (RuntimeException e) {
                LOGGER.error("Could not handle a frame of the bot", e);
            }
        }

        @Override
        public void onPing() {
            synchronized (lock) {
                if (mine == generation) {
                    lastActivity = clock.get();
                }
            }
        }

        @Override
        public void onClose(int code, String reason) {
            if (code == Protocol.CLOSE_PROTOCOL) {
                synchronized (lock) {
                    if (mine == generation) {
                        state = State.INCOMPATIBLE;
                        lastError = Optional.of("the bot refused the protocol");
                    }
                }
                lost(mine, "the bot refused the protocol");
            } else {
                lost(mine, "the bot closed the line (" + code + ")");
            }
        }

        @Override
        public void onError(Throwable error) {
            lost(mine, "the line failed (" + error.getClass().getSimpleName() + ")");
        }
    }
}
