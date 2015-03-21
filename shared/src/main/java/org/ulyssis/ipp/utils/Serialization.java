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
package org.ulyssis.ipp.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility static class for getting ObjectMappers, so
 * a single mapper can be shared and doesn't have to
 * be instantiated all the time.
 */
public final class Serialization {
    private static final ObjectMapper jsonMapper;

    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.findAndRegisterModules();
    }

    // Static methods only! No instances!
    private Serialization() {
    }

    /**
     * Get a JSON mapper, with all modules registered.
     *
     * This is equivalent to:
     *
     * [source,java]
     * --
     * jsonMapper = new ObjectMapper();
     * jsonMapper.findAndRegisterModules();
     * --
     *
     * @return A JSON ObjectMapper
     */
    public static ObjectMapper getJsonMapper() {
        return jsonMapper;
    }
}
