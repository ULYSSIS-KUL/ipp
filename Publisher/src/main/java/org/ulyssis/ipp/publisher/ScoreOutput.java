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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A score output interface. Multiple implementations exist, for outputting scores to files or passing them
 * on via HTTP.
 */
public interface ScoreOutput {
    static List<ScoreOutput> outputsFromOptions(PublisherOptions options) {
        List<ScoreOutput> result = new ArrayList<>();
        if (options.getHttp() != null) {
            result.add(new HttpOutput(options));
        }
        if (options.getOutputFile() != null) {
            result.add(new FileOutput(options.getOutputFile(), options.getTmpDir()));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Outputs the given score.
     */
    void outputScore(Score score);

    /**
     * Called at the end, allows for the score output to clean itself up, e.g. close upstream HTTP connections.
     */
    void cleanup();
}
