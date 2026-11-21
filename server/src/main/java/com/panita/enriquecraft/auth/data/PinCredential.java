package com.panita.enriquecraft.auth.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What the server keeps of a PIN: never the PIN itself, only its salted hash and its length (the
 * entry screen needs the length to know when the last digit was typed).
 *
 * @param salt   random bytes unique to this PIN, in Base64
 * @param hash   the derived key, in Base64
 * @param length how many digits the PIN has
 */
public record PinCredential(String salt, String hash, int length) {

    public static final Codec<PinCredential> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("salt").forGetter(PinCredential::salt),
            Codec.STRING.fieldOf("hash").forGetter(PinCredential::hash),
            Codec.INT.fieldOf("length").forGetter(PinCredential::length)
    ).apply(instance, PinCredential::new));
}
