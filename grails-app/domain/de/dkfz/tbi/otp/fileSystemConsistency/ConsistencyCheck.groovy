package de.dkfz.tbi.otp.fileSystemConsistency

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

class ConsistencyCheck implements ProcessParameterObject, Entity {

    /**
     * Date of when the consistency check was performed
     */
    Date date = new Date()

    @Override
    SeqType getSeqType() {
        throw new UnsupportedOperationException()
    }

    @Override
    Individual getIndividual() {
        throw new UnsupportedOperationException()
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        throw new UnsupportedOperationException()
    }

    @Override
    short getProcessingPriority() {
        ProcessingPriority.NORMAL.priority
    }
}
