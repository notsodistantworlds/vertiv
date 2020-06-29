package nsdw.vertiv.core.verticles.typicalserver

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import nsdw.vertiv.core.verticles.typicalserver.startup.ScheduledStartupStage
import nsdw.vertiv.core.verticles.typicalserver.startup.StartupStageCue
import nsdw.vertiv.utils.RxUtil
import nsdw.vertiv.utils.sockets.SocketUtil
import org.slf4j.LoggerFactory

import static nsdw.vertiv.utils.ConciseCodeUtil.onNotNull
import static nsdw.vertiv.utils.RxUtil.*
import static nsdw.vertiv.utils.RxUtil.CONTINUE
import static nsdw.vertiv.utils.RxUtil.CONTINUE

class TypicalServerVerticle extends AbstractVerticle {
    def log = LoggerFactory.getLogger(this.class)

    Router router;
    List<ScheduledStartupStage> scheduledStartupStages;

    protected List<ScheduledStartupStage> provideStartupScheduleSelf() {
        return new LinkedList<ScheduledStartupStage>();
    }

    @Override
    void start(Promise<Void> startPromised){
        try{
            Single.just(CONTINUE)
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
                    Single.just(CONTINUE)
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
                    observeAR ({ handler ->
                        server.listen(handler)
                    }).doOnNext(startedServer -> {
                        log.info "Server startup complete. Listening on port $httpOptions.port"
                    }).doOnError(error -> {
                        log.error("Server failed to start!", error)
                    }).collect(observeFirst())
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

    Single<Object> execStartupStages(StartupStageCue cue) {
        def parralelizable = scheduledStartupStages
                .findAll { it.cue == cue }
                .findAll { it.allowRunInParallel }
                .collect { it.stage.execStage() }
        Queue nonParallelizable = new LinkedList<>(scheduledStartupStages
                .findAll { it.cue == cue }
                .findAll { !it.allowRunInParallel })
        after(parralelizable)
                .flatMap(__ -> {
                    asyncReduceIterable(nonParallelizable.iterator(), CONTINUE,
                            (ScheduledStartupStage scheduled, ___) -> {
                                scheduled.stage.execStage()
                            })
                })//.subscribe(__ -> 0, err -> log.error("\n\n\n", err))
    }
}
