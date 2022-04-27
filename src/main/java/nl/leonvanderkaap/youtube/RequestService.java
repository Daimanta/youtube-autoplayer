package nl.leonvanderkaap.youtube;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
            boolean downloadSuccess = downloadVideo(video, fileIdName);
            while (!fileIdName.equals(queue.peek()) && !queue.isEmpty()) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    queue.clear();
                    return null;
                }
            }

            if (!downloadSuccess) {
                log.warn("Download failed!");
                if (!queue.isEmpty()) queue.poll();
                return null;
            }

            // If we arrive here, the download has completed without error(?). We can now play the file
            playVideo(video, fullPath, fileIdName);
            File target = new File(fullPath);
            target.deleteOnExit();

            return null;
        });
    }

    private String buildPlaylistFile(ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity, String fullPath, String id) throws IOException {
        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            StringBuilder builder = new StringBuilder();
            int start = 0;
            int end = 0;
            for (SponsorBlockVideoSegmentResponse segment: responseEntity.getBody()) {
                end = segment.getSegment().get(0).intValue();
                builder.append("#EXTVLCOPT:start-time=");
                builder.append(start);
                builder.append("\n");
                builder.append("#EXTVLCOPT:stop-time=");
                builder.append(end);
                builder.append("\n");
                builder.append(id);
                builder.append("\n");
                start = segment.getSegment().get(1).intValue();
            }
            builder.append("#EXTVLCOPT:start-time=");
            builder.append(start);
            builder.append("\n");
            builder.append(id);
            builder.append("\n");

            String playlistPath = fullPath + ".m3u";
            BufferedWriter bw = new BufferedWriter(new FileWriter(playlistPath));
            bw.write(builder.toString());
            bw.close();
            return playlistPath;
        } else {
            return fullPath;
        }
    }

    private boolean downloadVideo(String video, String fileIdName) {
        List<String> downloadArgumentList = new ArrayList<>();
        downloadArgumentList.add(LiveSettings.ytdlp);
        downloadArgumentList.add(video);
        downloadArgumentList.addAll(List.of("-P", LiveSettings.tempfolder, "-o", fileIdName));
        downloadArgumentList.addAll(List.of("-f", String.format("best[height<=%s]", LiveSettings.maxResolution)));
        String[] downloadArguments = downloadArgumentList.toArray(new String[]{});

        ProcessBuilder downloadProcessBuilder = new ProcessBuilder(downloadArguments);
        try {
            Process downloadProcess = downloadProcessBuilder.start();
            downloadProcess.onExit().get();
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            while (!fileIdName.equals(queue.peek()) && queue.isEmpty()) {
                try {
                    Thread.sleep(200);
                } catch (Exception ne) {
                    queue.clear();
                    return false;
                }
            }
            queue.poll();
            return false;
        }
        return true;
    }

    private void playVideo(String video, String fullPath, String fileIdName) {
        List<String> playArgumentList = new ArrayList<>();
        playArgumentList.add(LiveSettings.vlc);

        if (LiveSettings.blockSponsors) {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity = null;
            try {
                responseEntity = restTemplate.getForEntity("https://sponsor.ajay.app/api/skipSegments?videoID=" + video, SponsorBlockVideoSegmentResponse[].class, Collections.emptyMap());
            } catch (Exception ignored) {}
            try {
                playArgumentList.add(buildPlaylistFile(responseEntity, fullPath, fileIdName));
            } catch (Exception e) {
                playArgumentList.add(fullPath);
            }
        } else {
            playArgumentList.add(fullPath);
        }

        playArgumentList.add("--fullscreen");
        playArgumentList.addAll(List.of("--one-instance", "--playlist-enqueue"));
        String[] playArguments = playArgumentList.toArray(new String[]{});

        ProcessBuilder playProcessBuilder = new ProcessBuilder(playArguments);

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
}
