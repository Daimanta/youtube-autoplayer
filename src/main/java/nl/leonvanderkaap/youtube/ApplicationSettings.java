package nl.leonvanderkaap.youtube;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application")
@Getter
@Setter
public class ApplicationSettings {

    private String ytdlp;
    private String tempfolder;
    private String vlc;
    private boolean blockSponsors = true;
    private String maxResolution = "1080p";
}
