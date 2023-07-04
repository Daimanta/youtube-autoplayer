package nl.leonvanderkaap.yvplayer.vlc;

import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.integrations.youtube.SponsorBlockVideoSegmentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

@Service
@Slf4j
public class VlcPlaylistBuilder {

    public String buildRegularPlaylistFile(String fullPath, String title, String fileIdName, int duration) {
        try {
            return buildPlaylistFile(fullPath, title, fileIdName, duration);
        } catch (IOException e) {
            log.warn("Could not construct playback file");
            return null;
        }
    }

    public String buildSponsorCheckedPlayListFile(String video, String fullPath, String title, String fileIdName, int duration) {
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

    private String buildPlaylistFile(ResponseEntity<SponsorBlockVideoSegmentResponse[]> responseEntity, String fullPath, String title, String id, int duration) throws IOException {
        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            return buildSponsorBlockPlaylist(responseEntity.getBody(), fullPath, title, id, duration);
        } else {
            return buildPlaylistFile(fullPath, title, id, duration);
        }
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

}
