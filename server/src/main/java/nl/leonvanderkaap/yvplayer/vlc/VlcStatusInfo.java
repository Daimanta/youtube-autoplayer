package nl.leonvanderkaap.yvplayer.vlc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
public class VlcStatusInfo {
    private String volume;
    private String currentplid;
    private String state;
    private int length;
    private double position;
}
