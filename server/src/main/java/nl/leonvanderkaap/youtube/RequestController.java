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
    @GetMapping(path = "/play")
    public void downloadAndPlay(@RequestParam(required = true) String video) {
        requestService.queueVideo(video);
    }
}
