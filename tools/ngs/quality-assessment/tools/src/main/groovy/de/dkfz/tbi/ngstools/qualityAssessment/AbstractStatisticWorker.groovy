package de.dkfz.tbi.ngstools.qualityAssessment

abstract class AbstractStatisticWorker<Record> implements StatisticLogic<Record> {

    protected Parameters parameters

    protected FileParameters fileParameters

    @Override
    public void setParameters(Parameters parameters) {
        this.parameters = parameters
    }

    @Override
    public void setFileParameters(FileParameters fileParameters) {
        this.fileParameters = fileParameters
    }
}
