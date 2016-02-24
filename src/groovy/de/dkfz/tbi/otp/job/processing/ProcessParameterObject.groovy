package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

trait ProcessParameterObject {

    abstract SeqType getSeqType()

    abstract Individual getIndividual()

    Project getProject() {
        return individual?.project
    }

    abstract Set<SeqTrack> getContainedSeqTracks()

    abstract short getProcessingPriority()
}
