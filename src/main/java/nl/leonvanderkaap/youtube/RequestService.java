package nl.leonvanderkaap.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

@Service
@Slf4j
public class RequestService {

    private Executor executor;

    // Enforces the order of added videos
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public RequestService(Executor executor) {
        this.executor = executor;
    }

    public void playVideo(String video) {
        executor.execute(getPlayVideoFuture(video));
    }

    private FutureTask<Void> getPlayVideoFuture(String video) {
        return new FutureTask<>(() -> {
            String fileIdName = UUID.randomUUID().toString();
            queue.offer(fileIdName);
            String folderPath = LiveSettings.tempfolder;
            if (!folderPath.endsWith(File.separator)) {
                folderPath += File.separator;
            }
            String fullPath = folderPath + fileIdName;
            log.debug(String.format("Downloading '%s'", video));
            boolean downloadSuccess = downloadVideo(video, fileIdName);
            log.debug(String.format("Queueing '%s'", video));
            while (!fileIdName.equals(queue.peek()) && !queue.isEmpty()) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    queue.remove(fileIdName);
                    return null;
                }
            }

            if (!downloadSuccess) {
                log.warn("Download failed!");
                queue.remove(fileIdName);
                return null;
            }

            // If we arrive here, the download has completed without error(?). We can now play the file
            playVideo(video, fullPath, fileIdName);
            File target = new File(fullPath);
            target.deleteOnExit();

            return null;
        });
    }

    private String buildPlaylistFile(ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity, String fullPath, String title, String id) throws IOException {
        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            StringBuilder builder = new StringBuilder();
            double start = 0.0;
            double end = 0.0;
            builder.append("#EXTM3U'\n");
            builder.append("#EXTINF:-1,");
            builder.append(title);
            builder.append("\n");
            for (SponsorBlockVideoSegmentResponse segment: responseEntity.getBody()) {
                end = segment.getSegment().get(0);
                builder.append("#EXTINF:-1,");
                builder.append(title);
                builder.append("\n");
                builder.append("#EXTVLCOPT:start-time=");
                builder.append(start);
                builder.append("\n");
                builder.append("#EXTVLCOPT:stop-time=");
                builder.append(end);
                builder.append("\n");
                builder.append(id);
                builder.append("\n");
                start = segment.getSegment().get(1);
            }
            builder.append("#EXTINF:-1,");
            builder.append(title);
            builder.append("\n");
            builder.append("#EXTVLCOPT:start-time=");
            builder.append(start);
            builder.append("\n");
            builder.append(id);
            builder.append("\n");

            return createFileFromFullPath(fullPath, builder.toString());
        } else {
            return buildPlaylistFile(fullPath, title, id);
        }
    }

    private String buildPlaylistFile(String fullPath, String title, String id) throws IOException {
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

    private boolean downloadVideo(String video, String fileIdName) {
        List<String> downloadArgumentList = new ArrayList<>();
        downloadArgumentList.add(LiveSettings.ytdlp);
        downloadArgumentList.add(wrap(video));
        downloadArgumentList.add("--write-info-json");
        downloadArgumentList.addAll(List.of("-P", wrap(LiveSettings.tempfolder), "-o", wrap(fileIdName)));
        downloadArgumentList.addAll(List.of("-f", String.format("\"best[height<=%s]\"", LiveSettings.maxResolution)));
        String[] downloadArguments = downloadArgumentList.toArray(new String[]{});

        ProcessBuilder downloadProcessBuilder = new ProcessBuilder(downloadArguments);
        try {
            Process downloadProcess = downloadProcessBuilder.start();
            // Don't remove the lines reading the output
            // For some reason, some video downloads(especially bigger ones) break when the output is not read
            BufferedReader in = new BufferedReader(new InputStreamReader(downloadProcess.getInputStream()));
            while ((in.readLine()) != null);
            Process downloadResult = downloadProcess.onExit().get();
            if (downloadResult.exitValue() != 0) throw new RuntimeException();
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            queue.remove(fileIdName);
            return false;
        }
        return true;
    }

    private void playVideo(String video, String fullPath, String fileIdName) {
        List<String> playArgumentList = new ArrayList<>();
        playArgumentList.add(LiveSettings.vlc);

        String playListLocation = buildPlaylist(video, fullPath, fileIdName);
        if (playListLocation == null) return;
        playArgumentList.add(wrap(playListLocation));
        playArgumentList.add("--fullscreen");
        playArgumentList.addAll(List.of("--one-instance", "--playlist-enqueue"));
        String[] playArguments = playArgumentList.toArray(new String[]{});

        ProcessBuilder playProcessBuilder = new ProcessBuilder(playArguments);
        log.debug("Adding vlc video to queue");
        try {
            if (!queue.isEmpty()) {
                queue.poll();
            }
            Process playProcess = playProcessBuilder.start();
            playProcess.onExit().get();
        } catch (Exception e) {
            log.warn("Play failed", e);
        }
    }

    private String buildPlaylist(String video, String fullPath, String fileIdName) {
        ObjectMapper objectMapper = new ObjectMapper();
        String title;
        try {
            File metadataFile = new File(fullPath+".info.json");
            JsonNode node = objectMapper.readTree(metadataFile);
            JsonNode titleNode = node.get("title");
            title = titleNode.asText();
            metadataFile.delete();
        } catch (IOException e) {
            log.warn("Could not read video metadata file");
            return null;
        }
        if (LiveSettings.blockSponsors) {
            log.debug("Building sponsorblock file");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity = null;
            try {
                responseEntity = restTemplate.getForEntity("https://sponsor.ajay.app/api/skipSegments?videoID=" + video, SponsorBlockVideoSegmentResponse[].class, Collections.emptyMap());
            } catch (Exception ignored) {}
            try {
                return buildPlaylistFile(responseEntity, fullPath, title, fileIdName);
            } catch (Exception e) {
                try {
                    return buildPlaylistFile(fullPath, title, fileIdName);
                } catch (IOException ex) {
                    log.warn("Could not construct playback file");
                    return null;
                }
            }
        } else {
            try {
                return buildPlaylistFile(fullPath, title, fileIdName);
            } catch (IOException e) {
                log.warn("Could not construct playback file");
                return null;
            }
        }
    }

    private static String wrap(String str) {
        return "\""+str+"\"";
    }
}
