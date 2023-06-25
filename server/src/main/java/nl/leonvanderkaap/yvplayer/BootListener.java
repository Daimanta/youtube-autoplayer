package nl.leonvanderkaap.yvplayer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
public class BootListener {

    private ApplicationSettings applicationSettings;

    public BootListener(ApplicationSettings applicationSettings) {
        this.applicationSettings = applicationSettings;
    }

    @EventListener
    public void sanityCheck(ApplicationStartedEvent event) {
        if (applicationSettings.getYtdlp() != null) LiveSettings.ytdlp = applicationSettings.getYtdlp();
        if (applicationSettings.getTempfolder() != null) LiveSettings.tempfolder = applicationSettings.getTempfolder();
        if (applicationSettings.getVlc() != null) LiveSettings.vlc = applicationSettings.getVlc();
        if (applicationSettings.getVlcpassword() != null) LiveSettings.vlcPassword = applicationSettings.getVlcpassword();

        LiveSettings.blockSponsors = applicationSettings.isBlocksponsors();
        LiveSettings.maxResolution = applicationSettings.getMaxresolution();
        LiveSettings.vlcPort = applicationSettings.getVlcport();

        if (SystemUtils.IS_OS_WINDOWS) {
            if (LiveSettings.ytdlp == null) LiveSettings.ytdlp = "yt-dlp.exe";
            if (LiveSettings.tempfolder == null) LiveSettings.tempfolder = "C:\\temp";
            if (LiveSettings.vlc == null) LiveSettings.vlc = "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe";
        } else {
            // Assuming Linux
            if (LiveSettings.ytdlp == null) LiveSettings.ytdlp = "ytdlp";
            if (LiveSettings.tempfolder == null) LiveSettings.tempfolder = "/tmp";
            if (LiveSettings.vlc == null) LiveSettings.vlc = "vlc";
        }
        List<Object> liveSettings = LiveSettings.getAllValues();

        for (int i=0;i<liveSettings.size();i++) {
            if (liveSettings.get(i) == null) {
                log.error("Parameter at index " + i +" is null but must not be. Exiting.");
                throw new RuntimeException();
            }
        }


        if (LiveSettings.vlcPort < 1) {
            log.error("Vlc port should be a positive number. Exiting.");
            throw new RuntimeException();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(LiveSettings.ytdlp, "--version");
        try {
            processBuilder.start();
        } catch (IOException e) {
            log.error(String.format("Could not find ytdlp command at '%s'. Exiting.", LiveSettings.ytdlp));
            throw new RuntimeException();
        }

        File downloadFolder = new File(getDownloadTempFolder());
        if (!downloadFolder.exists()) {
            try {
                Files.createDirectory(Paths.get(getDownloadTempFolder()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static String getDownloadTempFolder() {
        String result = LiveSettings.tempfolder;
        if (!result.endsWith(File.separator)) {
            result += File.separator;
        }
        result += LiveSettings.DOWNLOAD_POSTFIX;
        return result;
    }

}
