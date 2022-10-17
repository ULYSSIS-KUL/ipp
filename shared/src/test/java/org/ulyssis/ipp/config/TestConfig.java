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

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ulyssis.ipp.utils.Serialization;
import org.ulyssis.ipp.TagId;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class TestConfig {

    @Test
    public void testEmptyConfig() {
        Optional<Config> conf = Config.fromConfigurationString("{}");
        assertThat(conf.isPresent(), is(true));
        Config config = conf.get();
        assertThat(config.getReaders(), emptyCollectionOf(ReaderConfig.class));
        assertThat(config.getSpeedwayURIs(), emptyCollectionOf(URI.class));
    }
    
    @Test
    public void testBasicConfig() {
        Optional<Config> conf = Config.fromConfigurationString("{\"readers\":[{\"uri\":\"redis://10.0.0.1:6379\"}]}");
        assertTrue(conf.isPresent());
        Config config = conf.get();
        assertEquals(1, config.getReaders().size());
        assertEquals(6379, config.getReader(0).getURI().getPort());
        assertEquals("10.0.0.1", config.getReader(0).getURI().getHost());
    }
    
    @Test
    public void testRealisticConfigFromFile() {
        Optional<Config> conf = Config.fromConfigurationFile(Paths.get("src/test/resources/test1.json"));
        assertTrue(conf.isPresent());
        Config config = conf.get();
        assertEquals(520D, config.getTrackLength(), 0D);
        assertEquals(3, config.getNbSpeedways());
        assertEquals(3, config.getSpeedwayURIs().size());
        assertEquals("10.0.0.1", config.getSpeedwayURI(0).getHost());
        assertEquals("10.0.0.2", config.getSpeedwayURI(1).getHost());
        assertEquals("10.0.0.3", config.getSpeedwayURI(2).getHost());
        assertEquals(3, config.getNbReaders());
        assertEquals(3, config.getReaders().size());
        assertEquals("10.0.0.4", config.getReader(0).getURI().getHost());
        assertEquals(ReaderConfig.Type.LLRP, config.getReader(0).getType());
        assertEquals(0D, config.getReader(0).getPosition(), 0D);
        assertEquals("10.0.0.5", config.getReader(1).getURI().getHost());
        assertEquals(ReaderConfig.Type.LLRP, config.getReader(1).getType());
        assertEquals(220D, config.getReader(1).getPosition(), 0D);
        assertEquals("10.0.0.6", config.getReader(2).getURI().getHost());
        assertEquals(ReaderConfig.Type.LLRP, config.getReader(2).getType());
        assertEquals(440D, config.getReader(2).getPosition(), 0D);
        assertEquals(35.25, config.getOutlierSpeedKmPerH(), 0D);
    }

    @Test
    public void testSimulationConfigFromFile() throws Exception {
        Optional<Config> conf = Config.fromConfigurationFile(Paths.get("src/test/resources/test2.json"));
        assertTrue(conf.isPresent());
        Config config = conf.get();
        assertEquals(520D, config.getTrackLength(), 0D);
        assertEquals(0, config.getNbSpeedways());
        assertEquals(1, config.getNbReaders());
        assertEquals("10.0.0.4", config.getReader(0).getURI().getHost());
        assertEquals(ReaderConfig.Type.SIMULATOR, config.getReader(0).getType());
        assertEquals(4, config.getReader(0).getSimulatedTeams().size());
        assertEquals(1000L, config.getReader(0).getSimulatedTeam(0).getLapTime());
        assertEquals(new TagId("0A"), config.getReader(0).getSimulatedTeam(0).getTag());
        assertEquals(2000L, config.getReader(0).getSimulatedTeam(1).getLapTime());
        assertEquals(new TagId("0B"), config.getReader(0).getSimulatedTeam(1).getTag());
        assertEquals(3000L, config.getReader(0).getSimulatedTeam(2).getLapTime());
        assertEquals(new TagId("0C"), config.getReader(0).getSimulatedTeam(2).getTag());
        assertEquals(4000L, config.getReader(0).getSimulatedTeam(3).getLapTime());
        assertEquals(new TagId("0D"), config.getReader(0).getSimulatedTeam(3).getTag());
        assertEquals(34D, config.getOutlierSpeedKmPerH(), 0D);
    }

    @Test
    public void testDeserializeTeam() throws Exception {
        Team team = Serialization.getJsonMapper().readValue("{\"teamNb\":1,\"name\":\"Team one\"}", Team.class);
        assertThat(team.getTeamNb(), equalTo(1));
        assertThat(team.getName(), equalTo("Team one"));
    }

    @Test
    public void testDeserializeTeamWithTags() throws Exception {
        Team team = Serialization.getJsonMapper().readValue("{\"teamNb\":2,\"name\":\"Team two\",\"tags\":[\"abcd0123\",\"0123ABCD\"]}",
                Team.class);
        assertThat(team.getTeamNb(), equalTo(2));
        assertThat(team.getName(), equalTo("Team two"));
        assertThat(team.getTags(), containsInAnyOrder(new TagId("abcd0123"), new TagId("0123ABCD")));
    }

    @Test
    public void testSerializeTeam() throws Exception {
        Team team = new Team(3, "Team three");
        String json = Serialization.getJsonMapper().writeValueAsString(team);
        assertThat(json, sameJSONAs("{\"teamNb\":3,\"name\":\"Team three\",\"tags\":[]}"));
    }

    @Test
    public void testSerializeTeamWithTags() throws Exception {
        Team team = new Team(4, "Team four", ImmutableList.of(new TagId("abcd0123"), new TagId("0123ABCD")));
        String json = Serialization.getJsonMapper().writeValueAsString(team);
        // TODO: This check is too specific
        assertThat(json, sameJSONAs("{\"teamNb\":4,\"name\":\"Team four\",\"tags\":[\"abcd0123\",\"0123ABCD\"]}")
            .allowingAnyArrayOrdering());
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testNameNaamMixup() throws Exception {
        exception.expect(UnrecognizedPropertyException.class);
        Serialization.getJsonMapper().readValue("{\"teamNb\":3,\"naam\":\"My team\"}", Team.class);
    }

    @Test
    public void testDistanceCalculation() {
        Optional<Config> conf = Config.fromConfigurationFile(Paths.get("src/test/resources/test1.json"));
        Config config = conf.get();

        assertThat(config.distanceBetweenTwoReaders(0, 1), equalTo(220D));
        assertThat(config.distanceBetweenTwoReaders(1, 2), equalTo(220D));
        assertThat(config.distanceBetweenTwoReaders(0, 2), equalTo(440D));

        assertThat(config.distanceBetweenTwoReaders(2, 0), equalTo(80D));
        assertThat(config.distanceBetweenTwoReaders(2, 1), equalTo(300D));
        assertThat(config.distanceBetweenTwoReaders(1, 0), equalTo(300D));

        assertThat(config.distanceBetweenTwoReaders(0, 0), equalTo(520D));
        assertThat(config.distanceBetweenTwoReaders(1, 1), equalTo(520D));
        assertThat(config.distanceBetweenTwoReaders(2, 2), equalTo(520D));
    }
}
