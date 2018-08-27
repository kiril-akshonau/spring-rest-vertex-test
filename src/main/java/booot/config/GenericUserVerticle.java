package booot.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
 * @author kakshonau
 *
 */
class GenericUserVerticle extends AbstractVerticle {

    private final Object verticleBean;
    private final Vertx vertx;
    private final ExceptionNodeAccessor accessor = new ExceptionNodeAccessor();
    private final Map<Method, MethodHandle> methods = new ConcurrentHashMap<>();
    private final Map<Class<? extends Throwable>, MethodHandleWrapper> exceptionHandlers = new ConcurrentHashMap<>();

    GenericUserVerticle(Object verticleBean, Vertx vertx) {
        this.verticleBean = verticleBean;
        this.vertx = vertx;
    }

    @Override
    public void start() throws Exception {
        Arrays.stream(verticleBean.getClass().getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(ExceptionHandler.class))
            .forEach(this::buildHandlers);
        Arrays.stream(verticleBean.getClass().getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(RequestMapping.class))
            .forEach(m -> vertx.eventBus()
                .consumer(m.toString())
                .handler(m.getParameterTypes().length != 0 && m.getParameterTypes()[0] == Future.class
                    ? getBlockingHandler(m)
                    : getHandler(m)));
    }

    private void buildHandlers(Method m) {
        ExceptionHandler exceptionHandler = m.getAnnotation(ExceptionHandler.class);
        int code = Optional.ofNullable(m.getAnnotation(ResponseStatus.class))
            .map(ResponseStatus::code)
            .map(HttpStatus::value)
            .orElse(HttpStatus.INTERNAL_SERVER_ERROR.value());
        Class<? extends Throwable>[] exceptions = exceptionHandler.value().length != 0 ? exceptionHandler.value()
            : new Class[] { Throwable.class };
        for (Class<? extends Throwable> exception : exceptions) {
            accessor.add(exception);
            MethodHandleWrapper wrapper = new MethodHandleWrapper(computeMethodHandler(m), code, m.getParameterTypes());
            if (null != exceptionHandlers.put(exception, wrapper)) {
                vertx.close(r -> {
                    throw new RuntimeException("Only one handler per exception is allowed");
                });
            }
        }
    }

    private Handler<Message<Object>> getBlockingHandler(Method m) {
        return msg -> vertx.executeBlocking(future -> {
            try {
                methods.computeIfAbsent(m, this::computeMethodHandler).invoke(verticleBean, future);
//                m.invoke(verticleBean, future);
            } catch (Throwable e) {
                future.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                msg.reply(result.result());
            } else {
                handleException(msg, result.cause());
            }
        });
    }

    private Handler<Message<Object>> getHandler(Method m) {
        return msg -> {
            try {
                msg.reply(methods.computeIfAbsent(m, this::computeMethodHandler).invoke(verticleBean));
//                msg.reply(m.invoke(verticleBean));
            } catch (Throwable e) {
                handleException(msg, e);
            }
        };
    }

    private String convertToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void handleException(Message<Object> msg, Throwable t) {
        Class<? extends Throwable> nearest = accessor.findNearest(t);
        Object workload = null;
        int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (null != nearest) {
            MethodHandleWrapper methodHandleWrapper = exceptionHandlers.get(nearest);
            MethodHandle methodHandle = methodHandleWrapper.handle;
            code = methodHandleWrapper.code;
            try {
                workload = methodHandle.invokeWithArguments(resolveHandlerArgs(methodHandleWrapper.args, t));
            } catch (Throwable e1) {
                t.printStackTrace();
                msg.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), convertToString(e1));
            }
        }
        msg.fail(code, null != workload ? workload.toString() : convertToString(t));
    }

    private MethodHandle computeMethodHandler(Method m) {
        try {
            Lookup in = MethodHandles.lookup().in(verticleBean.getClass());
            MethodHandle mh = null;
            if (!m.isAccessible()) {
                m.setAccessible(true);
                mh = in.unreflect(m);
                m.setAccessible(false);
            } else {
                mh = in.unreflect(m);
            }
            return mh;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] resolveHandlerArgs(Class<?>[] args, Throwable t) {
        return args.length == 0 ? new Object[] { verticleBean } : new Object[] { verticleBean, t };
    }

    private static final class MethodHandleWrapper {
        private final MethodHandle handle;
        private final int code;
        private final String message = null;
        private final Class<?>[] args;

        private MethodHandleWrapper(MethodHandle handle, int code, Class<?>[] args) {
            this.handle = handle;
            this.code = code;
            this.args = args.clone();
        }
    }
}
