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

import org.junit.Test;
import org.ulyssis.ipp.utils.Serialization;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTeamState {
    @Test
    public void testSerializeToJson_DefaultObject() throws Exception {
        TeamState teamState = new TeamState();
        assertThat(Serialization.getJsonMapper().writeValueAsString(teamState), equalTo("{\"tagFragmentCount\":0}"));
    }

    @Test
    public void testDeserializeFromJson_DefaultObject() throws Exception {
        TeamState teamState = Serialization.getJsonMapper().readValue("{\"tagFragmentCount\":0}", TeamState.class);
        assertThat(teamState.getTagFragmentCount(), equalTo(0));
        assertThat(teamState.getLastTagSeenEvent(), equalTo(Optional.<TagSeenEvent>empty()));
    }
}
