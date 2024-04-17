package org.code1337.statepatterntest.service.sendoutput;

import lombok.*;
import org.code1337.statepatterntest.entity.Address;
import org.code1337.statepatterntest.entity.DocumentEntity;
import org.code1337.statepatterntest.entity.ShippingChannel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SendOutputRequest {
    private String documentDispatchJobId;
    private String documentId;
    private Address address;
    private ShippingChannel shippingChannel;
    private DocumentEntity document;
}
