package nl.leonvanderkaap.yvplayer.commons;

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
    private int vlcport;
    private String vlcpassword;
    private boolean blocksponsors = true;
    private String maxresolution = "1080p";
}
