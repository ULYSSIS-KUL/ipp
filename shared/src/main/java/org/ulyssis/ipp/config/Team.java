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
package org.ulyssis.ipp.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.TagId;

public final class Team {
    private static final Logger LOG = LogManager.getLogger(Team.class);

    private int teamNb;
    private String name;
    private ImmutableList<TagId> tags;

    @SuppressWarnings("unused")
    private Team() {
        this.teamNb = 0;
        this.name = "";
        tags = ImmutableList.of();
    }

    public Team(int teamNb, String name) {
        this.teamNb = teamNb;
        this.name = name;
        this.tags = ImmutableList.of();
    }

    public Team(int teamNb, String name, ImmutableList<TagId> tags) {
        this.teamNb = teamNb;
        this.name = name;
        this.tags = tags;
    }

    public int getTeamNb() {
        return teamNb;
    }

    public String getName() {
        return name;
    }

    public ImmutableList<TagId> getTags() {
        return tags;
    }
}
