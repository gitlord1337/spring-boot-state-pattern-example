package org.code1337.statepatterntest.entity.state;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * to track the current state
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document
public class JobTracking {
    private JobState open;
    private JobState enrichData;
    private JobState processRenderJobs;
    private JobState sendOutput;
    private JobState finished;
}