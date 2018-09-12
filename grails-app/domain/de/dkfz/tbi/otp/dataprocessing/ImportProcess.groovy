package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

import static org.springframework.util.Assert.*

/**
 * Represents the state of the import process of the externally processed merged BAM files
 */
class ImportProcess implements Entity, ProcessParameterObject {

    enum State {
        NOT_STARTED,
        STARTED,
        FINISHED
    }

    State state = State.NOT_STARTED

    boolean replaceSourceWithLink

    boolean triggerAnalysis

    Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles

    static hasMany = [
            externallyProcessedMergedBamFiles: ExternallyProcessedMergedBamFile,
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

    public void updateState(State state) {
        notNull(state, "the input state for the method updateState is null")
        this.state = state
        assert this.save(flush:true)
    }

    @Override
    SeqType getSeqType() {
        return null
    }

    @Override
    Individual getIndividual() {
        return null
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return []
    }

    @Override
    short getProcessingPriority() {
        return 0
    }
}
