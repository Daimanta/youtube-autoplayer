package nl.leonvanderkaap.yvplayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.commons.ApplicationFutureTask;
import nl.leonvanderkaap.yvplayer.integrations.file.LocalFileProcessingService;
import nl.leonvanderkaap.yvplayer.integrations.http.HttpProcessingService;
import nl.leonvanderkaap.yvplayer.integrations.smb.SmbProcessingService;
import nl.leonvanderkaap.yvplayer.management.MessageLog;
import nl.leonvanderkaap.yvplayer.management.StatusService;
import nl.leonvanderkaap.yvplayer.vlc.*;
import nl.leonvanderkaap.yvplayer.integrations.youtube.YoutubeProcessingService;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestService {

    private final Executor executor;
    private final VlcCommunicatorService vlcCommunicatorService;
    private final YoutubeProcessingService youtubeProcessingService;
    private final SmbProcessingService smbProcessingService;
    private final HttpProcessingService httpProcessingService;
    private final LocalFileProcessingService localFileProcessingService;
    private final StatusService statusService;


    public void queueVideo(String video, boolean playAfterQueue) {
        executor.execute(getVideoQueueFuture(video, playAfterQueue));
    }

    public void togglePlay() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.togglePlay();
            return null;
        }));
    }

    public void play() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            VlcStatusInfo info = vlcCommunicatorService.getStatus();
            if (info.getState() == null || !info.getState().equals("playing")) {
                vlcCommunicatorService.next();
            }
            return null;
        }));
    }

    public void pause() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.pause();
            return null;
        }));
    }

    public void stop() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.stop();
            return null;
        }));
    }

    public void next() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.next();
            return null;
        }));
    }

    public void previous() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.previous();
            return null;
        }));
    }

    public void setVolume(int volume) {
        executor.execute(new ApplicationFutureTask<>(() -> {
            // Slider value goes up to 125, this translates to value 320
            // Max possible value is 200 at 512 but this is not shown in the VLC slider
            int value = (int) (((volume * 1.0) / 125.0) * 320.0);
            vlcCommunicatorService.setVolume(value);
            return null;
        }));
    }

    public void setTime(int percentage) {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.setTime(percentage);
            return null;
        }));
    }

    public void fullScreen() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.fullScreen();
            return null;
        }));
    }

    public void emptyPlaylist() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.emptyPlaylist();
            return null;
        }));
    }

    public void selectItem(String item) {
        executor.execute(new ApplicationFutureTask<>(() -> {
            String itemNumber;
            if (item.startsWith("plid_")) {
                itemNumber = item.substring("plid_".length());
            } else {
                itemNumber = item;
            }
            vlcCommunicatorService.selectItem(itemNumber);
            return null;
        }));
    }

    public List<MessageLog> getStatus() {
        return statusService.getStatus();
    }

    public VlcStatusInfo getVlcStatusInfo() {
        return vlcCommunicatorService.getStatus();
    }

    public PlaylistInfo getPlaylist() {

        List<PlaylistItem> items;


        ResponseEntity<String> playlistEntity;
        try {
            playlistEntity = vlcCommunicatorService.getPlaylist("localhost");
        } catch (Exception e) {
            return PlaylistInfo.EMPTY;
        }

        String playlistXmlString = playlistEntity.getBody();
        if (SystemUtils.IS_OS_WINDOWS && playlistXmlString != null) {
            playlistXmlString = new String(playlistXmlString.getBytes(StandardCharsets.ISO_8859_1));
        }

        XmlMapper xmlMapper = new XmlMapper();
        try {
            VlcPlaylistInfo vlcPlaylistInfo = xmlMapper.readValue(playlistXmlString, VlcPlaylistInfo.class);
            items = new ArrayList<>(vlcPlaylistInfo.toPlaylistItems(SupplementalItemInfo.SUPPLEMENTAL_INFO));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        VlcStatusInfo status = vlcCommunicatorService.getStatus();
        String currentIdString = status.getCurrentplid();
        String state = status.getState();

        return new PlaylistInfo(Integer.parseInt(currentIdString), state, items);
    }

    public List<String> getCurrentlyDownloading() {
        return youtubeProcessingService.getCurrentlyDownloading();
    }

    private FutureTask<Void> getVideoQueueFuture(String video, boolean playAfterQueue) {
        return new ApplicationFutureTask<>(() -> {
            FileQueueService fileQueueService;
            if (video.contains("youtube.com") || (video.length() < 13)) {
                fileQueueService = youtubeProcessingService;
            } else if (video.startsWith("smb://")) {
                fileQueueService = smbProcessingService;
            } else if (video.startsWith("http://") || video.startsWith("https://")) {
                fileQueueService = httpProcessingService;
            } else if (video.startsWith("file://")) {
                fileQueueService = localFileProcessingService;
            } else {
                return null;
            }
            fileQueueService.downloadAndQueueVideo(video);
            if (playAfterQueue) {
                play();
            }
            return null;
        });
    }



}
