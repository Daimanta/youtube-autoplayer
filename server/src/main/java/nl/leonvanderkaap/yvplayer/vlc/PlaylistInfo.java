package nl.leonvanderkaap.yvplayer.vlc;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@Getter
public class PlaylistInfo {
    private int activeIndex;
    private String playbackState;
    List<PlaylistItem> items;

    public PlaylistInfo(int activeIndex, String playbackState, List<PlaylistItem> items) {
        this.activeIndex = activeIndex;
        this.playbackState = playbackState;
        this.items = items;
    }

    public static PlaylistInfo EMPTY = new PlaylistInfo(-1, "stopped", Collections.emptyList());
}
