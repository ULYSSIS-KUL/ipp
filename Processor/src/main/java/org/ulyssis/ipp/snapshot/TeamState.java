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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.config.Config;
import org.ulyssis.ipp.config.ReaderConfig;
import org.ulyssis.ipp.status.StatusMessage;
import org.ulyssis.ipp.status.StatusReporter;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@JsonSerialize(using=TeamState.Serializer.class)
@JsonDeserialize(using=TeamState.Deserializer.class)
public final class TeamState {
    private static final Logger LOG = LogManager.getLogger(TeamState.class);

    // TODO: Make these configurable!
    private static final double ALPHA = 0.4;
    private static final long MIN_TIME_BETWEEN_UPDATES = 30L;

    public static boolean enableOutlierDetection = true;

    static class Serializer extends JsonSerializer<TeamState> {
        @Override
        public void serialize(TeamState value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            if (value.lastTagSeenEvent.isPresent()) {
                jgen.writeFieldName("lastTagSeenEvent");
                jgen.writeObject(value.lastTagSeenEvent.get());
            }
            jgen.writeFieldName("tagFragmentCount");
            jgen.writeNumber(value.tagFragmentCount);
            if (!Double.isNaN(value.speed)) {
                jgen.writeFieldName("speed");
                jgen.writeNumber(value.speed);
            }
            if (!Double.isNaN(value.predictedSpeed)) {
                jgen.writeFieldName("predictedSpeed");
                jgen.writeNumber(value.predictedSpeed);
            }
            jgen.writeEndObject();
        }
    }

