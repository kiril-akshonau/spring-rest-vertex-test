package booot.verticle;

import io.vertx.core.AbstractVerticle;

/**
 * @author kakshonau
 *
 */
public class ClassicVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.createHttpServer()
            .requestHandler(req -> req.response().end("8081 test"))
            .listen(8081, handler -> System.out
                .println(handler.succeeded() ? "http://localhost:8081/" : "Failed to listen on port 8081"));
    }
}
