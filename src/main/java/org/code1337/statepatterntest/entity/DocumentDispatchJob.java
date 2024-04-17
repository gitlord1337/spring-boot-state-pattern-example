package org.code1337.statepatterntest.entity;

import lombok.*;
import org.code1337.statepatterntest.entity.state.JobTracking;
import org.code1337.statepatterntest.entity.state.State;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "document_dispatch_job")
public class DocumentDispatchJob {
    @Id
    private String id;

    private Address address;

    private ShippingChannel shippingChannel;

    private DocumentEntity document;

    private EnrichtedData enrichtedData;

    private State state = State.OPEN;

    private JobTracking jobTracking = new JobTracking();
}