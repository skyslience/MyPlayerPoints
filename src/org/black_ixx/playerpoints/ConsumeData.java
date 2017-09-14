package org.black_ixx.playerpoints;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConsumeData {

    private int id;
    private String playerName;
    private int point;
    private String info;
    private String time;
    private String server;

}
