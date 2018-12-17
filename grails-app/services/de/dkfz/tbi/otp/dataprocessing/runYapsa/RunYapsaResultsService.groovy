package de.dkfz.tbi.otp.dataprocessing.runYapsa

import de.dkfz.tbi.otp.ngsdata.AbstractAnalysisResultsService

class RunYapsaResultsService extends AbstractAnalysisResultsService<RunYapsaInstance> {

    @Override
    String getVersionAttributeName() {
        "programVersion"
    }

    @Override
    Class<RunYapsaInstance> getInstanceClass() {
        RunYapsaInstance
    }

    @Override
    Map getQcData(RunYapsaInstance analysis) {
        [:]
    }
}
