package nsdw.vertiv.utils

import io.reactivex.rxjava3.core.Single
import io.vavr.Function2
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import io.reactivex.rxjava3.core.Observable

import java.util.stream.Collector

class RxUtil {

    static final CONTINUE = new Object();

    static <T> Observable<T> observeAR(Consumer<Handler<AsyncResult<T>>> binder){
        // TODO consider if this can be rewritten without CompletableFuture
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
            }.exceptionally {
                sub.onError(it)
            }
        })
    }

    static <E, T> Single<T> asyncReduceIterable(Iterator<E> iter, T collector, Function2<E, T, Single<T>> mapper) {
        if(!iter.hasNext()) {
            return Single.just(collector);
        }
        return Single.just(collector)
                .flatMap ({ result ->
                    mapper.apply(iter.next(), collector).flatMap(col -> {
                        return asyncReduceIterable(iter, col, mapper)
                    })
                })
    }

    static Single<Object> after(Iterable<Single> singles) {
        if(singles.size() > 0) {
            return Single.zip(singles, __ -> CONTINUE)
        } else {
            return Single.just(CONTINUE)
        }
    }

    static Collector observeFirst() {
        return Collector.of(
                __ -> null,
                (a, t) -> t,
                (a1, a2) -> a1,
                a -> a);
    }
}
