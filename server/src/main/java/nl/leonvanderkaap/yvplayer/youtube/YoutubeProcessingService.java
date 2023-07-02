package nl.leonvanderkaap.yvplayer.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.FileQueueService;
import nl.leonvanderkaap.yvplayer.LiveSettings;
import nl.leonvanderkaap.yvplayer.vlc.VlcCommunicatorService;
import nl.leonvanderkaap.yvplayer.vlc.VlcPlaylistBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class YoutubeProcessingService implements FileQueueService {

    private final YoutubeDownloadService youtubeDownloadService;
    private final VlcPlaylistBuilder vlcPlaylistBuilder;
    private final VlcCommunicatorService vlcCommunicatorService;

    public void downloadAndQueueVideo(String video) {
        Optional<FileInformation> fileInformationOpt = youtubeDownloadService.download(video);
        if (fileInformationOpt.isEmpty()) {
            log.warn("Download failed!");
            return;
        }

        FileInformation fileInformation = fileInformationOpt.get();

        // If we arrive here, the download has completed without error(?). We can now play the file
        queueVideo(fileInformation.path(), fileInformation.fileId());
        File target = new File(fileInformation.path());
        target.deleteOnExit();
    }

    private void queueVideo(String fullPath, String fileIdName) {
        log.debug("Trying to enqueue file");
        String playListLocation = buildPlaylist(fullPath, fileIdName);
        if (playListLocation == null) return;
        ResponseEntity<String> response = vlcCommunicatorService.addItemToPlayList(playListLocation);

        log.debug("Adding vlc video to queue");
    }

    private String buildPlaylist(String fullPath, String fileIdName) {
        ObjectMapper objectMapper = new ObjectMapper();
        String title;
        try {
            File metadataFile = new File(fullPath+".info.json");
            YoutubeVideoMetadata node = objectMapper.readValue(metadataFile, YoutubeVideoMetadata.class);
            title = node.getTitle();
            int duration = -1;
            try {
                duration = (int) node.getFormats().get(0).getFragments().get(0).getDuration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (LiveSettings.blockSponsors) {
                return vlcPlaylistBuilder.buildSponsorCheckedPlayListFile(node.getId(), fullPath, title, fileIdName, duration);
            } else {
                return vlcPlaylistBuilder.buildRegularPlaylistFile(fullPath, title, fileIdName, duration);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Could not read video metadata file");
            return null;
        }
    }

}
