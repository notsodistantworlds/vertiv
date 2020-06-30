package nsdw.vertiv.utils

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import nsdw.vertiv.utils.threading.VertxScheduler
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

import java.util.function.Consumer

import static ReactorUtil.*;

@ExtendWith(VertxExtension)
class ReactorUtilTest {
    static final log = LoggerFactory.getLogger(this.class)

    static WebClient webClient

    @BeforeAll
    static void setup() {
        webClient = WebClient.create(Vertx.vertx())
    }

    @Test
    void testAsyncReduceIterable(Vertx vertx, VertxTestContext ctx) {
        def iterable = [3, 4, 6, 10, 11, 12]
        def iter = iterable.iterator()

        def i = 0;
        def result = asyncReduceIterableOnScheduler(new VertxScheduler(vertx),
                iter, 0, { element, sum ->
            log.debug "On thread ${Thread.currentThread().name} async iteration $element $sum"
            ctx.verify {
                assert Thread.currentThread().name.contains("vert.x-worker-thread")
                assert element == iterable[i]
            }
            i++;
            return Mono.just(sum+element)
        })
        result.doOnSuccess((Consumer<Integer>){ calculatedSum ->
            log.debug "Reduced to $calculatedSum"
            ctx.verify { assert calculatedSum == 46 }
            ctx.completeNow()
        }).subscribe()
    }

}
