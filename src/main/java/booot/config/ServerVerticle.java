package booot.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @author kakshonau
 *
 */
class ServerVerticle extends AbstractVerticle {

    private static final Map<RequestMethod, HttpMethod> METHOD_MAPPING = new HashMap<RequestMethod, HttpMethod>() {
        {
            put(RequestMethod.GET, HttpMethod.GET);
            put(RequestMethod.POST, HttpMethod.POST);
            put(RequestMethod.PUT, HttpMethod.PUT);
        }
    };

    private final List<?> restVerticles;
    private final Vertx vertx;

    ServerVerticle(List<?> restVerticles, Vertx vertx) {
        this.restVerticles = restVerticles;
        this.vertx = vertx;
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        restVerticles.stream()
            .flatMap(c -> Arrays.stream(c.getClass().getDeclaredMethods()))
            .filter(m -> m.isAnnotationPresent(RequestMapping.class))
            .forEach(m -> handleMapping(router, m));
        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080, handler -> System.out
                .println(handler.succeeded() ? "http://localhost:8080/" : "Failed to listen on port 8080"));
    }

    private Handler<RoutingContext> getAllArticlesHandler(Method m) {
        // TODO: AnnotationUtils
        RequestMapping mapping = m.getAnnotation(RequestMapping.class);
        ResponseStatus responseStatus = m.getAnnotation(ResponseStatus.class);
        String[] produces = mapping.produces();
        return routingContext -> vertx.eventBus().send(m.toString(), "", result -> {
            if (result.succeeded()) {
                routingContext.response()
                    .setStatusCode(Optional.ofNullable(responseStatus)
                            .map(ResponseStatus::code)
                            .map(HttpStatus::value)
                            .orElse(HttpStatus.OK.value()))
                    .setStatusMessage(Optional.ofNullable(responseStatus)
                            .map(ResponseStatus::reason)
                            .orElse(HttpStatus.OK.getReasonPhrase()))
                    .putHeader("content-type", (Iterable<String>) Arrays.asList(produces))
                    .end(Optional.ofNullable(result.result())
                            .map(Message::body)
                            .map(Object::toString)
                            .map(Buffer::buffer)
                            .orElseGet(Buffer::buffer));
            } else {
                routingContext.response()
                    .setStatusCode(((ReplyException) result.cause()).failureCode())
                    .end(Optional.ofNullable(result.cause())
                            .map(Throwable::getMessage)
                            .map(Buffer::buffer)
                            .orElseGet(Buffer::buffer));
            }
        });
    }

    private void handleMapping(Router router, Method m) {
        RequestMapping mapping = m.getAnnotation(RequestMapping.class);
        RequestMethod[] methods = Optional.ofNullable(mapping.method())
                .filter(mm -> mm.length != 0)
                .orElseGet(() -> METHOD_MAPPING.keySet().toArray(new RequestMethod[0]));
        String[] paths = Optional.ofNullable(mapping.path())
                .filter(pp -> pp.length != 0)
                .orElseGet(() -> new String[] { "/" });
        for (RequestMethod requestMethod : methods) {
            for (String path : paths) {
                router.route(METHOD_MAPPING.get(requestMethod), path).handler(getAllArticlesHandler(m));
            }
        }
    }
}
