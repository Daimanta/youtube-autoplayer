package nl.leonvanderkaap.yvplayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.vlc.VlcCommunicatorService;
import nl.leonvanderkaap.yvplayer.vlc.VlcPlaylistBuilder;
import nl.leonvanderkaap.yvplayer.vlc.VlcPlaylistInfo;
import nl.leonvanderkaap.yvplayer.vlc.VlcStatusInfo;
import nl.leonvanderkaap.yvplayer.youtube.SponsorBlockVideoSegmentResponse;
import nl.leonvanderkaap.yvplayer.youtube.YoutubeDownloadService;
import nl.leonvanderkaap.yvplayer.youtube.YoutubeVideoMetadata;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

@Service
@Slf4j
public class RequestService {

    private Executor executor;
    private YoutubeDownloadService youtubeDownloadService;
    private VlcCommunicatorService vlcCommunicatorService;
    private VlcPlaylistBuilder vlcPlaylistBuilder;

    // Enforces the order of added videos
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public RequestService(Executor executor, YoutubeDownloadService youtubeDownloadService, VlcCommunicatorService vlcCommunicatorService, VlcPlaylistBuilder vlcPlaylistBuilder) {
        this.executor = executor;
        this.youtubeDownloadService = youtubeDownloadService;
        this.vlcCommunicatorService = vlcCommunicatorService;
        this.vlcPlaylistBuilder = vlcPlaylistBuilder;
    }

    public void queueVideo(String video) {
        executor.execute(getVideoQueueFuture(video));
    }

    public void togglePlay() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.togglePlay();
            return null;
        }));
    }

    public void play() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.play();
            return null;
        }));
    }

    public void pause() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.pause();
            return null;
        }));
    }

    public void stop() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.stop();
            return null;
        }));
    }

    public void next() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.next();
            return null;
        }));
    }

    public void previous() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.previous();
            return null;
        }));
    }

    public void volumeUp() {
        executor.execute(new FutureTask<>(() -> {
            VlcStatusInfo status = vlcCommunicatorService.getStatus();
            int volume = Integer.parseInt(status.getVolume());
            int newVolume = volume + 12;
            if (newVolume > 512) newVolume = 512;
            vlcCommunicatorService.setVolume(newVolume);
            return null;
        }));
    }

    public void volumeDown() {
        executor.execute(new FutureTask<>(() -> {
            VlcStatusInfo status = vlcCommunicatorService.getStatus();
            int volume = Integer.parseInt(status.getVolume());
            int newVolume = volume - 12;
            if (newVolume < 0) newVolume = 0;
            vlcCommunicatorService.setVolume(newVolume);
            return null;
        }));
    }

    public void fullScreen() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.fullScreen();
            return null;
        }));
    }

    public void emptyPlaylist() {
        executor.execute(new FutureTask<>(() -> {
            vlcCommunicatorService.emptyPlaylist();
            return null;
        }));
    }

    public void selectItem(String item) {
        executor.execute(new FutureTask<>(() -> {
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

    public PlaylistInfo getPlaylist() {

        List<PlaylistItem> items;

        ResponseEntity<String> playlistEntity = vlcCommunicatorService.getPlaylist("localhost");
        String playlistXmlString = playlistEntity.getBody();
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
        return new FutureTask<>(() -> {
            FileInformation fileInformation = youtubeDownloadService.download(video);
            if (fileInformation == null) {
                log.warn("Download failed!");
                return null;
            }

            // If we arrive here, the download has completed without error(?). We can now play the file
            queueVideo(fileInformation.path(), fileInformation.fileId());
            File target = new File(fileInformation.path());
            target.deleteOnExit();

            return null;
        });
    }

    private void queueVideo(String fullPath, String fileIdName) {
        log.debug("Trying to enqueue file");
        String playListLocation = buildPlaylist(fullPath, fileIdName);
        if (playListLocation == null) return;
        ResponseEntity<String> response = doRequest("localhost", "command=in_enqueue&input=file:///" + playListLocation.replace("\\", "/"));

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

    private static <T> ResponseEntity<T> get(RestTemplate restTemplate, URI uri, Map<String, String> headers, Class<T> clazz) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (String header: headers.keySet()) {
            httpHeaders.set(header, headers.get(header));
        }
        httpHeaders.setContentType(MediaType.TEXT_XML);
        HttpEntity<Void> requestEntity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, clazz);
    }

    private static ResponseEntity<String> doRequest(String host, String command) {
        RestTemplate restTemplate = new RestTemplate();
        URI enqueueURL;
        try {
            enqueueURL = new URI("http", null, host, LiveSettings.vlcPort, "/requests/status.xml", command, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return get(restTemplate, enqueueURL, Map.of("Authorization", "Basic " + LiveSettings.vlcPasswordBasicAuth()), String.class);
    }


}
