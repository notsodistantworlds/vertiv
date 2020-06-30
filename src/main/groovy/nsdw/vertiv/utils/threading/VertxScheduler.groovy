package nsdw.vertiv.utils.threading

import io.vertx.core.Vertx
import reactor.core.Disposable
import reactor.core.scheduler.Scheduler

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VertxScheduler implements Scheduler {
    private Vertx vertx
    VertxScheduler(Vertx vertx) {
        this.vertx = vertx
    }

    @Override
    Disposable schedule(Runnable runnable) {
        return getOwnWorker().schedule(runnable)
    }

    @Override
    Disposable schedule(Runnable task, long delay, TimeUnit unit) {
        return getOwnWorker().schedule(task, delay, unit)
    }

    @Override
    Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return getOwnWorker().schedulePeriodically(task, initialDelay, period, unit)
    }

    @Override
    Worker createWorker() {
        return new VertxWorker(vertx)
    }

    Worker ownWorker;
    Worker getOwnWorker() {
        if(ownWorker == null) {
            // we don't bother syncing here, since workers are more-or-less meaningless anyway
            ownWorker = createWorker()
        }
        return ownWorker
    }
}

class VertxWorker implements Scheduler.Worker {
    private Vertx vertx
    VertxWorker(Vertx vertx) {
        this.vertx = vertx
    }

    @Override
    Disposable schedule(Runnable runnable) {
        return schedule(runnable, 1, TimeUnit.MILLISECONDS);
    }

    @Override
    Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        def cancellable = new CancellableVertxTask(run);
        def ms = unit.toMillis(delay)
        vertx.setTimer(ms, timerId -> {
            execCancellableBlocking(cancellable, timerId);
        });
        return cancellable;
    }

    @Override
    Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        def cancellable = new CancellableVertxTask(run);
        def ms = unit.toMillis(period)
        vertx.setPeriodic(ms, timerId -> {
            execCancellableBlocking(cancellable, timerId);
        });
        return cancellable;
    }

    AtomicBoolean disposed = new AtomicBoolean();
    @Override
    void dispose() {
        // ...?
        disposed.set true
    }

    @Override
    boolean isDisposed() {
        return disposed
    }

    private void execCancellableBlocking(CancellableVertxTask task, long timerCancellationToken) {
        vertx.executeBlocking(promise -> {
            task.considerRunning();
            if(task.isDisposed()) {
                vertx.cancelTimer(timerCancellationToken);
            }
            promise.complete();
        }, result -> {
            // well there isn't a result.
        })
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