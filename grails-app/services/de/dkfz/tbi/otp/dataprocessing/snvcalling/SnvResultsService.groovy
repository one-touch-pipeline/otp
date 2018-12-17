package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.AbstractAnalysisResultsService

class SnvResultsService extends AbstractAnalysisResultsService<AbstractSnvCallingInstance> {

    @Override
    String getVersionAttributeName() {
        "pluginVersion"
    }

    @Override
    Class<AbstractSnvCallingInstance> getInstanceClass() {
        AbstractSnvCallingInstance
    }

    @Override
    Map getQcData(AbstractSnvCallingInstance analysis) {
        [:]
    }
}
