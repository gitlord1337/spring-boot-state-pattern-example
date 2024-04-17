package org.code1337.statepatterntest.service.switchstate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.code1337.statepatterntest.entity.state.State;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class SwitchStateRequest {
    private String documentDispatchJobId;
    private State nextState;
}
