package nl.leonvanderkaap.yvplayer.integrations.youtube;

import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.commons.LiveSettings;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YoutubeDownloadService {

    public Optional<FileInformation> download(String video) {
        if (video == null || video.isBlank()) return Optional.empty();
        int index = video.indexOf("watch?v=");
        String targetFileName;
        if (index == -1) {
            targetFileName = video;
        } else {
            targetFileName = video.substring(index+"watch?v=".length());
        }

        String folderPath = LiveSettings.getDownloadFolder();

        List<String> downloadArgumentList = new ArrayList<>();
        downloadArgumentList.add(LiveSettings.ytdlp);
        downloadArgumentList.add(wrap(video));
        downloadArgumentList.addAll(List.of("--write-info-json", "--write-subs", "-q"));
        downloadArgumentList.addAll(List.of("-P", wrap(folderPath), "-o", wrap(targetFileName)));
        downloadArgumentList.addAll(List.of("-S", String.format(wrap("height:%s"), LiveSettings.maxResolution)));
        String[] downloadArguments = downloadArgumentList.toArray(new String[]{});

        ProcessBuilder downloadProcessBuilder = new ProcessBuilder(downloadArguments);
        try {
            Process downloadProcess = downloadProcessBuilder.start();
            // Don't remove the lines reading the output
            // For some reason, some video downloads(especially bigger ones) break when the output is not read
            BufferedReader in = new BufferedReader(new InputStreamReader(downloadProcess.getInputStream()));
            BufferedReader errorStream = new BufferedReader(new InputStreamReader(downloadProcess.getInputStream()));
            while ((in.readLine()) != null);
            String errorString = collectStream(errorStream);
            if (!errorString.isEmpty()) {
                log.warn(errorString);
            }
            CompletableFuture<Process> future = downloadProcess.onExit();
            if (future != null) {
                Process downloadResult = future.get();
                if (downloadResult.exitValue() != 0) {
                    log.error(String.format("Failed to download video. Program arguments : %s", String.join(" ", downloadArgumentList)));
                    throw new RuntimeException();
                }
            }
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            return Optional.empty();
        }

        // Yt-dlp stopped accepting fixed names, which forces a rework or in this case a rename to the desired name
        String realFileName = findFilename(folderPath, targetFileName);
        if (!realFileName.equals(targetFileName)) {
            File target = new File(folderPath+File.separator+targetFileName);
            File source = new File(folderPath+File.separator+realFileName);
            source.renameTo(target);
        }

        return Optional.of(new FileInformation(folderPath+File.separator+targetFileName, targetFileName));
    }

    private static String wrap(String str) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "\""+str+"\"";
        } else {
            return str;
        }
    }

    private static String collectStream(BufferedReader stream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String errorLine;
        while ((errorLine = stream.readLine()) != null) {
            stringBuilder.append(errorLine);
        }
        return stringBuilder.toString();
    }

    private static String findFilename(String path, String desired) {
        File folder = new File(path);
        for (String fileName: folder.list()) {
            if (fileName.contains(desired) && !fileName.contains(".info")) {
                return fileName;
            }
        }
        return desired;
    }
}
