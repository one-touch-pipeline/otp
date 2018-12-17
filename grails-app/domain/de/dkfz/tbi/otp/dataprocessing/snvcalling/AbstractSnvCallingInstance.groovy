package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.utils.Entity

/**
 * For each tumor-control pair the snv pipeline will be called.
 * The AbstractSnvCallingInstance symbolizes one call of the pipeline.
 */
abstract class AbstractSnvCallingInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity {

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32
     */
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.snvSamplePairPath, instanceName)
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32/config.txt
     */
    OtpPath getConfigFilePath() {
        return new OtpPath(instancePath, "config.txt")
    }

    File getSnvCallingResult() {
        new File(getWorkDirectory(), "snvs_${individual.pid}_raw.vcf.gz")
    }

    File getSnvDeepAnnotationResult() {
        new File(getWorkDirectory(), "snvs_${individual.pid}.vcf.gz")
    }

    File getResultRequiredForRunYapsa() {
        new File(getWorkDirectory(), "snvs_${individual.pid}_somatic_snvs_conf_8_to_10.vcf")
    }

    File getCombinedPlotPath() {
        return new File(getWorkDirectory(), "snvs_${getIndividual().pid}_allSNVdiagnosticsPlots.pdf")
    }

    @Override
    String toString() {
        return "SCI ${id} ${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }
}
