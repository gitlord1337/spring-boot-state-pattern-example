package org.code1337.statepatterntest.service.renderdocument;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RenderDocumentResponse {
    private boolean attachment;
    private String documentDispatchJobId;
    private String documentId;
    private String documentUrl;
}
