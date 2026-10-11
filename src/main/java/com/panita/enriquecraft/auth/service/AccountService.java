package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthAccount;
import com.panita.enriquecraft.auth.data.AuthSession;
import com.panita.enriquecraft.auth.data.HeldState;
import com.panita.enriquecraft.auth.data.PinCredential;
import com.panita.enriquecraft.core.framework.data.SnbtStore;
import com.panita.enriquecraft.core.framework.data.WorldData;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * The accounts of players who log in with a PIN: their PIN, their remembered login, and where they
 * were while waiting to enter it. All changes are written to disk at once.
 */
public final class AccountService {

    private final SnbtStore<List<AuthAccount>> store;
    private final PinHasher hasher;
    private final Supplier<Instant> clock;

    public AccountService(WorldData worldData, PinHasher hasher, Supplier<Instant> clock) {
        this.store = worldData.register("auth/accounts.snbt", AuthAccount.CODEC.listOf(), List.of());
        this.hasher = hasher;
        this.clock = clock;
    }

    public Optional<AuthAccount> find(UUID id) {
        return store.get().stream().filter(account -> account.id().equals(id)).findFirst();
    }

    public Optional<AuthAccount> findByName(String name) {
        return store.get().stream().filter(account -> account.name().equalsIgnoreCase(name)).findFirst();
    }

    /** The names of the players who have an account, for suggestions. */
    public List<String> names() {
        return store.get().stream().map(AuthAccount::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    /** The number of digits of the player's PIN, or empty when they have not chosen one. */
    public Optional<Integer> pinLength(UUID id) {
        return find(id).flatMap(AuthAccount::pin).map(PinCredential::length);
    }

    /** Sets the PIN, replacing any earlier one, and ends the remembered login. Creates the account if needed. */
    public void setPin(UUID id, String name, String pin) {
        String salt = hasher.newSalt();
        PinCredential credential = new PinCredential(salt, hasher.hash(pin, salt), pin.length());
        upsert(id, name, account -> account.withPin(Optional.of(credential)).withSession(Optional.empty()));
    }

    public boolean verify(UUID id, String pin) {
        return find(id).flatMap(AuthAccount::pin)
                .map(credential -> hasher.matches(pin, credential.salt(), credential.hash()))
                .orElse(false);
    }

    /** Forgets the PIN and the remembered login: the next join chooses a new PIN. Returns whether there was an account. */
    public boolean reset(UUID id) {
        return update(id, account -> account.withPin(Optional.empty()).withSession(Optional.empty()));
    }

    /** Remembers the login of a player who just entered their PIN, for the given time, from that address. */
    public void startSession(UUID id, String address, Duration lasts) {
        AuthSession session = new AuthSession(clock.get().plus(lasts), hasher.fingerprint(address));
        update(id, account -> account.withSession(Optional.of(session)));
    }

    public boolean hasValidSession(UUID id, String address) {
        return find(id).flatMap(AuthAccount::session)
                .map(session -> session.isValidFor(hasher.fingerprint(address), clock.get()))
                .orElse(false);
    }

    /** How much longer the remembered login of a player lasts from this address, or empty when there is none. */
    public Optional<Duration> sessionRemaining(UUID id, String address) {
        Instant now = clock.get();
        return find(id).flatMap(AuthAccount::session)
                .filter(session -> session.isValidFor(hasher.fingerprint(address), now))
                .map(session -> Duration.between(now, session.expiresAt()));
    }

    /** Ends the remembered login. Returns whether there was an account. */
    public boolean endSession(UUID id) {
        return update(id, account -> account.withSession(Optional.empty()));
    }

    /** Notes where a player really was before they were moved to the waiting spot. */
    public void holdAt(UUID id, String name, HeldState position) {
        upsert(id, name, account -> account.withHeldAt(Optional.of(position)));
    }

    public Optional<HeldState> heldAt(UUID id) {
        return find(id).flatMap(AuthAccount::heldAt);
    }

    public void clearHeldAt(UUID id) {
        update(id, account -> account.withHeldAt(Optional.empty()));
    }

    private void upsert(UUID id, String name, UnaryOperator<AuthAccount> change) {
        List<AuthAccount> updated = new ArrayList<>(store.get());
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).id().equals(id)) {
                updated.set(index, change.apply(updated.get(index).withName(name)));
                store.set(List.copyOf(updated));
                return;
            }
        }
        updated.add(change.apply(AuthAccount.create(id, name)));
        store.set(List.copyOf(updated));
    }

    private boolean update(UUID id, UnaryOperator<AuthAccount> change) {
        List<AuthAccount> updated = new ArrayList<>(store.get());
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).id().equals(id)) {
                updated.set(index, change.apply(updated.get(index)));
                store.set(List.copyOf(updated));
                return true;
            }
        }
        return false;
    }
}
