package nl.leonvanderkaap.youtube;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LiveSettings {

    public static String ytdlp;
    public static String tempfolder;
    public static String vlc;
    public static List<Object> getAllValues() {
        Object[] resultArray = new Object[]{ytdlp, tempfolder, vlc};
        return Arrays.stream(resultArray).toList();
    }
}
