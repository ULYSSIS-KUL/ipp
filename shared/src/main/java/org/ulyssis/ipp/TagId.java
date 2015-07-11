/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 * 
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
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
import java.util.List;

/**
 * = Represents a tag id (that is properly comparable and immutable)
 *
 * Mostly just wraps a string. Compares are case insensitive.
 * (Tag id AABBCC is the same as aabbcc)
 * This can be used for hex ids, or string ids
 */
@JsonSerialize(using=TagId.Serializer.class)
@JsonDeserialize(using=TagId.Deserializer.class)
public final class TagId {
    private final String id;
    private final String lowerCaseId;
    private final int hashCode;

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
     * = Create a new TagId from the given byte array.
     *
     * @param id
     *        The tag id in byte array form. It will be
     *        converted to a lowercase hex form.
     */
    public TagId(byte[] id) {
        this(BaseEncoding.base16().lowerCase().encode(id));
    }

    /**
     * = Create a new TagId from the given byte list.
     *
     * @param id
     *        The tag id in byte list form. It will be
     *        converted to a lowercase hex form.
     */
    public TagId(List<Byte> id) {
        this(BaseEncoding.base16().lowerCase().encode(Bytes.toArray(id)));
    }

    /**
     * = Create a new TagId
     *
     * @param id
     *        A string that uniquely identifies the tag
     * @throws java.lang.NullPointerException
     *         A NullPointerException will be thrown if the given id is null.
     */
    public TagId(String id) throws NullPointerException {
        if (id == null) throw new NullPointerException("The tag id is not allowed to be null!");
        this.id = id;
        this.lowerCaseId = id.toLowerCase();
        this.hashCode = id.hashCode();
    }

    /**
     * = Get the tag id as a string
     *
     * @return This tag id as a string
     */
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof TagId)) return false;
        return this.lowerCaseId.equals(((TagId) other).lowerCaseId);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
