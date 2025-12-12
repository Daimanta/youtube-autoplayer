package nl.leonvanderkaap.yvplayer.integrations.youtube;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.commons.LiveSettings;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YoutubeDownloadService {

    @Getter
    private Map<String, String> downloadingMap = new HashMap<>();

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

        List<String> prefetchArgumentsList = new ArrayList<>();
        prefetchArgumentsList.add(LiveSettings.ytdlp);
        prefetchArgumentsList.add(wrap(video));
        prefetchArgumentsList.add("--skip-download");
        prefetchArgumentsList.add("--print");
        prefetchArgumentsList.add("%(title)s -- %(id)s");
        String[] prefetchArguments = prefetchArgumentsList.toArray(new String[0]);

        ProcessBuilder prefectProcessBuilder = new ProcessBuilder(prefetchArguments);
        try {
            Process downloadProcess = prefectProcessBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(downloadProcess.getInputStream()));
            String downloadInfo = collectStream(in);
            downloadingMap.put(targetFileName, downloadInfo);
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            return Optional.empty();
        }

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
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            return Optional.empty();
        } finally {
            downloadingMap.remove(targetFileName);
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
        String[] fileList = folder.list();
        for (String fileName: fileList) {
            if (fileName.contains(desired) && (fileName.endsWith(".mp4") || fileName.endsWith(".webm"))) {
                return fileName;
            }
        }
        //Fallback if mp4 doesn't work
        for (String fileName: fileList) {
            if (fileName.contains(desired) && !fileName.contains(".info")) {
                return fileName;
            }
        }

        return desired;
    }
}
