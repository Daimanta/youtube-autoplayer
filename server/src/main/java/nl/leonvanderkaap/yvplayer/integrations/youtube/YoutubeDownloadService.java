package nl.leonvanderkaap.yvplayer.integrations.youtube;

import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.LiveSettings;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class YoutubeDownloadService {

    public Optional<FileInformation> download(String video) {
        if (video == null || video.isBlank()) return Optional.empty();
        int index = video.indexOf("watch?v=");
        String fileName;
        if (index == -1) {
            fileName = video;
        } else {
            fileName = video.substring(index+"watch?v=".length());
        }

        String folderPath = LiveSettings.getDownloadFolder();

        List<String> downloadArgumentList = new ArrayList<>();
        downloadArgumentList.add(LiveSettings.ytdlp);
        downloadArgumentList.add(wrap(video));
        downloadArgumentList.add("--write-info-json");
        downloadArgumentList.addAll(List.of("-P", wrap(folderPath), "-o", wrap(fileName)));
        downloadArgumentList.addAll(List.of("-f", String.format(wrap("best[height<=%s]"), LiveSettings.maxResolution)));
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
            Process downloadResult = downloadProcess.onExit().get();
            if (downloadResult.exitValue() != 0) throw new RuntimeException();
        } catch (Exception e) {
            log.warn("Download failed: ", e);
            return Optional.empty();
        }
        return Optional.of(new FileInformation(folderPath+File.separator+fileName, fileName));
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
}
