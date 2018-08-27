package booot.verticle;

import org.springframework.web.bind.annotation.RequestMapping;

import booot.annotation.VertxRestController;

/**
 * @author kakshonau
 *
 */
@VertxRestController
public class SimpleVerticle {

    @RequestMapping(path = "/foo")
    public String foo() {
        return "foo";
    }
}
