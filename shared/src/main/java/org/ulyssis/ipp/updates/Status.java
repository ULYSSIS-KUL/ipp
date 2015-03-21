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
package org.ulyssis.ipp.updates;

public enum Status {
    NoResults {
        @Override
        public boolean isPublic() {
            return false;
        }
    }, // No results
    PreResults {
        @Override
        public boolean isPublic() {
            return false;
        }
    }, // Started, but no results are being sent yet
    Ok, // Everything is working normally
    FinalHour {
        @Override
        public boolean isPublic() {
            return false;
        }
    }, // Final hour: no new updates coming through
    FinalScore, // The final score of the run
    TempFailure, // There is a temporary problem causing updates not to come through
    PermFailure, // There is a permanent failure, so the system probably won't come online again
    Emergency, // The run is cancelled or suspended due to an emergency
    ItsComplicated; // ...it's complicated

    public boolean isPublic() {
        return true;
    }
}
