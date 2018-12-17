package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.ngsdata.AbstractAnalysisResultsService

class SophiaResultsService extends AbstractAnalysisResultsService<SophiaInstance> {

    @Override
    String getVersionAttributeName() {
        "pluginVersion"
    }

    @Override
    Class<SophiaInstance> getInstanceClass() {
        SophiaInstance
    }

    @Override
    Map getQcData(SophiaInstance analysis) {
        SophiaQc qc = SophiaQc.findBySophiaInstance(analysis)
        [
                controlMassiveInvPrefilteringLevel: qc?.controlMassiveInvPrefilteringLevel,
                tumorMassiveInvFilteringLevel: qc?.tumorMassiveInvFilteringLevel,
                rnaContaminatedGenesMoreThanTwoIntron: qc?.rnaContaminatedGenesMoreThanTwoIntron,
                rnaContaminatedGenesCount: qc?.rnaContaminatedGenesCount,
                rnaDecontaminationApplied: qc?.rnaDecontaminationApplied,
        ]
    }
}
