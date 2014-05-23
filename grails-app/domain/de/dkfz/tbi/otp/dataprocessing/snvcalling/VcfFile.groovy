package de.dkfz.tbi.otp.dataprocessing.snvcalling


/**
 * A vcf (variant call format) file lists the results of a comparison between a tumor and a control.
 *
 *
 */
class VcfFile {

    String fileName

    Date dateCreated

    Date lastUpdated

    /**
     * Specifies the last step that was processed with this
     */
    SnvCallingStep step

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
    }
}
