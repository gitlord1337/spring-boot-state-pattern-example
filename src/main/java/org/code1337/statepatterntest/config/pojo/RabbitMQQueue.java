package org.code1337.statepatterntest.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RabbitMQQueue {
    private String name;
    private boolean request;
    private boolean response;
}
