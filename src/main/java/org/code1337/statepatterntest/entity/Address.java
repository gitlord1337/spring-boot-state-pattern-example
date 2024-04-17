package org.code1337.statepatterntest.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document
public class Address {
    private String firstName;
    private String lastName;
    private String street;
    private String zip;
    private String email;
}