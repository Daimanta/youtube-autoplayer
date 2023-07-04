package nl.leonvanderkaap.yvplayer.integrations.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class SponsorBlockVideoSegmentResponse {

    private List<Double> segment;
    private String UUID;
    private String category;
    private Double videoDuration;
    private String actionType;
    private String userID;
    private int locked;
    private int votes;
    private String description;
}
