package nl.leonvanderkaap.yvplayer.vlc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import nl.leonvanderkaap.yvplayer.LiveSettings;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VlcCommunicatorService {

    private final RestTemplate restTemplate;


    public ResponseEntity<String> togglePlay() {
        return doRequest("localhost", "command=pl_pause");
    }

    public ResponseEntity<String> play() {
        return doRequest("localhost", "command=pl_pause");
    }

    public ResponseEntity<String> pause() {
        return doRequest("localhost", "command=pl_pause");
    }

    public ResponseEntity<String> stop() {
        return doRequest("localhost", "command=pl_stop");
    }

    public ResponseEntity<String> next() {
        return doRequest("localhost", "command=pl_next");
    }

    public ResponseEntity<String> previous() {
        return doRequest("localhost", "command=pl_previous");
    }

    public ResponseEntity<String> setVolume(int volume) {
        return doRequest("localhost", "command=volume&val="+volume);
    }

    public ResponseEntity<String> fullScreen() {
        return doRequest("localhost", "command=fullscreen");
    }

    public ResponseEntity<String> emptyPlaylist() {
        return doRequest("localhost", "command=pl_empty");
    }

    public ResponseEntity<String> selectItem(String item) {
        return doRequest("localhost", "command=pl_play&id="+item);
    }

    private ResponseEntity<String> doRequest(String host, String command) {
        URI enqueueURL;
        try {
            enqueueURL = new URI("http", null, host, LiveSettings.vlcPort, "/requests/status.xml", command, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return get(restTemplate, enqueueURL, Map.of("Authorization", "Basic " + LiveSettings.vlcPasswordBasicAuth()), String.class);
    }

    private <T> ResponseEntity<T> get(RestTemplate restTemplate, URI uri, Map<String, String> headers, Class<T> clazz) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (String header: headers.keySet()) {
            httpHeaders.set(header, headers.get(header));
        }
        httpHeaders.setContentType(MediaType.TEXT_XML);
        HttpEntity<Void> requestEntity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, clazz);
    }

    public VlcStatusInfo getStatus() {
        ResponseEntity<String> responseString = doRequest("localhost", null);
        XmlMapper xmlMapper = new XmlMapper();
        try {
            return xmlMapper.readValue(responseString.getBody(), VlcStatusInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> getPlaylist(String host) {
        URI enqueueURL;
        try {
            enqueueURL = new URI("http", null, host, LiveSettings.vlcPort, "/requests/playlist_jstree.xml", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return get(restTemplate, enqueueURL, Map.of("Authorization", "Basic " + LiveSettings.vlcPasswordBasicAuth()), String.class);
    }
}
