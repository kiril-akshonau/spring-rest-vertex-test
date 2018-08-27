package booot.config;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import booot.annotation.VertxRestController;
import booot.verticle.ClassicVerticle;
import booot.verticle.MongoVerticle;
import booot.verticle.SimpleVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * @author kakshonau
 *
 */
@Configuration
public class Config {

    @Bean
    public MongoVerticle mongeVerticle() {
        return new MongoVerticle();
    }

    @Bean
    public SimpleVerticle simpleVerticle() {
        return new SimpleVerticle();
    }

    @Bean
    public ClassicVerticle classicVerticle() {
        return new ClassicVerticle();
    }

    @Bean
    @ConditionalOnMissingBean
    public Vertx vertex() {
        return Vertx.vertx();
    }

    @Bean
    public MongoClient mongoClient(Vertx vertx) {
        return MongoClient.createShared(vertx, new JsonObject()
                .put("connection_string", "mongodb://localhost:27017")
                .put("db_name", "audit"), "audit");
    }

    @Bean(name = "stub")
    public String stub(Vertx vertx, Optional<List<Verticle>> allVerticles, List<Object> controllers) {
        List<GenericUserVerticle> collect = controllers.stream()
            .filter(c -> c.getClass().isAnnotationPresent(VertxRestController.class))
            .map(c -> new GenericUserVerticle(c, vertx))
            .collect(Collectors.toList());
        collect.forEach(vertx::deployVerticle);
        allVerticles.ifPresent(v -> v.forEach(vertx::deployVerticle));
        vertx.deployVerticle(new ServerVerticle(controllers, vertx));
        return "";
    }
}
