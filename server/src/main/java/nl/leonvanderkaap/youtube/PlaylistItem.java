package nl.leonvanderkaap.youtube;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PlaylistItem {
    private String id;
    private String title;
    private int duration;

    public PlaylistItem(String id, String title, int duration) {
        this.id = id;
        this.title = title;
        this.duration = duration;
    }

}
