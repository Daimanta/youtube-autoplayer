package nl.leonvanderkaap.youtube;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

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

        if (LiveSettings.getAllValues().stream().anyMatch(Objects::isNull)) {
            log.error("Some essential configuration values are null, this shouldn't happen. Exiting.");
            System.exit(1);
        }

        if (LiveSettings.vlcPort < 1) {
            log.error("Vlc port should be a positive number. Exiting.");
            System.exit(1);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(LiveSettings.ytdlp, "--version");
        try {
            processBuilder.start();
        } catch (IOException e) {
            log.error(String.format("Could not find ytdlp command at '%s'. Exiting.", LiveSettings.ytdlp));
            System.exit(1);
        }
    }

}
