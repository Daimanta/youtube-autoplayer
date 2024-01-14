package nl.leonvanderkaap.yvplayer.integrations.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.FileQueueService;
import nl.leonvanderkaap.yvplayer.commons.LiveSettings;
import nl.leonvanderkaap.yvplayer.management.StatusService;
import nl.leonvanderkaap.yvplayer.vlc.SupplementalItemInfo;
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
    private final StatusService statusService;

    public void downloadAndQueueVideo(String video) {
        statusService.addOk(String.format("Started download of '%s'", video));
        Optional<FileInformation> fileInformationOpt = youtubeDownloadService.download(video);
        if (fileInformationOpt.isEmpty()) {
            statusService.addError("Youtube download failed");
            return;
        }

        FileInformation fileInformation = fileInformationOpt.get();

        // If we arrive here, the download has completed without error(?). We can now play the file
        queueVideo(fileInformation.path(), fileInformation.fileId());
        File target = new File(fileInformation.path());
        target.deleteOnExit();
    }

    private void queueVideo(String fullPath, String fileIdName) {
        FileListInformation playListLocation = buildPlaylist(fullPath, fileIdName);
        if (playListLocation == null) return;
        SupplementalItemInfo.SUPPLEMENTAL_INFO.put(playListLocation.fileLocation.substring(playListLocation.fileLocation.lastIndexOf(File.separator) + 1), new SupplementalItemInfo(playListLocation.fileLocation, playListLocation.title, playListLocation.duration));
        ResponseEntity<String> response = vlcCommunicatorService.addItemToPlayList(playListLocation.fileLocation);
    }

    private FileListInformation buildPlaylist(String fullPath, String fileIdName) {
        ObjectMapper objectMapper = new ObjectMapper();
        String title;
        try {
            File metadataFile = new File(fullPath+".info.json");
            YoutubeVideoMetadata node = objectMapper.readValue(metadataFile, YoutubeVideoMetadata.class);
            title = node.getTitle();
            int duration = -1;
            try {
                duration = node.getDuration();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (LiveSettings.blockSponsors) {
                statusService.addedToQueue(title);
                return new FileListInformation(title, duration, vlcPlaylistBuilder.buildSponsorCheckedPlayListFile(node.getId(), fullPath, title, fileIdName, duration));
            } else {
                statusService.addedToQueue(title);
                return new FileListInformation(title, duration, vlcPlaylistBuilder.buildRegularPlaylistFile(fullPath, title, fileIdName, duration));
            }

        } catch (IOException e) {
            statusService.addError(e.getMessage());
            return null;
        }
    }


    @Getter
    private static class FileListInformation {
        private String title;
        private int duration;
        private String fileLocation;

        public FileListInformation(String title, int duration, String fileLocation) {
            this.title = title;
            this.duration = duration;
            this.fileLocation = fileLocation;
        }
    }
}
