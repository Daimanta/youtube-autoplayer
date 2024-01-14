package nl.leonvanderkaap.yvplayer.vlc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public List<PlaylistItem> toPlaylistItems(Map<String, SupplementalItemInfo> supplementalInformation) {
        List<PlaylistItem> result = new ArrayList<>();
        ListDetails listDetails = this.item.getItem().get(0);
        if (listDetails == null || listDetails.getItem() == null) return result;
        for (ItemDetails itemDetails: listDetails.getItem()) {
            PlaylistItem item = null;
            if (itemDetails.duration.equals("-1") && supplementalInformation != null) {
                SupplementalItemInfo supplementalInfo = supplementalInformation.get(itemDetails.getName());
                if (supplementalInfo != null) {
                    item = new PlaylistItem(itemDetails.id, supplementalInfo.getTitle(), supplementalInfo.getDuration());
                } else {
                    item = new PlaylistItem(itemDetails.id, itemDetails.name, Integer.parseInt(itemDetails.duration));
                }
            } else {
                item = new PlaylistItem(itemDetails.id, itemDetails.name, Integer.parseInt(itemDetails.duration));
            }

            result.add(item);
        }
        return result;
    }
}
