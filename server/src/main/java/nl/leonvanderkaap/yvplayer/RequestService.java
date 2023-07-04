package nl.leonvanderkaap.yvplayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.commons.ApplicationFutureTask;
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
    private final StatusService statusService;


    public void queueVideo(String video) {
        executor.execute(getVideoQueueFuture(video));
    }

    public void togglePlay() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.togglePlay();
            return null;
        }));
    }

    public void play() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            vlcCommunicatorService.play();
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

    public void volumeUp() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            VlcStatusInfo status = vlcCommunicatorService.getStatus();
            int volume = Integer.parseInt(status.getVolume());
            int newVolume = volume + 12;
            if (newVolume > 512) newVolume = 512;
            vlcCommunicatorService.setVolume(newVolume);
            return null;
        }));
    }

    public void volumeDown() {
        executor.execute(new ApplicationFutureTask<>(() -> {
            VlcStatusInfo status = vlcCommunicatorService.getStatus();
            int volume = Integer.parseInt(status.getVolume());
            int newVolume = volume - 12;
            if (newVolume < 0) newVolume = 0;
            vlcCommunicatorService.setVolume(newVolume);
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
            items = new ArrayList<>(vlcPlaylistInfo.toPlaylistItems());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        VlcStatusInfo status = vlcCommunicatorService.getStatus();
        String currentIdString = status.getCurrentplid();
        String state = status.getState();

        return new PlaylistInfo(Integer.parseInt(currentIdString), state, items);
    }

    private FutureTask<Void> getVideoQueueFuture(String video) {
        return new ApplicationFutureTask<>(() -> {
            FileQueueService fileQueueService;
            if (video.contains("youtube.com") || (video.length() < 13)) {
                fileQueueService = youtubeProcessingService;
            } else if (video.startsWith("smb://")) {
                fileQueueService = smbProcessingService;
            } else if (video.startsWith("http://") || video.startsWith("https://")) {
                fileQueueService = httpProcessingService;
            } else {
                return null;
            }
            fileQueueService.downloadAndQueueVideo(video);
            return null;
        });
    }

}
