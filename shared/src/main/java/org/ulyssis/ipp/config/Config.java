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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Defines all configuration options of the counting system.
 * 
 * Read from JSON.
 * 
 * Care should be taken to ensure that this class is immutable. Only
 * Jackson may write to the members of this class, when reading the
 * configuration from JSON.
 */
public class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class);

    private double trackLength;
    private ImmutableList<ReaderConfig> readers = ImmutableList.of();
    private ImmutableList<URI> speedwayURIs = ImmutableList.of();
    private ImmutableList<Team> teams = ImmutableList.of();
    private long retryInterval = 5000L;
    private long minUpdateInterval = 5000L;
    private long maxUpdateInterval = 30000L;
    private String statusChannel = "status";
    private String updateChannel = "update";
    private String updatesList = "updates";
    private String updatesSet = "updates";
    private String controlChannel = "control";
    private double maxSpeedKmPerH = 44.72D; // Default value here is Usain Bolt's top speed

    /**
     * Private constructor used by Jackson
     */
    private Config() {
    }
    
    /**
     * Create a configuration from the given configuration file.
     * 
     * Expects the configuration file to be UTF-8 formatted.
     */
    public static Optional<Config> fromConfigurationFile(Path configFile) {
        try {
            byte[] config = Files.readAllBytes(configFile);
            Charset charset = Charset.forName("utf-8");
            String configString = charset.decode(ByteBuffer.wrap(config)).toString();
            return fromConfigurationString(configString);
        } catch (IOException e) {
            LOG.error("Error reading configuration file: {}", configFile, e);
            return Optional.empty();
        }
    }
    
    /**
     * Create a configuration from the given JSON configuration string.
     */
    public static Optional<Config> fromConfigurationString(String configuration) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        try {
            Config result = mapper.readValue(configuration, Config.class);
            return Optional.of(result);
        } catch (IOException e) {
            LOG.error("Error reading configuration", e);
            return Optional.empty();
        }
    }

    /**
     * Get the track length in meters.
     */
    public double getTrackLength() {
        return trackLength;
    }

    public ImmutableList<ReaderConfig> getReaders() {
        return readers;
    }
    public ReaderConfig getReader(int readerId) {
        return this.readers.get(readerId);
    }
    public int getNbReaders() {
        return readers.size();
    }

    public ImmutableList<URI> getSpeedwayURIs() {
        return speedwayURIs;
    }
    /**
     * Get the URI of the Impinj Speedway corresponding
     * to the reader with the given id. This id ranges from 0 to
     * getSpeedwayURIs.size() - 1
     */
    public URI getSpeedwayURI(int readerId) {
        return speedwayURIs.get(readerId);
    }
    public int getNbSpeedways() {
        return speedwayURIs.size();
    }

    /**
     * The retry interval for connections in ms.
     */
    public long getRetryInterval() {
        return retryInterval;
    }
    
    /**
     * The minimum amount of time in ms between two updates, any
     * update within this amount of time is ignored.
     */
    public long getMinUpdateInterval() {
        return minUpdateInterval;
    }
    
    /**
     * The maximum time in ms to wait for an update before issuing a warning.
     */
    public long getMaxUpdateInterval() {
        return maxUpdateInterval;
    }
    
    /**
     * Gets the Redis channel name on which status updates are posted.
     *
     * Defaults to "status".
     *
     * Generally, the db id will be added to the prefix, because
     * pubsub channels are not database-specific in Redis.
     */
    public String getStatusChannel() {
        return statusChannel;
    }
    
    /**
     * The prefix of the channel where the reader announces new updates.
     *
     * This prefix defaults to "update". Generally, the db id will be added
     * to the prefix, because if multiple readers would use the same Redis
     * instance (e.g. for testing purposes), these would collide, because
     * pubsub channels are not database-specific.
     */
    public String getUpdateChannel() {
        return updateChannel;
    }

    /**
     * The prefix of the channel where an entity in the system receives control messages.
     *
     * This defaults to "control". Generally, the db id will be added to this prefix,
     * because pubsub channels are not database-specific in Redis.
     */
    public String getControlChannel() {
        return controlChannel;
    }

    /**
     * The name of the list where readers push their updates on.
     *
     * Defaults to "updates".
     */
    public String getUpdatesList() {
        return updatesList;
    }

    /**
     * The name of the set in which updates are stored in the processor.
     */
    public String getUpdatesSet() {
        return updatesSet;
    }

    /**
     * The teams, with team number and name.
     */
    public ImmutableList<Team> getTeams() {
        return teams;
    }

    /**
     * Maximum valid speed, reads will be rejected if the speed between two readers
     * was higher than this value.
     */
    public double getMaxSpeedKmPerH() {
        return maxSpeedKmPerH;
    }

    // TODO: Maybe find some other solution?
    private static Config currentConfig;

    public static void setCurrentConfig(Config config) {
        currentConfig = config;
    }

    public static Config getCurrentConfig() {
        return currentConfig;
    }

    /**
     * Calculates distance between two readers in the direction of the course. If the two given readers are the same,
     * the length of a full lap is returned.
     */
    public double distanceBetweenTwoReaders(int firstId, int secondId) {
        double firstPos = readers.get(firstId).getPosition();
        double secondPos = readers.get(secondId).getPosition();
        if (firstId < secondId) {
            return secondPos - firstPos;
        } else {
            return trackLength - firstPos + secondPos;
        }
    }
}
