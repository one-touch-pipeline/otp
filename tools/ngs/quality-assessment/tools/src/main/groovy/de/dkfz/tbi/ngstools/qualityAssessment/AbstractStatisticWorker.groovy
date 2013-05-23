package de.dkfz.tbi.ngstools.qualityAssessment

abstract class AbstractStatisticWorker<Record> implements StatisticLogic<Record> {

    protected Parameters parameters

    @Override
    public void setParameters(Parameters parameters) {
        this.parameters = parameters
    }
}
