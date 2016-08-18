package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * Represents all results (particularly VCF (variant call format) files) of one job for the comparison of disease and control.
 * Even if more than one file will be produced by the called script, only one instance of SnvJobResult represents these results.
 *
 *
 */
class SnvJobResult implements Entity {

    Date dateCreated

    Date lastUpdated

    boolean withdrawn = false

    /**
     * Specifies the last step that was processed with this
     */
    SnvCallingStep step

    /**
     * Specifies the result which was produced by a previous job
     * and is the input for the current job which produces this SnvJobResult.
     * <code>null</code> if this is a result for {@link SnvCallingStep#CALLING}.
     */
    SnvJobResult inputResult

    /**
     * The overall processing state of this vcf file.
     * At the moment, when the file is created a job is already working on it, which is why it always starts
     * as {@link SnvProcessingStates#IN_PROGRESS}.
     * @see SnvProcessingStates#FAILED
     */
    SnvProcessingStates processingState = SnvProcessingStates.IN_PROGRESS

    /**
     * The script which was used to produce these results
     */
    ExternalScript externalScript

    /**
     * Stores the script which was used to join the vcf files of all chromosomes.
     * This script is only used in the CallingJob.
     * Therefore it shall only be set when then step = {@link SnvCallingStep#CALLING}
     */
    ExternalScript chromosomeJoinExternalScript

    String md5sum

    Long fileSize

    static belongsTo = [
        snvCallingInstance: SnvCallingInstance
    ]
    SnvCallingInstance snvCallingInstance

    static constraints = {
        processingState validator: { val, obj ->
            return val != SnvProcessingStates.FAILED
        }
        step unique: 'snvCallingInstance'
        withdrawn validator: { boolean withdrawn, SnvJobResult result ->
            return withdrawn ||
                    !result.snvCallingInstance.sampleType1BamFile.withdrawn &&
                    !result.snvCallingInstance.sampleType2BamFile.withdrawn &&
                    !result.inputResult?.withdrawn
        }
        inputResult nullable: true, validator: { val, obj ->
            if (val != null && val.processingState != SnvProcessingStates.FINISHED) {
                return false
            }

            if (val != null && (val.sampleType1BamFile.id != obj.sampleType1BamFile.id || val.sampleType2BamFile.id != obj.sampleType2BamFile.id)) {
                return false
            }

            return (obj.step == SnvCallingStep.CALLING ? val == null : val != null)
        }
        externalScript validator: { ExternalScript val, SnvJobResult obj ->
            return obj.step.externalScriptIdentifier == val.scriptIdentifier &&
                    val.scriptVersion == obj.snvCallingInstance.config.externalScriptVersion
        }
        chromosomeJoinExternalScript nullable: true, validator: { ExternalScript val, SnvJobResult obj ->
            if (obj.step == SnvCallingStep.CALLING) {
                return val.scriptVersion == obj.snvCallingInstance.config.externalScriptVersion
            } else {
                return val == null
            }
        }
        md5sum nullable: true, validator: { val, obj ->
            return validateFileInformation (val, obj, { val ==~ /^[0-9a-f]{32}$/ })
        }

        fileSize nullable: true, validator: { val, obj ->
            return validateFileInformation (val, obj, { val > 0 })
        }
    }

    private static boolean validateFileInformation(def val, def obj, def closure) {
        if (obj.processingState == SnvProcessingStates.FINISHED && [SnvCallingStep.CALLING, SnvCallingStep.SNV_DEEPANNOTATION].contains(obj.step)) {
            return val && closure()
        } else {
            return true
        }
    }


    static mapping = {
        snvCallingInstance index: "snv_job_result_snv_calling_instance_idx"
    }

    AbstractMergedBamFile getSampleType1BamFile() {
        return snvCallingInstance.sampleType1BamFile
    }

    AbstractMergedBamFile getSampleType2BamFile() {
        return snvCallingInstance.sampleType2BamFile
    }

    /**
     * Returns the path of the SnvJobResult.
     * ! Be aware that for the SnvFilterJob there is more than one result file, which is why the directory will be returned here!
     *
     * Example for Calling without given chromosome name
     * ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32/snvs_${pid}_raw.vcf.gz
     */
    OtpPath getResultFilePath(String chromosomeName = null) {
        if (step == SnvCallingStep.CALLING) {
            return new OtpPath(snvCallingInstance.snvInstancePath, step.getResultFileName(snvCallingInstance.individual, chromosomeName))
        } else if (step == SnvCallingStep.FILTER_VCF) {
            return snvCallingInstance.snvInstancePath
        } else {
            return new OtpPath(snvCallingInstance.snvInstancePath, step.getResultFileName(snvCallingInstance.individual))
        }
    }

    void withdraw() {
        SnvJobResult.withTransaction {
            SnvJobResult.findAllByInputResult(this).each {
                it.withdraw()
            }

            assert LogThreadLocal.threadLog : 'This method produces relevant log messages. Thread log must be set.'
            LogThreadLocal.threadLog.info "Execute WithdrawnFilesRename.groovy script afterwards"
            LogThreadLocal.threadLog.info "Withdrawing ${this}"
            withdrawn = true
            assert save(flush: true)
        }
    }
}
