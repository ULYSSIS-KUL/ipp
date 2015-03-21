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
import org.ulyssis.ipp.TagId;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class TestTeamTagMap {
    @Test
    public void testAddTagToTeam() throws Exception {
        TeamTagMap ttm = new TeamTagMap();
        TeamTagMap ttmAfter = ttm.addTagToTeam("ABCDEFF012", 0);
        assertThat(ttm.tagToTeam("ABCDEFF012"), equalTo(Optional.empty()));
        assertThat(ttmAfter.getTagToTeam(), hasEntry(new TagId("ABCDEFF012"), 0));
        assertThat(ttmAfter.tagToTeam("ABCDEFF012"), equalTo(Optional.of(0)));
    }

    @Test
    public void testRemoveTagFromTeam() throws Exception {
        TeamTagMap ttm = new TeamTagMap();
        ttm = ttm.addTagToTeam("ABCD", 0);
        ttm = ttm.addTagToTeam("BCDE", 1);
        ttm = ttm.removeTag("ABCD");
        assertThat(ttm.tagToTeam("ABCD"), equalTo(Optional.empty()));
        assertThat(ttm.tagToTeam("BCDE"), equalTo(Optional.of(1)));
    }

    @Test
    public void testLoadFromJackson() throws Exception {
        String myJson = "{\"2\":[\"ABCDEF\"],\"4\":[\"FEDCEB\"]}";
        TeamTagMap result = Serialization.getJsonMapper().readValue(myJson, TeamTagMap.class);
        assertThat(result.tagToTeam(new TagId("ABCDEF")), equalTo(Optional.of(2)));
        assertThat(result.tagToTeam(new TagId("FEDCEB")), equalTo(Optional.of(4)));
    }

    @Test
    public void testWriteToJson() throws Exception {
        TeamTagMap ttm = new TeamTagMap();
        ttm = ttm.addTagToTeam("abcd", 4);
        ttm = ttm.addTagToTeam("deff", 5);
        ttm = ttm.addTagToTeam("adcb", 4);
        String result = Serialization.getJsonMapper().writeValueAsString(ttm);
        assertThat(result, equalTo("{\"4\":[\"ABCD\",\"ADCB\"],\"5\":[\"DEFF\"]}"));
    }

    @Test
    public void testAddSameTagTwice_ShouldKeepFirstState() throws Exception {
        TeamTagMap ttm = new TeamTagMap();
        ttm = ttm.addTagToTeam("abcd", 4);
        ttm = ttm.addTagToTeam("abcd", 6);
        assertThat(ttm.tagToTeam("abcd").get(), equalTo(4));
    }
}
