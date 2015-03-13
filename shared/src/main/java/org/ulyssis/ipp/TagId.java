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
import com.google.common.io.BaseEncoding.DecodingException;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JsonSerialize(using=TagId.Serializer.class)
@JsonDeserialize(using=TagId.Deserializer.class)
public final class TagId {
    private final byte[] id;
    private final List<Byte> idList;

    public TagId(byte[] id) {
        this.id = Arrays.copyOf(id, id.length);
        this.idList = Bytes.asList(this.id);
    }

    public TagId(List<Byte> id) {
        this.id = Bytes.toArray(id);
        this.idList = Bytes.asList(this.id);
    }

    public TagId(String hexId) throws DecodingException {
        this.id = BaseEncoding.base16().decode(hexId.toUpperCase());
        this.idList = Bytes.asList(this.id);
    }

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

    public List<Byte> toList() {
        return Collections.unmodifiableList(idList);
    }

    public byte[] toArray() {
        return Arrays.copyOf(id, id.length);
    }

    @Override
    public String toString() {
        return BaseEncoding.base16().encode(id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TagId)) {
            return false;
        }
        return idList.equals(((TagId) other).idList);
    }

    @Override
    public int hashCode() {
        return idList.hashCode();
    }
}
