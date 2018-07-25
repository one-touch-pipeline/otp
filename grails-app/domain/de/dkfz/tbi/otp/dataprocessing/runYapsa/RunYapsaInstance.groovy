package de.dkfz.tbi.otp.dataprocessing.runYapsa

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class RunYapsaInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity {

    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.runYaspaSamplePairPath, instanceName)
    }

    @Override
    RunYapsaConfig getConfig() {
        return RunYapsaConfig.get(super.config.id)
    }

    ReferenceGenome getReferenceGenome() {
        CollectionUtils.exactlyOneElement(containedSeqTracks*.configuredReferenceGenome.unique())
    }

    LibraryPreparationKit getLibraryPreparationKit() {
        CollectionUtils.exactlyOneElement(containedSeqTracks*.libraryPreparationKit.unique())
    }

    @Override
    String toString() {
        return "RYI ${id}${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }
}
