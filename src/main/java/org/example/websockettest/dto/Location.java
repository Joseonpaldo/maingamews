package org.example.websockettest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Location {
    private int location;
    private int landmark;
}
