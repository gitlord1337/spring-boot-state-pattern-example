package org.code1337.statepatterntest.entity.state;

public enum State {
    OPEN,
    ENRICH_DATA,
    PROCESS_RENDER_JOBS,
    SEND_OUTPUT,
    FINISHED
}
