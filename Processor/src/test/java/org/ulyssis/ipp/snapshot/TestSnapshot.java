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
import org.ulyssis.ipp.snapshot.events.AddTagEvent;
import org.ulyssis.ipp.snapshot.events.EndEvent;
import org.ulyssis.ipp.snapshot.events.StartEvent;
import org.ulyssis.ipp.snapshot.events.TagSeenEvent;
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
        Mockito.when(config.getTrackLength()).thenReturn(520D);
        ReaderConfig reader1 = Mockito.mock(ReaderConfig.class);
        Mockito.when(reader1.getPosition()).thenReturn(0D);
        ReaderConfig reader2 = Mockito.mock(ReaderConfig.class);
        Mockito.when(reader2.getPosition()).thenReturn(170D);
        ReaderConfig reader3 = Mockito.mock(ReaderConfig.class);
        Mockito.when(reader3.getPosition()).thenReturn(350D);
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
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH)
                .withStartTime(Instant.EPOCH)
                .withEndTime(Instant.EPOCH)
                .build();
        MatcherAssert.assertThat(objectMapper.writeValueAsString(snapshot),
                SameJSONAs.sameJSONAs("{snapshotTime:0,startTime:0,endTime:0,teamTagMap:{}," +
                        "teamStates:{},publicTeamStates:{},statusMessage:\"\",status:NoResults,updateFrequency:3}"));
    }

    @Test
    public void testSerializeToJson_ComplexerObject() throws Exception {
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH)
                .withStartTime(Instant.EPOCH)
                .withEndTime(Instant.EPOCH)
                .withTeamStates(new TeamStates()
                        .setStateForTeam(0, new TeamState().addTagSeenEvent(
                                new TagSeenEvent(Instant.EPOCH, new TagId("ABCD"), 0))
                                .addTagSeenEvent(new TagSeenEvent(Instant.EPOCH.plus(1000, ChronoUnit.SECONDS),
                                        new TagId("ABCD"), 0))))
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
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH).build();
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = addTagEvent.apply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamTagMap().tagToTeam("ABCD").get(), Matchers.equalTo(0));
    }

    @Test
    public void testTagSeenEvents_ShouldAddLap() throws Exception {
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH).build();
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = addTagEvent.apply(snapshot);
        StartEvent startEvent = new StartEvent(Instant.EPOCH.plus(2, ChronoUnit.SECONDS));
        snapshot = startEvent.apply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(3, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = tagSeenEvent.apply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(60, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = tagSeenEvent2.apply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamStates().getNbLapsForTeam(0), Matchers.equalTo(1));
    }

    @Test
    public void testTagSeenEventBeforeStart_ShouldBeIgnored() throws Exception {
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH).build();
        AddTagEvent addTagEvent =
                new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = addTagEvent.apply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(2, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = tagSeenEvent.apply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(3, ChronoUnit.SECONDS), new TagId("ABCD"), 0);
        snapshot = tagSeenEvent2.apply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamStates().getNbLapsForTeam(0), Matchers.equalTo(0));
    }

    @Test
    public void testTagSeenEventAfterEnd_ShouldBeIgnored() throws Exception {
        Snapshot snapshot = Snapshot.builder(Instant.EPOCH).build();
        AddTagEvent addTagEvent = new AddTagEvent(Instant.EPOCH.plus(1, ChronoUnit.SECONDS), new TagId("DCBA"), 3);
        snapshot = addTagEvent.apply(snapshot);
        StartEvent startEvent = new StartEvent(Instant.EPOCH.plus(2, ChronoUnit.SECONDS));
        snapshot = startEvent.apply(snapshot);
        TagSeenEvent tagSeenEvent =
                new TagSeenEvent(Instant.EPOCH.plus(3, ChronoUnit.SECONDS), new TagId("DCBA"), 0);
        snapshot = tagSeenEvent.apply(snapshot);
        EndEvent endEvent = new EndEvent(Instant.EPOCH.plus(4, ChronoUnit.SECONDS));
        snapshot = endEvent.apply(snapshot);
        TagSeenEvent tagSeenEvent2 =
                new TagSeenEvent(Instant.EPOCH.plus(4, ChronoUnit.SECONDS), new TagId("DCBA"), 0);
        snapshot = tagSeenEvent2.apply(snapshot);
        MatcherAssert.assertThat(snapshot.getTeamStates().getNbLapsForTeam(3), Matchers.equalTo(0));
    }
}
