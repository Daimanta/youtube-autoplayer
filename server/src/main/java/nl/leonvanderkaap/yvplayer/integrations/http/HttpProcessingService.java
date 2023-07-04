package nl.leonvanderkaap.yvplayer.integrations.http;

import lombok.RequiredArgsConstructor;
import nl.leonvanderkaap.yvplayer.FileQueueService;
import nl.leonvanderkaap.yvplayer.commons.LiveSettings;
import nl.leonvanderkaap.yvplayer.management.MessageLog;
import nl.leonvanderkaap.yvplayer.management.StatusService;
import nl.leonvanderkaap.yvplayer.management.StatusType;
import nl.leonvanderkaap.yvplayer.vlc.VlcCommunicatorService;
import nl.leonvanderkaap.yvplayer.vlc.VlcPlaylistBuilder;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HttpProcessingService implements FileQueueService {

    public static final List<String> MOVIE_EXTENSIONS = List
            .of("webm", "mkv", "flv", "vob", "ogg", "ogv", "drc", "gif", "gifv", "mng", "avi",
                    "mts", "m2ts", "ts", "mov", "qt", "wmv", "yuv", "rm", "rmvb", "viv", "asf", "amv",
                    "mp4", "m4p", "m4v", "mpg", "mp2", "mpeg", "mpe", "mpv", "m2v", "svi", "3gp", "3g2", "mxf",
                    "roq", "nsv", "f4v", "f4p", "f4a", "f4b");

    private final VlcPlaylistBuilder vlcPlaylistBuilder;
    private final VlcCommunicatorService vlcCommunicatorService;
    private final StatusService statusService;

    @Override
    public void downloadAndQueueVideo(String video) {
        String lowerCased = video.toLowerCase();
        if (MOVIE_EXTENSIONS.stream().noneMatch(x -> lowerCased.endsWith("."+x))) {
            statusService.addError("Not a recognized video file");
            return;
        }

        int lastSlash = video.lastIndexOf("/");
        if (lastSlash == -1) return;
        String title = video.substring(lastSlash);

        URL url;
        try {
            url = new URL(video);
        } catch (MalformedURLException e) {
            return;
        }

        String targetFileName = UUID.randomUUID().toString();
        String folderPath = LiveSettings.getDownloadFolder();
        if (!folderPath.endsWith(File.separator)) folderPath += File.separator;
        String fullPath = folderPath + targetFileName;

        statusService.addStartedDownload(title);
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fullPath)) {
            byte[] dataBuffer = new byte[1024 * 1024 * 8];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return;
        }

        String playlistLocation = vlcPlaylistBuilder.buildRegularPlaylistFile(fullPath, title, targetFileName, -1);
        vlcCommunicatorService.addItemToPlayList(playlistLocation);
        statusService.addedToQueue(title);
    }
}
