package nsdw.vertiv.utils

import io.vavr.Function2
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import java.util.stream.Collector

class ReactorUtil {

    static final CONTINUE = new Object();

    // TODO make into an extension method
    static <T> Mono<T> monoFromVertxAR(Consumer<Handler<AsyncResult<T>>> binder){
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
        return Mono.fromFuture(fut);
    }

    static <E, T> Mono<T> asyncReduceIterableAfter(Mono<T> awaitOperation, Iterator<E> iter, T collector, Function2<E, T, Mono<T>> mapper) {
        return awaitOperation.flatMap(__ -> {
            return asyncReduceIterable_iteration(iter, collector, mapper)
        })
    }

    static <E, T> Mono<T> asyncReduceIterableOnScheduler(Scheduler scheduler, Iterator < E > iter, T collector, Function2<E, T, Mono<T>> mapper) {
        return Mono.just(CONTINUE).publishOn(scheduler).flatMap(__ -> {
            return asyncReduceIterable_iteration(iter, collector, mapper)
        })
    }

    static <E, T> Mono<T> asyncReduceIterable(Iterator<E> iter, T collector, Function2<E, T, Mono<T>> mapper) {
        return asyncReduceIterable_iteration(iter, collector, mapper)
    }

    private static <E, T> Mono<T> asyncReduceIterable_iteration(Iterator<E> iter, T collector, Function2<E, T, Mono<T>> mapper) {
        if(!iter.hasNext()) {
            return Mono.just(collector);
        }
        return Mono.just(collector)
                .flatMap ({ result ->
                    mapper.apply(iter.next(), collector).flatMap(col -> {
                        return asyncReduceIterable(iter, col, mapper)
                    })
                })
    }

    // TODO not sure this is needed in Reactor
    static Mono<Object> after(Iterable<Mono> singles) {
        if(singles.size() > 0) {
            return Mono.zip(singles, __ -> CONTINUE)
        } else {
            return Mono.just(CONTINUE)
        }
    }

    static <T> Mono<T> later(Consumer<CompletableFuture<T>> resolver) {
        def fut = new CompletableFuture<T>();
        resolver(fut)
        return Mono.fromFuture(fut)
    }

    static Collector observeFirst() {
        return Collector.of(
                __ -> null,
                (a, t) -> t,
                (a1, a2) -> a1,
                a -> a);
    }
}
