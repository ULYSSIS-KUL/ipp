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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public abstract class Publisher implements ScoreOutput {
    private static final Logger LOG = LogManager.getLogger(Publisher.class);

    private final List<ScoreOutput> scoreOutputs;

    public Publisher(PublisherOptions options) throws IOException {
        scoreOutputs = ScoreOutput.outputsFromOptions(options);
    }

    @Override
    public final void outputScore(Score score) {
        LOG.debug("Outputting score {}", score.getTime());
        for (ScoreOutput output : scoreOutputs) {
            output.outputScore(score);
        }
    }

    @Override
    public final void cleanup() {
        LOG.info("Cleaning up!");
        scoreOutputs.forEach(ScoreOutput::cleanup);
    }
}
