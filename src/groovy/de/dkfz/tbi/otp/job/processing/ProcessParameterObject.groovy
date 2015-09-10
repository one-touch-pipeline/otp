package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqPlatformModelLabel
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

interface ProcessParameterObject {

    SeqType getSeqType()

    Individual getIndividual()

    Set<SeqTrack> getContainedSeqTracks()

    short getProcessingPriority()
}
