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
