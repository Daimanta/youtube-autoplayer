package nl.leonvanderkaap.yvplayer.integrations.smb;

import jakarta.annotation.PostConstruct;
import jcifs.smb.SmbFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.FileQueueService;
import nl.leonvanderkaap.yvplayer.LiveSettings;
import nl.leonvanderkaap.yvplayer.vlc.VlcCommunicatorService;
import nl.leonvanderkaap.yvplayer.vlc.VlcPlaylistBuilder;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmbProcessingService implements FileQueueService {

    private final VlcPlaylistBuilder vlcPlaylistBuilder;
    private final VlcCommunicatorService vlcCommunicatorService;

    @PostConstruct
    public void setup() {
        jcifs.Config.registerSmbURLHandler();
    }
    @Override
    public void downloadAndQueueVideo(String video) {
        SmbFile smbFile;
        try {
            smbFile = new SmbFile(video);
        } catch (MalformedURLException e) {
            log.warn("Incorrect smb url format");
            return;
        }
        try {
            if (!smbFile.isFile()) return;
            String title = smbFile.getName();
            InputStream smbFileInputStream = smbFile.getInputStream();
            String folderPath = LiveSettings.getDownloadFolder();
            if (!folderPath.endsWith(File.separator)) folderPath += File.separator;
            String targetFileName = UUID.randomUUID().toString();
            File localFile = new File(folderPath + targetFileName);
            OutputStream outStream = new FileOutputStream(localFile);
            byte[] buffer = new byte[8 * 1024 * 1024];
            int bytesRead;
            while ((bytesRead = smbFileInputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            IOUtils.closeQuietly(smbFileInputStream);
            IOUtils.closeQuietly(outStream);
            String playlistLocation = vlcPlaylistBuilder.buildRegularPlaylistFile(folderPath + targetFileName, title, targetFileName, -1);
            vlcCommunicatorService.addItemToPlayList(playlistLocation);
        } catch (Exception e) {
            log.warn(e.getMessage());
        } finally {
            smbFile.close();
        }
    }
}
