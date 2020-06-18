package nsdw.vertiv.core

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import nsdw.vertiv.utils.RxUtil
import nsdw.vertiv.utils.sockets.SocketUtil
import org.slf4j.LoggerFactory

import static nsdw.vertiv.utils.ConciseCodeUtil.onNotNull

class TypicalServerVerticle extends AbstractVerticle {
    def log = LoggerFactory.getLogger(this.class)
    @Override
    void start(Promise<Void> startPromised){

        log.info "Verticle startup begin..."

        def httpOptions = new HttpServerOptions();
        onNotNull(config().getJsonObject("http").getInteger("port")) { port ->
            httpOptions.port = port
        }

        // TODO stages

        log.info "Router setup..."
        Router router = Router.router(vertx)
        router.get("/about").handler(rtx -> {
            rtx.response().setStatusCode(200).end("${this.class.simpleName} - fallback about page!");
        })

        if(httpOptions.port == 0) {
            httpOptions.port = SocketUtil.tryAllocatePort(true)
        }

        log.info "Server startup..."
        HttpServer server = vertx.createHttpServer(httpOptions)
        server.requestHandler(router)
        RxUtil.observeAR ({ it ->
            server.listen(it)
        }).subscribe(startedServer -> {
            log.info "Server startup complete. Listening on port $httpOptions.port"
            startPromised.complete()
        },error -> {
            log.error("Server failed to start!", error)
            startPromised.fail(error)
        })
    }
}
