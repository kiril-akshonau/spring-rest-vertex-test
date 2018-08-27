package booot.verticle;

import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import booot.annotation.VertxRestController;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * @author kakshonau
 *
 */
@VertxRestController
public class MongoVerticle {

    @Resource
    private MongoClient client;

    @RequestMapping(produces = "application/json")
    @ResponseStatus(code = HttpStatus.ACCEPTED, reason = "my reason")
    public void findActivities(Future<String> future) {
        client.find("Activity", new JsonObject(), res -> future.complete(res.result()
                .stream()
                .map(JsonObject::encodePrettily)
                .collect(Collectors.joining(",", "[", "]"))));
    }

    @RequestMapping(path = "/test", method = { RequestMethod.POST, RequestMethod.GET })
    private String doTest() {
        return "test";
    }

    @RequestMapping(path = "/exception")
    void exception() {
        throw new ArithmeticException();
    }

    @RequestMapping(path = "/exception_workload")
    void exceptionWorkload() throws SQLException {
        throw new SQLException();
    }

    @RequestMapping(path = "/exception_unknown")
    public void exceptionUnknown() throws Throwable {
        throw new Throwable();
    }

    @ResponseStatus(code = HttpStatus.NOT_IMPLEMENTED, reason = "test exception")
    @ExceptionHandler(RuntimeException.class)
    public void arithmeticException(RuntimeException e) {
        System.err.println(Thread.currentThread().getName());
        e.printStackTrace();
    }

    @ExceptionHandler(SQLException.class)
    private String sqlException() {
        System.err.println(Thread.currentThread().getName());
        return Thread.currentThread().getName();
    }
}
