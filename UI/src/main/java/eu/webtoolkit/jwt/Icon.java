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
package eu.webtoolkit.jwt;

/**
 * Utility class to gain access to WIcon, which is non-public
 */
public class Icon extends WIcon {
    public Icon(WContainerWidget parent) {
        super(parent);
    }

    public Icon() {
        super();
    }

    public Icon(final String name, WContainerWidget parent) {
        super(name, parent);
    }

    public Icon(final String name) {
        super(name);
    }
}
