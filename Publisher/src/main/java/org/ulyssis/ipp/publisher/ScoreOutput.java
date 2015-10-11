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

public interface ScoreOutput {
    static List<ScoreOutput> outputsFromOptions(PublisherOptions options) {
        List<ScoreOutput> result = new ArrayList<>();
        if (options.getHttp() != null) {
            result.add(new HttpOutput());
        }
        if (options.getOutputFile() != null) {
            result.add(new FileOutput(options.getOutputFile()));
        }
        return Collections.unmodifiableList(result);
    }

    void outputScore(Score score);
    void cleanup();
}
