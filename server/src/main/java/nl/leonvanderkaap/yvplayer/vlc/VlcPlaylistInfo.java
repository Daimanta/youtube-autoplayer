package nl.leonvanderkaap.yvplayer.vlc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.leonvanderkaap.yvplayer.PlaylistItem;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
public class VlcPlaylistInfo {

    private OuterWrapper item;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Getter
    public static class OuterWrapper {
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ListDetails> item;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Getter
    public static class ListDetails {
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ItemDetails> item;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @Getter
    public static class ItemDetails {
        private String id;
        private String name;
        private String duration;
    }

    public List<PlaylistItem> toPlaylistItems() {
        List<PlaylistItem> result = new ArrayList<>();
        ListDetails listDetails = this.item.getItem().get(0);
        if (listDetails == null || listDetails.getItem() == null) return result;
        for (ItemDetails itemDetails: listDetails.getItem()) {
            result.add(new PlaylistItem(itemDetails.id, itemDetails.name, Integer.parseInt(itemDetails.duration)));
        }
        return result;
    }
}
