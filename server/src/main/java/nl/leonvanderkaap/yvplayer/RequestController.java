package nl.leonvanderkaap.yvplayer;

import lombok.extern.slf4j.Slf4j;
import nl.leonvanderkaap.yvplayer.management.MessageLog;
import nl.leonvanderkaap.yvplayer.vlc.PlaylistInfo;
import nl.leonvanderkaap.yvplayer.vlc.VlcStatusInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.*;

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
    public void downloadAndQueue(@RequestParam(required = true) String video, @RequestParam(required = false, defaultValue = "false", name = "play") boolean playAfterQueue) {
        requestService.queueVideo(video, playAfterQueue);
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
    @GetMapping("/setvolume")
    public void setVolume(@RequestParam(required = true) int value) {
        requestService.setVolume(value);
    }

    @CrossOrigin
    @GetMapping("/settime")
    public void setTime(@RequestParam(required = true) int value) {
        requestService.setTime(value);
    }

    @CrossOrigin
    @GetMapping("/emptyplaylist")
    public void emptyPlaylist() {
        requestService.emptyPlaylist();
    }

    @CrossOrigin
    @GetMapping(value = "/getplaylist", produces = APPLICATION_JSON_VALUE)
    public PlaylistInfo getPlaylist() {
        return requestService.getPlaylist();
    }

    @CrossOrigin
    @GetMapping("/select/{item}")
    public void selectItem(@PathVariable String item) {
        requestService.selectItem(item);
    }

    @CrossOrigin
    @GetMapping(value = "/status", produces = APPLICATION_JSON_VALUE)
    public List<MessageLog> getStatus() {
        return requestService.getStatus();
    }


    @CrossOrigin
    @GetMapping(value = "/vlcstatus", produces = APPLICATION_JSON_VALUE)
    public VlcStatusInfo getVlcStatusInfo() {
        return requestService.getVlcStatusInfo();
    }
}
