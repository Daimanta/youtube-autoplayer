package nl.leonvanderkaap.yvplayer.vlc;

import lombok.Getter;

import java.util.HashMap;

@Getter
public class SupplementalItemInfo {

    public static final HashMap<String, SupplementalItemInfo> SUPPLEMENTAL_INFO = new HashMap<>();
    private String fileName;
    private String title;
    private int duration;

    public SupplementalItemInfo(String fileName, String title, int duration) {
        this.fileName = fileName;
        this.title = title;
        this.duration = duration;
    }
}
