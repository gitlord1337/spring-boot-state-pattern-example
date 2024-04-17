package org.code1337.statepatterntest.entity;

import lombok.*;
import org.code1337.statepatterntest.entity.state.JobState;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "attachment")
public class DocumentAttachment {
    @Id
    private String id;

    private String templateName;

    private String format;

    private String documentUrl;

    private JobState rendered;
}