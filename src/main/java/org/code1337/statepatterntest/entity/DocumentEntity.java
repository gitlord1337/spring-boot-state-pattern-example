package org.code1337.statepatterntest.entity;

import lombok.*;
import org.code1337.statepatterntest.entity.state.JobState;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "document")
public class DocumentEntity {
    @Id
    private String id;

    private String templateName;

    private List<DocumentAttachment> attachments = new LinkedList<>();

    private String format;

    private String documentUrl;

    private JobState rendered;
}