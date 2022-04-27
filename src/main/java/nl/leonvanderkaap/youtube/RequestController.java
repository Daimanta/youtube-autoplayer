package nl.leonvanderkaap.youtube;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
public class RequestController {

    @GetMapping(path = "/play")
    public void downloadAndPlay(@RequestParam(required = true) String video) {

        String id = UUID.randomUUID().toString();
        String folderPath = LiveSettings.tempfolder;
        if (!folderPath.endsWith(File.separator)) {
            folderPath += File.separator;
        }
        String fullPath = folderPath + id;

        ProcessBuilder downloadProcessBuilder = new ProcessBuilder("yt-dlp", video, "--sponsorblock-remove", "all","-P", LiveSettings.tempfolder, "-o", id);
        try {
            Process downloadProcess = downloadProcessBuilder.start();
            downloadProcess.onExit().get();
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            return;
        }
        // If we arrive here, the download has completed with error(?). We can now play the file
        ProcessBuilder playProcessBuilder = new ProcessBuilder(LiveSettings.vlc, fullPath, "-f");
        try {
            Process playProcess = playProcessBuilder.start();
            playProcess.onExit().get();
        } catch (Exception e) {
            log.warn("Play failed", e);
            return;
        }
    }
}
