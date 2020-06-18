package nsdw.vertiv.utils

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import io.reactivex.rxjava3.core.Observable

class RxUtil {

    static <T> Observable<T> observeAR(Consumer<Handler<AsyncResult<T>>> binder){
        def fut = new CompletableFuture<T>()
        Handler<AsyncResult<T>> capture = res -> {
            if(res.succeeded()) {
                fut.complete(res.result())
            } else {
                fut.completeExceptionally(res.cause())
            }
        }
        binder.accept(capture);
        return Observable.create({sub ->
            fut.thenAccept {
                sub.onNext(it)
            }
        })
    }

}
