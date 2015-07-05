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
package org.ulyssis.ipp.snapshot;

import org.ulyssis.ipp.TagId;

import java.time.Instant;

public abstract class TagEvent extends Event {
    private TagId tag;
    private int teamNb;

    public TagEvent(Instant instant, TagId tag, int teamNb) {
        super(instant);
        this.tag = tag;
        this.teamNb = teamNb;
    }

    public TagId getTag() {
        return tag;
    }

    private void setTag(TagId tag) {
        this.tag = tag;
    }

    public int getTeamNb() {
        return teamNb;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }
}
