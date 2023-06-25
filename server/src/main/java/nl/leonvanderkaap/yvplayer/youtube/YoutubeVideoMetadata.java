package nl.leonvanderkaap.yvplayer.youtube;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeVideoMetadata {
    private String id;
    private String title;
    private List<YoutubeVideoMetadataFormat> formats;


    @NoArgsConstructor
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoutubeVideoMetadataFormat {
        private String format_id;
        private List<YoutubeVideoMetadataFragment> fragments;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoutubeVideoMetadataFragment {
        private float duration;
    }
}
