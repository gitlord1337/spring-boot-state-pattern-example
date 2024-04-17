package org.code1337.statepatterntest.service.enrichdata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class EnrichDataRequest {
    private String documentDispatchJobId;
    private String documentId;
}
