package nl.leonvanderkaap.youtube;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api")
public class RequestController {

    private RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @CrossOrigin
    @GetMapping(path = "/queue")
    public void downloadAndQueue(@RequestParam(required = true) String video) {
        requestService.queueVideo(video);
    }

    @CrossOrigin
    @GetMapping("/play")
    public void togglePlay() {
        requestService.togglePlay();
    }

    @CrossOrigin
    @GetMapping("/stop")
    public void stop() {
        requestService.stop();
    }

    @CrossOrigin
    @GetMapping("/fullscreen")
    public void toggleFullscreen() {
        requestService.fullScreen();
    }

    @CrossOrigin
    @GetMapping("/next")
    public void next() {
        requestService.next();
    }

    @CrossOrigin
    @GetMapping("/previous")
    public void previous() {
        requestService.previous();
    }

    @CrossOrigin
    @GetMapping("/volumeup")
    public void volumeUp() {
        requestService.volumeUp();
    }

    @CrossOrigin
    @GetMapping("/volumedown")
    public void volumeDown() {
        requestService.volumeDown();
    }

    @CrossOrigin
    @GetMapping("/emptyplaylist")
    public void emptyPlaylist() {
        requestService.emptyPlaylist();
    }

    @CrossOrigin
    @GetMapping(value = "/getplaylist", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PlaylistItem> getPlaylist() {
        return requestService.getPlaylist();
    }
}
