package org.code1337.statepatterntest.service.enrichdata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class EnrichDataResponse {
    private String documentDispatchJobId;
    private String documentId;
    private String data1;
    private String data2;
    private String data3;
}
