package nl.leonvanderkaap.youtube;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
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
            String id = UUID.randomUUID().toString();
            queue.offer(id);
            String folderPath = LiveSettings.tempfolder;
            if (!folderPath.endsWith(File.separator)) {
                folderPath += File.separator;
            }
            String fullPath = folderPath + id;

            ProcessBuilder downloadProcessBuilder = new ProcessBuilder(LiveSettings.ytdlp, video, "--sponsorblock-remove", "all","-P", LiveSettings.tempfolder, "-o", id, "-f", "best[height<=1080p]");
            try {
                Process downloadProcess = downloadProcessBuilder.start();
                downloadProcess.onExit().get();
            } catch (Exception e) {
                log.warn("Download failed: ", e);
                while (!id.equals(queue.peek()) && queue.isEmpty()) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception ne) {
                        return null;
                    }
                }
                queue.poll();
                return null;
            }
            while (!id.equals(queue.peek()) && !queue.isEmpty()) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    return null;
                }
            }
            // If we arrive here, the download has completed without error(?). We can now play the file
            ProcessBuilder playProcessBuilder = new ProcessBuilder(LiveSettings.vlc, fullPath, "-f", "--mmdevice-volume", "0.15", "--one-instance", "--playlist-enqueue");
            try {
                if (!queue.isEmpty()) {
                    queue.poll();
                }
                Process playProcess = playProcessBuilder.start();


                playProcess.onExit().get();
            } catch (Exception e) {
                log.warn("Play failed", e);
                return null;
            }

            return null;
        });
    }
}
