package de.dkfz.tbi.otp.dataprocessing.runYapsa

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.Entity

/**
 * An execution of the RunYAPSA workflow.
 */
class RunYapsaInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity {

    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.runYapsaSamplePairPath, instanceName)
    }

    @Override
    RunYapsaConfig getConfig() {
        return RunYapsaConfig.get(super.config.id)
    }

    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    LibraryPreparationKit getLibraryPreparationKit() {
        return sampleType2BamFile.workPackage.libraryPreparationKit
    }

    @Override
    String toString() {
        return "RYI ${id}${withdrawn ? ' (withdrawn)' : ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }
}
