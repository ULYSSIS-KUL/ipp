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

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class TestTeamStates {
    @Test
    public void testJsonSerialize_DefaultObject() throws Exception {
        TeamStates teamStates = new TeamStates();
        assertThat(Serialization.getJsonMapper().writeValueAsString(teamStates), equalTo("{}"));
    }

    @Test
    public void testJsonSerialize_OneMember() throws Exception {
        TeamStates teamStates = (new TeamStates()).setStateForTeam(0, new TeamState());
        assertThat(Serialization.getJsonMapper().writeValueAsString(teamStates), equalTo("{\"0\":{\"tagFragmentCount\":0}}"));
    }

    @Test
    public void testJsonDeserialize_DefaultObject() throws Exception {
        TeamStates teamStates = Serialization.getJsonMapper().readValue("{}", TeamStates.class);
        assertThat(teamStates.getTeamNbToState(), equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void testJsonDeserialize_OneMember() throws Exception {
        TeamStates teamStates = Serialization.getJsonMapper().readValue("{\"0\":{\"tagFragmentCount\":0}}", TeamStates.class);
        assertThat(teamStates.getTeamNbToState(), hasKey(0));
        // TODO: This test doesn't work because of NaN
        // assertThat(teamStates.getStateForTeam(0), sameBeanAs(Optional.of(new TeamState())));
    }
}
