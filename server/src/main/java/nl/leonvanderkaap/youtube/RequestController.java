package nl.leonvanderkaap.youtube;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
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
}
