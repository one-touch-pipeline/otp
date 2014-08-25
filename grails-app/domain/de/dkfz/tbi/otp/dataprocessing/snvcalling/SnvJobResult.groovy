package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile

/**
 * Represents all results (particularly VCF (variant call format) files) of one job for the comparison of disease and control.
 * Even if more than one file will be produced by the called script, only one instance of SnvJobResult represents these results.
 *
 *
 */
class SnvJobResult {

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
     * as {@link SnvProcessingStates.IN_PROGRESS}.
     */
    SnvProcessingStates processingState = SnvProcessingStates.IN_PROGRESS


    static belongsTo = [
        snvCallingInstance: SnvCallingInstance
    ]

    static constraints = {
        processingState validator: { val, obj ->
            return val != SnvProcessingStates.IGNORED
        }
        step unique: 'snvCallingInstance'
        inputResult validator: { val, obj ->
            if (val != null && val.withdrawn == true && obj.withdrawn != true) {
                return false
            }
            if (val != null && val.processingState != SnvProcessingStates.FINISHED) {
                return false
            }

            if (val != null && (val.tumorBamFile != obj.tumorBamFile || val.controlBamFile != obj.controlBamFile)) {
                return false
            }

            return (obj.step == SnvCallingStep.CALLING ? val == null : val != null)
        }
    }

    ProcessedMergedBamFile getTumorBamFile() {
        return snvCallingInstance.tumorBamFile
    }

    ProcessedMergedBamFile getControlBamFile() {
        return snvCallingInstance.controlBamFile
    }
}
