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
package org.ulyssis.ipp.publisher;

import org.junit.Test;
import org.ulyssis.ipp.TagId;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.Team;
import org.ulyssis.ipp.snapshot.AddTagEvent;
import org.ulyssis.ipp.snapshot.CorrectionEvent;
import org.ulyssis.ipp.snapshot.Event;
import org.ulyssis.ipp.snapshot.Snapshot;
import org.ulyssis.ipp.updates.Status;
import org.ulyssis.ipp.utils.Serialization;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestScore {
    private static class Result {
        int nb, laps;
        String name;
        Result(int nb, String name, int laps) {
            this.nb = nb;
            this.name = name;
            this.laps = laps;
        }
    }

    private List<Result> results2014 = Arrays.asList(
            new Result(4, "VTK", 1010),
            new Result(1, "Apolloon", 988),
            new Result(3, "LBK", 970),
            new Result(2, "Ekonomika", 969),
            new Result(16, "Medica", 912),
            new Result(5, "Wina", 847),
            new Result(18, "Lerkeveld", 838),
            new Result(11, "Industria", 836),
            new Result(9, "Atmosphere", 825),
            new Result(17, "Pedal", 821),
            new Result(13, "VRG", 801),
            new Result(10, "4 Speed", 800),
            new Result(14, "Politika", 794),
            new Result(12, "Oker", 769),
            new Result(8, "Letteren United", 760),
            new Result(7, "Psychokring", 734),
            new Result(6, "Enof Leuven", 556),
            new Result(15, "Run for Specials", 503)
    );

    private List<Result> results2018 = Arrays.asList(
            new Result(1, "Apolloon", 1073),
            new Result(4, "VTK", 1072),
            new Result(3, "LBK", 1016),
            new Result(2, "Viva Ekonomika", 1015),
            new Result(16, "Medica", 954),
            new Result(11, "Industria", 953),
            new Result(5, "Wina", 884),
            new Result(9, "Atmosphere", 876),
            new Result(13, "VRG Crimen", 865),
            new Result(8, "Tripel Hop", 864),
            new Result(18, "Lerkeveld", 855),
            new Result(6, "UCLL OKeR", 832),
            new Result(17, "Pedal", 828),
            new Result(7, "Psychokring", 811),
            new Result(10, "Runner's High", 810),
            new Result(12, "Farmaceutica", 795),
            new Result(14, "Politika", 748),
            new Result(15, "Run for Specials", 497)
    );

    void test24u(int edition, List<Result> results) throws Exception {
        Config.setCurrentConfig(Config.fromConfigurationFile(Paths.get("..","configs","24u" + edition + ".json")).get());
        BufferedReader reader = Files.newBufferedReader(Paths.get("..","replays","24u" + edition, "events.json"));
        String line = reader.readLine();
        Snapshot snapshot = new Snapshot(Instant.EPOCH);
        for (Team team : Config.getCurrentConfig().getTeams()) {
            for (TagId tag : team.getTags()) {
                snapshot = new AddTagEvent(Instant.EPOCH, tag, team.getTeamNb()).apply(snapshot);
            }
        }
        while (line != null) {
            Event event = Serialization.getJsonMapper().readValue(line, Event.class);
            snapshot = event.apply(snapshot);
            line = reader.readLine();
        }
        Score score = new Score(snapshot, true);
        assertThat(score.getStatus(), equalTo(Status.FinalScore));
        Collection<Score.Team> teams = score.getTeams();
        List<Score.Team> t = new ArrayList<>(teams);
        assertThat(t.size(), equalTo(results.size()));
        for (int i = 0; i < t.size(); ++i) {
            Result result = results.get(i);
            Score.Team team = t.get(i);
            assertThat(team.getNb(), equalTo(result.nb));
            assertThat(team.getName(), equalTo(result.name));
            assertThat(team.getLaps(), equalTo(result.laps));
        }
    }

    @Test
    public void test24u2014() throws Exception {
        test24u(2014, results2014);
    }

    @Test
    public void test24u2018() throws Exception {
        test24u(2018, results2018);
    }
}