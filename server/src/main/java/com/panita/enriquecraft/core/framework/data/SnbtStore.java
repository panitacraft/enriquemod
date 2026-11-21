package com.panita.enriquecraft.core.framework.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.panita.enriquecraft.core.framework.io.AtomicFiles;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * One data file holding one value, described by a Mojang {@link Codec}. Every {@link #set} is
 * written to disk at once, atomically.
 * <p>
 * Files are SNBT, the text form of NBT that Minecraft itself uses in commands. Unlike JSON it
 * keeps the exact type of every number, so an item saved here comes back with byte-for-byte the
 * same components and custom data.
 * <p>
 * Safety rule: a file that cannot be read (broken SNBT, or content the codec rejects) is never
 * overwritten. It is renamed to {@code <name>.broken}, the problem is logged, and the store
 * starts from its empty value. If the file cannot even be moved aside, the store stays in memory
 * only, so the unreadable file is left exactly as it is.
 *
 * @param <T> the type of the stored value; treat it as immutable and {@link #set} a new value to change it
 */
public final class SnbtStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnbtStore.class);
    private static final String BROKEN_SUFFIX = ".broken";

    private final String relativePath;
    private final Codec<T> codec;
    private final T emptyValue;
    private Path file;
    private DynamicOps<Tag> ops;
    private T value;
    private boolean writable = true;

    /**
     * @param relativePath the file, relative to the data directory
     * @param codec        how the value is read and written
     * @param emptyValue   the value when there is no file yet
     */
    public SnbtStore(String relativePath, Codec<T> codec, T emptyValue) {
        this.relativePath = relativePath;
        this.codec = codec;
        this.emptyValue = emptyValue;
        this.value = emptyValue;
    }

    public T get() {
        return value;
    }

    /**
     * Replaces the value and writes it to disk. A failed write is logged; the new value stays in
     * memory.
     *
     * @throws IllegalStateException if the store has not been loaded yet
     */
    public void set(T newValue) {
        if (file == null) {
            throw new IllegalStateException("The store " + relativePath + " is not loaded yet");
        }
        value = newValue;
        if (!writable) {
            LOGGER.error("{} is unreadable and could not be moved aside, so this change is kept in memory only", file);
            return;
        }
        DataResult<Tag> encoded = codec.encodeStart(ops, newValue);
        if (encoded.error().isPresent()) {
            LOGGER.error("Could not encode {}: {}", relativePath, encoded.error().get().message());
            return;
        }
        try {
            AtomicFiles.write(file, new SnbtPrinterTagVisitor().visit(encoded.result().orElseThrow()) + "\n");
        } catch (IOException e) {
            LOGGER.error("Could not write {}: {}", file, e.getMessage());
        }
    }

    /**
     * Reads the file, replacing the current value.
     *
     * @param directory the data directory
     * @param ops       how to turn the codec's data into NBT; carries the registries for item components
     */
    void load(Path directory, DynamicOps<Tag> ops) {
        this.file = directory.resolve(relativePath);
        this.ops = ops;
        this.value = emptyValue;
        this.writable = true;
        if (!Files.exists(file)) {
            return;
        }
        try {
            Tag tag = TagParser.create(NbtOps.INSTANCE).parseFully(Files.readString(file, StandardCharsets.UTF_8));
            DataResult<T> decoded = codec.parse(ops, tag);
            if (decoded.error().isPresent()) {
                setAside("the content is not valid: " + decoded.error().get().message());
            } else {
                value = decoded.result().orElseThrow();
            }
        } catch (CommandSyntaxException e) {
            setAside("it is not valid SNBT: " + e.getMessage());
        } catch (IOException e) {
            writable = false;
            LOGGER.error("Could not read {}: {}. Its data is not available and it will not be overwritten.", file, e.getMessage());
        }
    }

    private void setAside(String reason) {
        Path broken = file.resolveSibling(file.getFileName() + BROKEN_SUFFIX);
        try {
            Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.error("Could not use {}: {}. It was moved to {} and the data starts empty.", file, reason, broken);
        } catch (IOException e) {
            writable = false;
            LOGGER.error("Could not use {}: {}. It could not be moved aside ({}), so it is left untouched and changes stay in memory.",
                    file, reason, e.getMessage());
        }
    }
}
