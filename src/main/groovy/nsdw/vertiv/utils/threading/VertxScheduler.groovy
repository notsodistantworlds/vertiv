package nsdw.vertiv.utils.threading

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.vertx.core.Vertx

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VertxScheduler extends Scheduler {
    private Vertx vertx
    VertxScheduler(Vertx vertx) {
        this.vertx = vertx
    }
    @Override
    Worker createWorker() {
        return new VertxWorker(vertx);
    }
}

class VertxWorker extends Scheduler.Worker{
    private Vertx vertx
    VertxWorker(Vertx vertx) {
        this.vertx = vertx
    }

    @Override
    Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
        def cancellable = new CancellableVertxTask(run);
        vertx.executeBlocking(promise -> {
            cancellable.considerRunning();
            promise.complete();
        }, result -> {
            // well there isn't a result.
        })
        return cancellable;
    }

    AtomicBoolean disposed;
    @Override
    void dispose() {
        // ...?
        disposed.set true
    }

    @Override
    boolean isDisposed() {
        return disposed
    }

    private class CancellableVertxTask implements Disposable {
        AtomicBoolean disposed
        private Runnable routine
        CancellableVertxTask(Runnable routine) {
            // we assume that this is running on a vert.x worker thread
            this.routine = routine
        }
        void considerRunning() {
            if(!disposed) {
                routine.run();
            }
        }

        @Override
        void dispose() {
            disposed.set true
        }
        @Override
        boolean isDisposed() {
            return disposed
        }
    }
}