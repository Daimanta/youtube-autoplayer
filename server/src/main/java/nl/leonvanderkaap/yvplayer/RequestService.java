package nl.leonvanderkaap.yvplayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
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

    // Enforces the order of added videos
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public RequestService(Executor executor, YoutubeDownloadService youtubeDownloadService) {
        this.executor = executor;
        this.youtubeDownloadService = youtubeDownloadService;
    }

    public void queueVideo(String video) {
        executor.execute(getVideoQueueFuture(video));
    }

    public void togglePlay() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_pause");
            return null;
        }));
    }

    public void play() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_pause");
            return null;
        }));
    }

    public void pause() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_pause");
            return null;
        }));
    }

    public void stop() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_stop");
            return null;
        }));
    }

    public void next() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_next");
            return null;
        }));
    }

    public void previous() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_previous");
            return null;
        }));
    }

    public void volumeUp() {
        executor.execute(new FutureTask<>(() -> {
            LinkedHashMap<String, ?> status = getStatus();
            Object volumeObject = status.get("volume");
            if (volumeObject instanceof String volumeString) {
                try {
                    int volume = Integer.parseInt(volumeString);
                    int newVolume = volume + 12;
                    if (newVolume > 512) newVolume = 512;
                    doRequest("localhost", "command=volume&val="+newVolume);
                } catch (Exception e) {}
            }
            return null;
        }));
    }

    public void volumeDown() {
        executor.execute(new FutureTask<>(() -> {
            LinkedHashMap<String, ?> status = getStatus();
            Object volumeObject = status.get("volume");
            if (volumeObject instanceof String volumeString) {
                try {
                    int volume = Integer.parseInt(volumeString);
                    int newVolume = volume - 12;
                    if (newVolume < 0) newVolume = 0;
                    doRequest("localhost", "command=volume&val="+newVolume);
                } catch (Exception e) {}
            }
            return null;
        }));
    }

    public void fullScreen() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=fullscreen");
            return null;
        }));
    }

    public void emptyPlaylist() {
        executor.execute(new FutureTask<>(() -> {
            ResponseEntity<String> response = doRequest("localhost", "command=pl_empty");
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
            ResponseEntity<String> response = doRequest("localhost", "command=pl_play&id="+itemNumber);
            return null;
        }));
    }

    public PlaylistInfo getPlaylist() {

        List<PlaylistItem> items = new ArrayList<>();

        ResponseEntity<String> playlistEntity = getPlaylist("localhost");
        String playlistXmlString = playlistEntity.getBody();
        XmlMapper xmlMapper = new XmlMapper();
        try {
            LinkedHashMap<String, ?> map = xmlMapper.readValue(playlistXmlString, LinkedHashMap.class);
            Object playListOuterWrapperObj = map.get("item");
            if (playListOuterWrapperObj instanceof LinkedHashMap<?,?> playlistOuterWrapper) {
                Object playListInnerWrapperObj = playlistOuterWrapper.get("item");
                if (playListInnerWrapperObj instanceof ArrayList<?> playlistInnerWrapper) {
                    Object listDetailsObj = playlistInnerWrapper.get(0);
                    if (listDetailsObj instanceof LinkedHashMap<?,?> listDetails) {
                        Object listItemsObj = listDetails.get("item");
                        if (listItemsObj instanceof ArrayList<?> listItems) {
                            for (Object itemObj: listItems) {
                                if (itemObj instanceof LinkedHashMap<?,?> item) {
                                    String itemId = (String) item.get("id");
                                    String title = (String) item.get("name");
                                    String duration = (String) item.get("duration");
                                    items.add(new PlaylistItem(itemId, title, Integer.parseInt(duration)));
                                }
                            }
                        } else if (listItemsObj instanceof LinkedHashMap<?,?> singleItem) {
                            String itemId = (String) singleItem.get("id");
                            String title = (String) ((LinkedHashMap<?,?>)singleItem.get("content")).get("name");
                            String duration = (String) singleItem.get("duration");
                            items.add(new PlaylistItem(itemId, title, Integer.parseInt(duration)));
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        LinkedHashMap<String, ?> status = getStatus();
        String currentIdString = (String) status.get("currentplid");
        String state = (String) status.get("state");

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

    private String buildPlaylistFile(ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity, String fullPath, String title, String id, int duration) throws IOException {
        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            return buildSponsorBlockPlaylist(responseEntity.getBody(), fullPath, title, id, duration);
        } else {
            return buildPlaylistFile(fullPath, title, id, duration);
        }
    }

    private String buildSponsorBlockPlaylist(SponsorBlockVideoSegmentResponse[] segments, String fullPath, String title, String id, int duration) throws IOException{
        if (segments.length == 0) return buildPlaylistFile(fullPath, title, id, duration);
        StringBuilder builder = new StringBuilder();
        double start = 0.0;
        double end = 0.0;
        builder.append("#EXTM3U'\n");
        int index = 1;
        for (SponsorBlockVideoSegmentResponse segment: segments) {
            end = segment.getSegment().get(0);
            addPlaylistTitleSegment(builder, (int) (end - start), index++, title);
            addPlaylistStartSegment(builder, start);
            addPlaylistEndSegment(builder, end);
            addPlaylistFilenameSegment(builder, id);
            start = segment.getSegment().get(1);
        }
        addPlaylistTitleSegment(builder, duration-(int)start, index, title);
        addPlaylistStartSegment(builder, start);
        addPlaylistFilenameSegment(builder, id);

        return createFileFromFullPath(fullPath, builder.toString());
    }

    private String toDurationString(int duration) {
        if (duration >= 3600) {
            int hours = duration / 3600;
            int rest = duration % 3600;
            int minutes = rest / 60;
            int seconds = rest % 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (duration >= 60) {
            int minutes = duration / 60;
            int seconds = duration % 60;
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return "00:" + String.format("%02d", duration);
        }
    }

    private void addPlaylistStartSegment(StringBuilder builder, double start) {
        builder.append("#EXTVLCOPT:start-time=");
        builder.append(start);
        builder.append("\n");
    }

    private void addPlaylistEndSegment(StringBuilder builder, double end) {
        builder.append("#EXTVLCOPT:stop-time=");
        builder.append(end);
        builder.append("\n");
    }

    private void addPlaylistTitleSegment(StringBuilder builder, int length, int index, String title) {
        builder.append("#EXTINF:");
        builder.append(length);
        builder.append(",");
        builder.append(title);
        builder.append(" (");
        builder.append(index);
        builder.append(") ");
        builder.append(toDurationString(length));
        builder.append("\n");
    }

    private void addPlaylistFilenameSegment(StringBuilder builder, String id) {
        builder.append(id);
        builder.append("\n");
    }

    private String buildPlaylistFile(String fullPath, String title, String id, int duration) throws IOException {
        String formatString =
                """
                #EXTM3U
                #EXTINF:-1,%s
                %s
                """;
        String fileString = String.format(formatString, title, id);
        return createFileFromFullPath(fullPath, fileString);
    }

    private String createFileFromFullPath(String fullPath, String fileContents) throws IOException{
        String playlistPath = fullPath + ".m3u8";
        BufferedWriter bw = new BufferedWriter(new FileWriter(playlistPath));
        bw.write(fileContents);
        bw.close();
        return playlistPath;
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
                return buildSponsorCheckedPlayListFile(node.getId(), fullPath, title, fileIdName, duration);
            } else {
                return buildRegularPlaylistFile(fullPath, title, fileIdName, duration);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Could not read video metadata file");
            return null;
        }

    }

    private String buildSponsorCheckedPlayListFile(String video, String fullPath, String title, String fileIdName, int duration) {
        log.debug("Building sponsorblock file");
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity = null;
        try {
            responseEntity = restTemplate.getForEntity("https://sponsor.ajay.app/api/skipSegments?videoID=" + video, SponsorBlockVideoSegmentResponse[].class, Collections.emptyMap());
        } catch (Exception ignored) {}
        try {
            return buildPlaylistFile(responseEntity, fullPath, title, fileIdName, duration);
        } catch (Exception e) {
            try {
                return buildPlaylistFile(fullPath, title, fileIdName, duration);
            } catch (IOException ex) {
                log.warn("Could not construct playback file");
                return null;
            }
        }
    }

    private String buildRegularPlaylistFile(String fullPath, String title, String fileIdName, int duration) {
        try {
            return buildPlaylistFile(fullPath, title, fileIdName, duration);
        } catch (IOException e) {
            log.warn("Could not construct playback file");
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

    private static ResponseEntity<String> getPlaylist(String host) {
        RestTemplate restTemplate = new RestTemplate();
        URI enqueueURL;
        try {
            enqueueURL = new URI("http", null, host, LiveSettings.vlcPort, "/requests/playlist_jstree.xml", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return get(restTemplate, enqueueURL, Map.of("Authorization", "Basic " + LiveSettings.vlcPasswordBasicAuth()), String.class);
    }

    private static LinkedHashMap<String, ?> getStatus() {
        ResponseEntity<String> responseStirng = doRequest("localhost", null);
        XmlMapper xmlMapper = new XmlMapper();
        try {
            return xmlMapper.readValue(responseStirng.getBody(), LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
