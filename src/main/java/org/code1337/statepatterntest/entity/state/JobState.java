package org.code1337.statepatterntest.entity.state;


import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class JobState {
    private boolean sent;
    private boolean ok;
    private boolean error;
    private String errorMessage;
}