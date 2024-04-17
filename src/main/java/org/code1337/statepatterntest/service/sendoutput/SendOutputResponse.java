package org.code1337.statepatterntest.service.sendoutput;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class SendOutputResponse {
    private String documentDispatchJobId;
    private String documentId;
    boolean error;
    private String errorMessage;
}
