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
package org.ulyssis.ipp.updates;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

/**
 * Represents a single update that is transferred as JSON over Redis.
 *
 * This is an immutable object.
 *
 * One tag update looks as such:
 *
 * [source,javascript]
 * --
 * {
 *     "readerId": 0, // the id of the reader
 *     "updateCount": 1234, // a count per reader, incremented on every update
 *     "updateTime": 12345.678, // floating point, seconds since UNIX epoch (UTC)
 *     "tag": "00112233445566778899AABBCCDDEEFF" // hex encoding of tag identifier (can be lowercase or uppercase)
 * }
 * --
 */
public final class TagUpdate {
    private Instant updateTime;
    private TagId tag;
    private int readerId;
    private long updateCount;

    /**
     * Constructor
     *
     * @param readerId
     *        The id of the reader
     * @param updateCount
     *        The count for this update, per reader, to be incremented on every update
     * @param updateTime
     *        The time for this update
     * @param tag
     *        The tag id
     */
    @JsonCreator
    public TagUpdate(
            @JsonProperty("readerId") int readerId,
            @JsonProperty("updateCount") long updateCount,
            @JsonProperty("updateTime") Instant updateTime,
            @JsonProperty("tag") TagId tag) {
        this.updateTime = updateTime;
        this.tag = tag;
        this.readerId = readerId;
        this.updateCount = updateCount;
    }

    /**
     * Get the time for this update
     *
     * @return The time for this update
     */
    public Instant getUpdateTime() {
        return updateTime;
    }

    /**
     * Get the tag id for this update
     *
     * @return The tag id for this update
     */
    public TagId getTag() {
        return tag;
    }

    /**
     * Get the reader id for this update
     *
     * @return The reader id for this update
     */
    public int getReaderId() {
        return readerId;
    }

    /**
     * Get the update count for this update
     *
     * @return The update count for this update
     */
    public long getUpdateCount() {
        return updateCount;
    }
}
