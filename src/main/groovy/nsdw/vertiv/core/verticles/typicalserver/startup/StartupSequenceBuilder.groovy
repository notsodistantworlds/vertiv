package nsdw.vertiv.core.verticles.typicalserver.startup

class StartupSequenceBuilder {
    private List<ScheduledStartupStage> scheduledStages;
    private StartupSequenceBuilder() {

    }
    static StartupSequenceBuilder startupSequence() {
        return new StartupSequenceBuilder();
    }
    StartupSequenceBuilder withStage(StartupStageCue invokedWhen, IStartupStage stage) {

    }
}
