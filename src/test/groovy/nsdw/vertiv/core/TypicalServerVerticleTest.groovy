package nsdw.vertiv.core

import io.vertx.core.DeploymentOptions
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.ext.web.client.WebClient
import nsdw.vertiv.utils.sockets.SocketUtil
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory

@ExtendWith(VertxExtension)
class TypicalServerVerticleTest {
    static final log = LoggerFactory.getLogger(this.class)

    static WebClient webClient

    @BeforeAll
    static void setup() {
        webClient = WebClient.create(Vertx.vertx())
    }

    @Test
    void testVerticleStartupSuccessful(VertxTestContext ctx) {
        log.info "works?"

        def port = SocketUtil.tryAllocatePort(true)
        def deployOpts = new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put("http", new JsonObject()
                            .put("port", port)
                    )
            )
        Vertx.vertx().deployVerticle(TypicalServerVerticle.class.name, deployOpts, __ -> {
                webClient.get(port, "localhost", "/about").send(ar -> {
                    ctx.verify {
                        assert ar.result().bodyAsString()  == "TypicalServerVerticle - fallback about page!"
                    }
                    ctx.completeNow()
                })
            })
    }

}
