package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class IndelCallingInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity, RoddyAnalysisResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String
    ]

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/indel_results/paired/tumor_control/2014-08-25_15h32
     */
    OtpPath getIndelInstancePath() {
        return new OtpPath(samplePair.indelSamplePairPath, instanceName)
    }

    @Override
    public String toString() {
        return "ICI ${id}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    File getWorkDirectory() {
        return getIndelInstancePath().absoluteDataManagementPath
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return super.config
    }
}
