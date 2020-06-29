package nsdw.vertiv.utils

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import nsdw.vertiv.utils.threading.VertxScheduler
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory

import static nsdw.vertiv.utils.RxUtil.*;

@ExtendWith(VertxExtension)
class RxUtilTest {
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
        def result = asyncReduceIterable(iter, 0, { element, sum ->
            log.debug "On thread ${Thread.currentThread().name} async iteration $element $sum"
            ctx.verify { assert element == iterable[i] }
            i++;
            return Single.just(sum+element)
        })
        result.doOnSuccess((Consumer<Integer>){ calculatedSum ->
            log.debug "Reduced to $calculatedSum"
            ctx.verify { assert calculatedSum == 46 }
            ctx.completeNow()
        }).subscribeOn(new VertxScheduler(vertx)).subscribe()
    }

}
