package org.ulyssis.ipp;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a tag id (that is properly comparable and immutable)
 *
 * Mostly just a handy wrapper around a byteList, that
 * properly implements equals for usage in HashMaps and HashSets,
 * and JSON (de)serialization as a hex string.
 */
@JsonSerialize(using=TagId.Serializer.class)
@JsonDeserialize(using=TagId.Deserializer.class)
public final class TagId {
    private final byte[] id;
    private final List<Byte> idList;

    static class Serializer extends JsonSerializer<TagId> {
        @Override
        public void serialize(TagId value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.toString());
        }
    }

    static class Deserializer extends JsonDeserializer<TagId> {
        @Override
        public TagId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return new TagId(jp.getValueAsString());
        }
    }

    /**
     * Create a new TagId from the given byte array.
     *
     * @param id
     *        The tag id in byte array form. This array
     *        will be copied over to guarantee immutability.
     */
    public TagId(byte[] id) {
        this.id = Arrays.copyOf(id, id.length);
        this.idList = Bytes.asList(this.id);
    }

    /**
     * Create a new TagId from the given byte list.
     *
     * @param id
     *        The tag id in byte list form. This list
     *        will be copied over into a byte array.
     */
    public TagId(List<Byte> id) {
        this.id = Bytes.toArray(id);
        this.idList = Bytes.asList(this.id);
    }

    /**
     * Create a new TagId from the given hex string.
     *
     * @param hexId
     *        The hex representation of the tag id.
     * @throws java.lang.IllegalArgumentException
     *         An IllegalArgumentException will be thrown if the
     *         hex string can't be decoded.
     */
    public TagId(String hexId) throws IllegalArgumentException {
        this.id = BaseEncoding.base16().decode(hexId.toUpperCase());
        this.idList = Bytes.asList(this.id);
    }

    /**
     * Convert this tag id to a list.
     *
     * @return An *unmodifiable view* of the bytes corresponding to this tag id.
     */
    public List<Byte> toList() {
        return Collections.unmodifiableList(idList);
    }

    /**
     * Convert this tag id to an array.
     *
     * @return A *copy* of the bytes corresponding to this tag id.
     */
    public byte[] toArray() {
        return Arrays.copyOf(id, id.length);
    }

    /**
     * Convert this tag id to a hex string.
     *
     * @return A hex representation of this tag id.
     */
    @Override
    public String toString() {
        return BaseEncoding.base16().encode(id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof TagId)) return false;
        return idList.equals(((TagId) other).idList);
    }

    @Override
    public int hashCode() {
        return idList.hashCode();
    }
}
