package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.*

trait ProcessParameterObject {

    abstract SeqType getSeqType()

    abstract Individual getIndividual()

    Project getProject() {
        return individual?.project
    }

    Realm getRealm() {
        return project?.realm
    }

    abstract Set<SeqTrack> getContainedSeqTracks()

    abstract short getProcessingPriority()
}
