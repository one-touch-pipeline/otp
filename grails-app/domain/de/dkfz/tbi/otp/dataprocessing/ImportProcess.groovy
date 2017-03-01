package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.Entity

/**
 * Represents the state of the import process of the externally processed merged BAM files
 */
class ImportProcess implements Entity {

    enum State {
        NOT_STARTED,
        STARTED,
        FINISHED
    }

    State state = State.NOT_STARTED

    boolean replaceSourceWithLink

    boolean triggerSnv

    boolean triggerIndel

    boolean triggerAceseq

    Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles

    static hasMany = [
            externallyProcessedMergedBamFiles: ExternallyProcessedMergedBamFile
    ]

    static constraints = {
        externallyProcessedMergedBamFiles validator: { val, obj ->
            List<ImportProcess> importProcesses = ImportProcess.createCriteria().listDistinct {
                externallyProcessedMergedBamFiles {
                    'in'('id', val*.id)
                }
            }
            for (ImportProcess importProcess : importProcesses) {
                if (importProcess && importProcess.id != obj.id) {
                    return "This set of bam files was already imported"
                }
            }
            return true
        }
    }
}
