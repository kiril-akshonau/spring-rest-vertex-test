package booot.app;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import booot.config.Config;

/**
 * @author kakshonau
 *
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder()
                .sources(App.class, Config.class)
                .web(WebApplicationType.NONE)
                .build()
                .run(args);
    }
}
