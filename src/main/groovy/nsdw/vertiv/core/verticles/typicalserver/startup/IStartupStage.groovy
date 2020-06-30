package nsdw.vertiv.core.verticles.typicalserver.startup

import reactor.core.publisher.Mono

interface IStartupStage {
    Mono<Object> execStage();
}

class ScheduledStartupStage {
    public StartupStageCue cue;
    public boolean allowRunInParallel;
    public IStartupStage stage;
}

/**
 * Specifies when a startup stage should be executed.
 * Note that some elements are equivalent. However, we do not
 * guarantee this equivalency moving forward.
  */
enum StartupStageCue {
    BEFORE_ALL,

    BEFORE_LOAD_CONFIG,

    AFTER_LOAD_CONFIG,
    BEFORE_ROUTER_SETUP,

    AFTER_ROUTER_SETUP,
    BEFORE_SERVER_STARTUP,

    AFTER_SERVER_STARTUP,
    AFTER_ALL
}