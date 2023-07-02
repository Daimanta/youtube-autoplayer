package nl.leonvanderkaap.yvplayer;


import java.io.File;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class LiveSettings {

    public static final String DOWNLOAD_POSTFIX = "ytdownloads";

    public static String ytdlp;
    public static String tempfolder;
    public static String vlc;
    public static int vlcPort;
    public static String vlcPassword;

    public static boolean blockSponsors;
    public static String maxResolution;
    public static List<Object> getAllValues() {
        Object[] resultArray = new Object[]{ytdlp, tempfolder, vlc, blockSponsors, maxResolution, vlcPassword};
        return Arrays.stream(resultArray).toList();
    }

    public static String vlcPasswordBasicAuth() {
        return Base64.getEncoder().encodeToString((":"+vlcPassword).getBytes());
    }

    public static String getDownloadFolder() {
        String folderPath = LiveSettings.tempfolder;
        if (!folderPath.endsWith(File.separator)) {
            folderPath += File.separator;
        }

        folderPath += LiveSettings.DOWNLOAD_POSTFIX;
        return folderPath;
    }
}
