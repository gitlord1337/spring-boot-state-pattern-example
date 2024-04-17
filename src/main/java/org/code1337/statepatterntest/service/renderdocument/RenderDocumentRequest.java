package org.code1337.statepatterntest.service.renderdocument;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class RenderDocumentRequest {
    private boolean attachment;
    private String documentDispatchJobId;
    private String documentId;
    private String templateName;
    private String format;
}
