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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.ReaderConfig;
import org.ulyssis.ipp.TagId;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TestSnapshot {
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Before
    public void setUp() {
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getNbReaders()).thenReturn(3);
        Mockito.when(config.getTrackLength()).thenReturn(300D);
        ReaderConfig reader1 = Mockito.mock(ReaderConfig.class);
        Mockito.when(reader1.getPosition()).thenReturn(0D);
        ReaderConfig reader2 = Mockito.mock(ReaderConfig.class);
        Mockito.when(reader2.getPosition()).thenReturn(100D);
        ReaderConfig reader3 = Mockito.mock(ReaderConfig.class);
        Mockito.when(reader3.getPosition()).thenReturn(200D);
        ImmutableList<ReaderConfig> readers = ImmutableList.of(reader1, reader2, reader3);
        Mockito.when(config.getReaders()).thenReturn(readers);
        Config.setCurrentConfig(config);
    }

    @After
    public void cleanUp() {
        Config.setCurrentConfig(null);
    }

    @Test
    public void testSerializeToJson_DefaultObject() throws Exception {
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH, null)
                .withStartTime(Instant.EPOCH)
                .withEndTime(Instant.EPOCH)
                .build();
        MatcherAssert.assertThat(objectMapper.writeValueAsString(snapshot),
                SameJSONAs.sameJSONAs("{snapshotTime:0,startTime:0,endTime:0,teamTagMap:{}," +
                        "teamStates:{},publicTeamStates:{},statusMessage:\"\",status:NoResults,updateFrequency:3}"));
    }

    @Test
    public void testSerializeToJson_ComplexerObject() throws Exception {
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH, null)
                .withStartTime(Instant.EPOCH)
                .withEndTime(Instant.EPOCH)
                .withTeamStates(new TeamStates()
                        .setStateForTeam(0, new TeamState().addTagSeenEvent(
                                new Snapshot(Instant.EPOCH),
                                new TagSeenEvent(Instant.EPOCH, new TagId("ABCD"), 0, 0L))
                                .addTagSeenEvent(null, // TODO: It's not really clean that we're passing null here,
                                        //       but it should work fine nonetheless
                                        new TagSeenEvent(Instant.EPOCH.plus(1000, ChronoUnit.SECONDS),
                                                new TagId("ABCD"), 0, 1L))))
                .withTeamTagMap(new TeamTagMap()
                        .addTagToTeam("ABCD", 0))
                .withStatusMessage("foo").build();
        MatcherAssert.assertThat(objectMapper.writeValueAsString(snapshot),
                SameJSONAs.sameJSONAs("{\"snapshotTime\":0,statusMessage:foo,\"startTime\":0,\"endTime\":0,\"teamTagMap\":{\"0\":[\"ABCD\"]}," +
                        "\"teamStates\":{\"0\":{\"lastTagSeenEvent\":{\"type\":\"TagSeen\",\"time\":1000," +
                        "\"tag\":\"ABCD\",\"readerId\":0}, \"tagFragmentCount\":3}}}").allowingExtraUnexpectedFields());
    }

    @Test
    public void testAddTagEvent() throws Exception {
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = addTagEvent.doApply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamTagMap().tagToTeam("ABCD").get(), Matchers.equalTo(0));
    }

    @Test
    public void testTagSeenEvents_ShouldAddLap() throws Exception {
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = addTagEvent.doApply(snapshot);
        StartEvent startEvent = new StartEvent(Instant.EPOCH.plus(2, ChronoUnit.SECONDS));
        snapshot = startEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(3, ChronoUnit.SECONDS), new TagId("ABCD"), 0, 0L);
        snapshot = tagSeenEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(60, ChronoUnit.SECONDS), new TagId("ABCD"), 0, 1L);
        snapshot = tagSeenEvent2.doApply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamStates().getNbLapsForTeam(0), Matchers.equalTo(1));
    }

    @Test
    public void testTagSeenEventBeforeStart_ShouldBeIgnored() throws Exception {
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        AddTagEvent addTagEvent =
                new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = addTagEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(2, ChronoUnit.SECONDS), new TagId("ABCD"), 0, 0L);
        snapshot = tagSeenEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(3, ChronoUnit.SECONDS), new TagId("ABCD"), 0, 1L);
        snapshot = tagSeenEvent2.doApply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamStates().getNbLapsForTeam(0), Matchers.equalTo(0));
    }

    @Test
    public void testTagSeenEventAfterEnd_ShouldBeIgnored() throws Exception {
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("DCBA"), 3);
        snapshot = addTagEvent.doApply(snapshot);
        StartEvent startEvent = new StartEvent(Instant.EPOCH.plus(2, ChronoUnit.SECONDS));
        snapshot = startEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(3, ChronoUnit.SECONDS), new TagId("DCBA"), 0, 0L);
        snapshot = tagSeenEvent.doApply(snapshot);
        EndEvent endEvent = new EndEvent(Instant.EPOCH.plus(50, ChronoUnit.SECONDS));
        snapshot = endEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(100, ChronoUnit.SECONDS), new TagId("DCBA"), 0, 1L);
        snapshot = tagSeenEvent2.doApply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamStates().getNbLapsForTeam(3), Matchers.equalTo(0));
    }

    @Test
    public void testPredictedSpeedWhenStartedThenFirstEvent() throws Exception {
        TagId tag = new TagId("DCBA");
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH, tag, 3);
        snapshot = addTagEvent.doApply(snapshot);
        StartEvent startEvent = new StartEvent(Instant.EPOCH);
        snapshot = startEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(10, ChronoUnit.SECONDS), tag, 0, 0L);
        snapshot = tagSeenEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(50, ChronoUnit.SECONDS), tag, 1, 1L);
        snapshot = tagSeenEvent2.doApply(snapshot);
        double speedShouldBe = 100D / (50D - 10D);
        MatcherAssert.assertThat(snapshot.getTeamStates().getStateForTeam(3).get()
            .getPredictedSpeed(), Matchers.equalTo(speedShouldBe));
        MatcherAssert.assertThat(snapshot.getTeamStates().getStateForTeam(3).get()
                .getSpeed(), Matchers.equalTo(speedShouldBe));
    }

    @Test
    public void testPredictedSpeedWhenFirstEventThenStarted() throws Exception {
        TagId tag = new TagId("DCBA");
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH, tag, 3);
        snapshot = addTagEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.minus(10, ChronoUnit.SECONDS), tag, 0, 0L);
        snapshot = tagSeenEvent.doApply(snapshot);
        StartEvent startEvent = new StartEvent(Instant.EPOCH);
        snapshot = startEvent.doApply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(50, ChronoUnit.SECONDS), tag, 1, 1L);
        snapshot = tagSeenEvent2.doApply(snapshot);
        double speedShouldBe = 100D / 50D;
        MatcherAssert.assertThat(snapshot.getTeamStates().getStateForTeam(3).get()
                .getPredictedSpeed(), Matchers.equalTo(speedShouldBe));
        MatcherAssert.assertThat(snapshot.getTeamStates().getStateForTeam(3).get()
                .getSpeed(), Matchers.equalTo(speedShouldBe));
    }
}
