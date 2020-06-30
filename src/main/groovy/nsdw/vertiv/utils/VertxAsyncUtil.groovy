package nsdw.vertiv.utils;

import io.vertx.core.AsyncResult
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer;

class VertxAsyncUtil {
    static <T> void applyAsyncResultToFuture(AsyncResult<T> ar, CompletableFuture<T> existingFuture) {
        if(ar.failed()) {
            existingFuture.completeExceptionally(ar.cause())
        } else {
            existingFuture.complete(ar.result())
        }
    }

    /**
     * This is kind of silly, but this method applies an AR to a future only if the former
     * is a failure, otherwise it returns a Mono with AR's result for further processing
     * @param ar The source AR
     * @param existingFuture The future to fail if the AR failed
     * @return A Mono<T> object that will contain the AR's result if there is one
     */
    static <T> Mono<T> applyAsyncResultToFutureIfFailed(AsyncResult<T> ar, CompletableFuture<T> existingFuture) {
        if(ar.failed()) {
            existingFuture.completeExceptionally(ar.cause())
            return Mono.empty()
        } else {
            return Mono.just(ar.result())
        }
    }

    static <T> Mono<T> handleAR(AsyncResult<T> ar, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        if(ar.succeeded()) {
            onSuccess(ar.result())
        } else {
            onError(ar.cause())
        }
    }
}
