package nl.leonvanderkaap.yvplayer.integrations.file;

import lombok.RequiredArgsConstructor;
import nl.leonvanderkaap.yvplayer.FileQueueService;
import nl.leonvanderkaap.yvplayer.management.StatusService;
import nl.leonvanderkaap.yvplayer.vlc.VlcCommunicatorService;
import nl.leonvanderkaap.yvplayer.vlc.VlcPlaylistBuilder;
import org.springframework.stereotype.Service;

import static nl.leonvanderkaap.yvplayer.integrations.http.HttpProcessingService.MOVIE_EXTENSIONS;

@Service
@RequiredArgsConstructor
public class LocalFileProcessingService implements FileQueueService {

    private final StatusService statusService;
    private final VlcPlaylistBuilder vlcPlaylistBuilder;
    private final VlcCommunicatorService vlcCommunicatorService;

    public static final String PATH_PREFIX = "file://";

    @Override
    public void downloadAndQueueVideo(String video) {

        String lowerCased = video.toLowerCase();
        if (MOVIE_EXTENSIONS.stream().noneMatch(x -> lowerCased.endsWith("."+x))) {
            statusService.addError("Not a recognized video file");
            return;
        }

        if (video.length() < PATH_PREFIX.length() + 1) {
            statusService.addError("Incorrect file path");
            return;
        }

        String fullPath = video.substring(PATH_PREFIX.length());
        if (!fullPath.startsWith("/")) {
            statusService.addError("Filepath must be absolute");
            return;
        }

        String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);

        String playlistLocation = vlcPlaylistBuilder.buildRegularPlaylistFile(fullPath, fileName, fileName, -1);
        vlcCommunicatorService.addItemToPlayList(playlistLocation);
        statusService.addedToQueue(fileName);
    }
}
