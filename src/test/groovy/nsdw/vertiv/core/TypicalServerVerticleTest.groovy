package nsdw.vertiv.core

import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.Vertx
import io.vertx.ext.web.client.HttpResponse
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.ext.web.client.WebClient
import nsdw.vertiv.core.verticles.typicalserver.TypicalServerVerticle
import nsdw.vertiv.core.verticles.typicalserver.startup.ScheduledStartupStage
import nsdw.vertiv.core.verticles.typicalserver.startup.StartupStageCue
import nsdw.vertiv.utils.sockets.SocketUtil
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

import java.util.stream.Collectors

import static nsdw.vertiv.utils.ReactorUtil.*
import static nsdw.vertiv.utils.ReactorUtil.CONTINUE
import static nsdw.vertiv.utils.VertxAsyncUtil.*

@ExtendWith(VertxExtension)
class TypicalServerVerticleTest {
    static final log = LoggerFactory.getLogger(this.class)

    static WebClient webClient

    @BeforeAll
    static void setup() {
        webClient = WebClient.create(Vertx.vertx())
    }

    @Test
    /**
     * When a TypicalServerVerticle is deployed
     * expect the /about endpoint to function, no matter what.
     */
    void testVerticleStartupSuccessful(VertxTestContext ctx) {
        deployTypicalServerVerticle(new TypicalServerVerticle())
            .flatMap({ startupDetails ->
                callDefaultAbout(startupDetails.port)
            })
            .doOnSuccess(response -> {
                ctx.verify {
                    assert response.bodyAsString()  == "TypicalServerVerticle - fallback about page!"
                }
                ctx.completeNow()
            }).doOnError(ctx::failNow).subscribe()
    }

    @Test
    /**
     * When a TypicalServerVerticle executes its startup routine
     * expect all StartupStageCues to be invoked at some point.
     * TODO assert BEFORE_ALL is invoked first
     * TODO assert AFTER_ALL is invoked last
     */
    void testVerticleStartupStages_allCuesGetCalled(VertxTestContext ctx) {
        def expectedCues = StartupStageCue.values()
        def unprocessedCues = Arrays.stream(StartupStageCue.values()).collect(Collectors.toList())
        def verticle = new TypicalServerVerticle() {
            @Override
            protected List<ScheduledStartupStage> provideStartupScheduleSelf() {
                def rv = new LinkedList<ScheduledStartupStage>();
                expectedCues.each { cue ->
                    def scheduled = new ScheduledStartupStage();
                    scheduled.stage = {
                        log.info("Startup cue $cue processed!")
                        unprocessedCues.remove(cue)
                        Mono.just(CONTINUE)
                    }
                    scheduled.allowRunInParallel = false
                    scheduled.cue = cue
                    rv.add(scheduled)
                }
                return rv
            }
        }
        deployTypicalServerVerticle(verticle).doOnSuccess ({
            ctx.verify {
                assert unprocessedCues == []
            }
            ctx.completeNow()
        }).subscribe()
    }

    @Test
    /**
     * When a TypicalServerVerticle executes its startup routine
     * and a user-defined stage reports an error
     * expect the verticle to halt its startup
     */
    void testVerticleStartupStages_userDefinedStageFails_verticleStarupFails(VertxTestContext ctx) {
        def verticle = new TypicalServerVerticle() {
            @Override
            protected List<ScheduledStartupStage> provideStartupScheduleSelf() {
                def rv = new LinkedList<ScheduledStartupStage>();
                def scheduled = new ScheduledStartupStage();
                scheduled.stage = {
                    log.info("Custom user stage presumably failed!")
                    Mono.error(new IllegalStateException("You're not allowed to start, boy!"))
                }
                scheduled.allowRunInParallel = false
                scheduled.cue = StartupStageCue.AFTER_ALL
                rv.add(scheduled)
                return rv
            }
        }
        deployTypicalServerVerticle(verticle)
                .flatMap({ startupDetails ->
                    return callDefaultAbout(startupDetails.port)
                })
                .subscribe(
                        {res ->  ctx.failNow(new Exception("Expected server startup failure"))},
                        { err -> ctx.completeNow() }
                )
    }

    private static Mono<HttpResponse> callDefaultAbout(int port) {
        return later({ fut ->
            webClient.get(port, "localhost", "/about")
                    .timeout(500)
                    .send(ar -> applyAsyncResultToFuture(ar, fut))
        })
    }

    private static Mono<Object> deployTypicalServerVerticle(TypicalServerVerticle verticle) {
        return later({fut ->
            def startupDetails = [:]
            def port = SocketUtil.tryAllocatePort(true)
            def deployOpts = new DeploymentOptions()
                    .setConfig(
                            new JsonObject()
                                    .put("http", new JsonObject()
                                            .put("port", port)
                                    )
                    )
            startupDetails.port = port
            Vertx.vertx().deployVerticle(verticle, deployOpts, { ar ->
                handleAR(ar,
                    {__ ->
                        startupDetails.success = true
                        fut.complete(startupDetails)
                    },
                    { __ ->
                        startupDetails.success = false
                        fut.complete(startupDetails)
                    })
            })
        })
    }

}