    static class Deserializer extends JsonDeserializer<TeamState> {
        @Override
        public TeamState deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            ObjectCodec oc = jp.getCodec();
            JsonNode node = oc.readTree(jp);
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            Optional<TagSeenEvent> lastTagSeenEvent = Optional.empty();
            int tagFragmentCount = 0;
            double speed = Double.NaN;
            double predictedSpeed = Double.NaN;
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                if (entry.getKey().equals("lastTagSeenEvent")) {
                    TagSeenEvent event = oc.treeAsTokens(entry.getValue()).readValueAs(TagSeenEvent.class);
                    lastTagSeenEvent = Optional.of(event);
                } else if (entry.getKey().equals("tagFragmentCount")) {
                    tagFragmentCount = oc.treeAsTokens(entry.getValue()).getIntValue();
                } else if (entry.getKey().equals("speed")) {
                    speed = oc.treeAsTokens(entry.getValue()).getDoubleValue();
                } else if (entry.getKey().equals("predictedSpeed")) {
                    predictedSpeed = oc.treeAsTokens(entry.getValue()).getDoubleValue();
                }
            }
            return new TeamState(lastTagSeenEvent, tagFragmentCount, speed, predictedSpeed);
        }
    }

    private final Optional<TagSeenEvent> lastTagSeenEvent;

    // The number of fragments that has been run for this team.
    // A track is divided into a number of fragments equal to the
    // number of readers.
    private final int tagFragmentCount;

    private final double speed;
    private final double predictedSpeed;

    public TeamState() {
        this.lastTagSeenEvent = Optional.empty();
        this.tagFragmentCount = 0;
        this.speed = Double.NaN;
        this.predictedSpeed = Double.NaN;
    }

    private TeamState(Optional<TagSeenEvent> lastTagSeenEvent, int tagFragmentCount, double speed, double predictedSpeed) {
        this.lastTagSeenEvent = lastTagSeenEvent;
        this.tagFragmentCount = tagFragmentCount;
        this.speed = speed;
        this.predictedSpeed = predictedSpeed;
    }

    // TODO: Refactor!
    public TeamState addTagSeenEvent(Snapshot snapshot, TagSeenEvent event) {
        int newTagFragmentCount = tagFragmentCount;
        double newSpeed = Double.NaN;
        double newPredictedSpeed = Double.NaN;
        int lastEventId = 0;
        if (lastTagSeenEvent.isPresent()) {
            TagSeenEvent lastEvent = lastTagSeenEvent.get();
            double secondsDiff = Duration.between(lastEvent.getTime(), event.getTime()).toMillis() / 1000.0;
            double distanceInMeters = Config.getCurrentConfig().distanceBetweenTwoReaders(lastEvent.getReaderId(), event.getReaderId());
            double speedInMPerS = distanceInMeters / secondsDiff;
            double speedInKmPerH = speedInMPerS * 3.6;
            if (enableOutlierDetection && speedInKmPerH > Config.getCurrentConfig().getOutlierSpeedKmPerH()) {
                LOG.info("Rejecting event because the measured speed is {} km/h, higher than the max value {} km/h",
                        speedInKmPerH, Config.getCurrentConfig().getOutlierSpeedKmPerH());
                // TODO: better way to do this side effect than just running a task from this method? Affects architecture around Event.doApply though
                CompletableFuture.runAsync(() -> reportOutlier(event));
            }
            lastEventId = lastEvent.getReaderId();
        }
        int diff = (event.getReaderId() - lastEventId);
        if (diff < 0) {
            diff = Config.getCurrentConfig().getNbReaders() + diff;
        } else if (diff == 0 && (lastTagSeenEvent.isPresent() || // TODO: Refactor this fustercluck of comparisons
                (snapshot.getStartTime().isBefore(event.getTime()) &&
                        !Duration.between(snapshot.getStartTime(), event.getTime()).minusSeconds(MIN_TIME_BETWEEN_UPDATES).isNegative()))) {
            diff = Config.getCurrentConfig().getNbReaders();
        }
        newTagFragmentCount += diff;
        List<ReaderConfig> readers = Config.getCurrentConfig().getReaders();
        double distance = 0;
        if (lastTagSeenEvent.isPresent()) {
            for (int i = tagFragmentCount; i < newTagFragmentCount; i++) {
                int j = i % Config.getCurrentConfig().getNbReaders();
                int k = (i + 1) % Config.getCurrentConfig().getNbReaders();
                if (k > j) {
                    distance += readers.get(k).getPosition() - readers.get(j).getPosition();
                } else if (j > k) {
                    assert k == 0;
                    distance += Config.getCurrentConfig().getTrackLength() - readers.get(j).getPosition();
                } else {
                    // This can happen when there is only one reader.
                    distance += Config.getCurrentConfig().getTrackLength();
                }
            }
            double time = Duration.between(lastTagSeenEvent.get().getTime(), event.getTime()).toMillis() / 1000D;
            newSpeed = distance / time;
            if (Double.isNaN(predictedSpeed)) {
                newPredictedSpeed = newSpeed;
            } else {
                newPredictedSpeed = newSpeed * ALPHA + predictedSpeed * (1 - ALPHA);
            }
        } else if (snapshot.getStartTime().isBefore(event.getTime()) &&
                !Duration.between(snapshot.getStartTime(), event.getTime()).minusSeconds(MIN_TIME_BETWEEN_UPDATES).isNegative()) {
            double time = Duration.between(snapshot.getStartTime(), event.getTime()).toMillis() / 1000D;
            distance = readers.get(event.getReaderId()).getPosition();
            newSpeed = distance / time;
            if (Double.isNaN(predictedSpeed)) {
                newPredictedSpeed = newSpeed;
            } else {
                newPredictedSpeed = newSpeed * ALPHA + predictedSpeed * (1 - ALPHA);
            }
        }
        return new TeamState(Optional.of(event), newTagFragmentCount, newSpeed, newPredictedSpeed);
    }

    private void reportOutlier(TagSeenEvent event) {
        StatusReporter reporter = new StatusReporter(
                StatusReporter.getRedisURI(),
                Config.getCurrentConfig().getStatusChannel()
        );

        String tag = event.getTag().toString();
        int reader = event.getReaderId();
        long eventId = event.getId().get();
        reporter.broadcast(new StatusMessage(
                StatusMessage.MessageType.READ_OUTLIER,
                "Tag " + tag + " at reader " + reader + " (event id " + eventId + ")")
        );
    }

    public TeamState addCorrection(int correction) {
        int newTagFragmentCount = tagFragmentCount + correction * Config.getCurrentConfig().getNbReaders();
        // TODO: Should we lower bound it to 0?
        if (newTagFragmentCount < 0) {
            newTagFragmentCount = 0;
        }
        // TODO: I don't know about the prediction, if something should happen to that.
        return new TeamState(lastTagSeenEvent, newTagFragmentCount, speed, predictedSpeed);
    }

    public int getTagFragmentCount() {
        return tagFragmentCount;
    }

    public Optional<TagSeenEvent> getLastTagSeenEvent() {
        return lastTagSeenEvent;
    }

    public int getNbLaps() {
        return tagFragmentCount / Config.getCurrentConfig().getNbReaders();
    }

    public double getSpeed() {
        return speed;
    }

    public double getPredictedSpeed() {
        return predictedSpeed;
    }
}
