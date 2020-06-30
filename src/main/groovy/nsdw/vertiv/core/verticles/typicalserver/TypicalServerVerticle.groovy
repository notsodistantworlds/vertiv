package nsdw.vertiv.core.verticles.typicalserver

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import nsdw.vertiv.core.verticles.typicalserver.startup.IStartupStage
import nsdw.vertiv.core.verticles.typicalserver.startup.ScheduledStartupStage
import nsdw.vertiv.core.verticles.typicalserver.startup.StartupStageCue
import nsdw.vertiv.utils.sockets.SocketUtil
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

import java.util.function.Consumer

import static nsdw.vertiv.utils.ConciseCodeUtil.onNotNull
import static nsdw.vertiv.utils.ReactorUtil.*

class TypicalServerVerticle extends AbstractVerticle {
    protected static final def log = LoggerFactory.getLogger(this.class)

    Router router;
    List<ScheduledStartupStage> scheduledStartupStages;

    protected List<ScheduledStartupStage> provideStartupScheduleSelf() {
        return new LinkedList<ScheduledStartupStage>();
    }

    @Override
    void start(Promise<Void> startPromised){
        try{
            // TODO pass in through a constructor(?)
            if(scheduledStartupStages == null) {
                scheduledStartupStages = provideStartupScheduleSelf();
            }
            Mono.just(CONTINUE)
                .flatMap({ __ ->
                    log.info "Verticle startup begin..."
                    return execStartupStages(StartupStageCue.BEFORE_ALL)
                })
                .flatMap(__ -> execStartupStages(StartupStageCue.BEFORE_LOAD_CONFIG))
                // TODO - fetch config
                .flatMap(__ -> execStartupStages(StartupStageCue.AFTER_LOAD_CONFIG))

                .flatMap(__ -> execStartupStages(StartupStageCue.BEFORE_ROUTER_SETUP))
                .flatMap({ __ ->
                    log.info "Router setup..."
                    router = Router.router(vertx)
                    router.get("/about").handler(rtx -> {
                        rtx.response().setStatusCode(200).end("${this.class.simpleName} - fallback about page!");
                    })
                    Mono.just(CONTINUE)
                })
                .flatMap(__ -> execStartupStages(StartupStageCue.AFTER_ROUTER_SETUP))

                .flatMap(__ -> execStartupStages(StartupStageCue.BEFORE_SERVER_STARTUP))
                .flatMap({ __ ->
                    def httpOptions = new HttpServerOptions();
                    onNotNull(config().getJsonObject("http").getInteger("port")) { port ->
                        httpOptions.port = port
                    }

                    if(httpOptions.port == 0) {
                        httpOptions.port = SocketUtil.tryAllocatePort(true)
                    }

                    log.info "Server startup..."
                    HttpServer server = vertx.createHttpServer(httpOptions)
                    server.requestHandler(router)
                    monoFromVertxAR ({ handler ->
                        server.listen(handler)
                    }).doOnNext(startedServer -> {
                        log.info "Server startup complete. Listening on port $httpOptions.port"
                    }).doOnError(error -> {
                        log.error("Server failed to start!", error)
                    })
                })
                .flatMap(__ -> execStartupStages(StartupStageCue.AFTER_SERVER_STARTUP))

                .flatMap(__ -> execStartupStages(StartupStageCue.AFTER_ALL))
                .subscribe(result -> {
                    log.info("Verticle startup successful!")
                    startPromised.complete();
                }, error -> {
                    log.error("Verticle startup failed!", error)
                    startPromised.fail(error)
                })
        } catch(Throwable t) {
            log.error("Server startup failed due to unexpected exception in startup scheduler", t)
            startPromised.fail(t)
        }
    }

    Mono<Object> execStartupStages(StartupStageCue cue) {
        def parralelizable = scheduledStartupStages
                .findAll { it.cue == cue }
                .findAll { it.allowRunInParallel }
                .collect {
                    it.stage.execStage()
                        .doOnError(getStageCueErrorReporter(it.stage, cue))
                }
        Queue nonParallelizable = new LinkedList<>(
                scheduledStartupStages
                .findAll { it.cue == cue }
                .findAll { !it.allowRunInParallel })
        after(parralelizable)
                .flatMap(__ -> {
                    asyncReduceIterable(nonParallelizable.iterator(), CONTINUE,
                            (ScheduledStartupStage scheduled, ___) -> {
                                scheduled.stage.execStage()
                                        .doOnError(getStageCueErrorReporter(scheduled.stage, cue))
                            })
                })
    }

    private static Consumer<Throwable> getStageCueErrorReporter(IStartupStage stage, StartupStageCue cue) {
        return { throwable ->
            log.error "IStartupStage $stage (concrete type ${stage.getClass()}) \n" +
                    "failed on cue $cue\n " +
                    "and reported it as Throwable ${throwable.getClass()}", throwable
        }
    }

}
