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
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.indelSamplePairPath, instanceName)
    }

    @Override
    public String toString() {
        return "ICI ${id}${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return RoddyWorkflowConfig.get(super.config.id)
    }

    List<File> getResultFilePathsToValidate() {
        return ["indel_${this.individual.pid}.vcf.gz", "indel_${this.individual.pid}.vcf.raw.gz"].collect {
            new File(getWorkDirectory(), it)
        }
    }

    File getCombinedPlotPath() {
        return new File(getWorkDirectory(), "screenshots/indel_somatic_functional_combined.pdf")
    }
}
