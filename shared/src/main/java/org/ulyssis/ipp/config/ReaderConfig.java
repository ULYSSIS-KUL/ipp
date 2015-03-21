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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ulyssis.ipp.TagId;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the configuration of one reader (Raspberry Pi).
 * 
 * Care should be taken to ensure that this class is immutable. Only
 * Jackson may write to the members of this class, when reading the
 * configuration from JSON.
 */
public class ReaderConfig {
    private URI uri;
    private Type type = Type.LLRP;
    private double position;
    private List<SimulatedTeam> simulatedTeams = new ArrayList<>();
    private ReplayMode replayMode = ReplayMode.REALTIME;

    public static class SimulatedTeam {
        private final TagId tag;
        private final long lapTime;

        @JsonCreator
        public SimulatedTeam(
                @JsonProperty("tag") TagId tag,
                @JsonProperty("lapTime") long lapTime) {
            this.tag = tag;
            this.lapTime = lapTime;
        }

        public TagId getTag() {
            return tag;
        }

        public long getLapTime() {
            return lapTime;
        }
    }

    public enum Type {
        LLRP,
        SIMULATOR,
        REPLAY
    }

    public enum ReplayMode {
        REALTIME,
        ALL_AT_ONCE
    }
    
    /**
     * Private constructor used by Jackson
     */
    @SuppressWarnings("unused")
    private ReaderConfig() {
    }

    public ReaderConfig(URI uri) {
        this.uri = uri;
    }

    public URI getURI() {
        return uri;
    }
    
    /**
     * Private setter used by Jackson
     */
    @SuppressWarnings("unused")
    private void setURI(URI uri) {
        this.uri = uri;
    }

    public double getPosition() {
        return position;
    }

    /**
     * Private setter used by Jackson.
     */
    @SuppressWarnings("unused")
    private void setPosition(double position) {
        this.position = position;
    }

    public Type getType() {
        return type;
    }

    /**
     * Private setter used by Jackson.
     */
    @SuppressWarnings("unused")
    private void setType(Type type) {
        this.type = type;
    }

    public List<SimulatedTeam> getSimulatedTeams() {
        return Collections.unmodifiableList(simulatedTeams);
    }

    public SimulatedTeam getSimulatedTeam(int id) {
        return simulatedTeams.get(id);
    }

    /**
     * Private setter used by Jackson.
     */
    @SuppressWarnings("unused")
    private void setSimulatedTeams(List<SimulatedTeam> simulatedTeams) {
        this.simulatedTeams = simulatedTeams;
    }

    public ReplayMode getReplayMode() {
        return this.replayMode;
    }

    /**
     * Private setter used by Jackson.
     */
    @SuppressWarnings("unused")
    private void setReplayMode(ReplayMode replayMode) {
        this.replayMode = replayMode;
    }
}
