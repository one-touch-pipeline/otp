package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

/**
 * Represents a set of SeqTracks that were aligned and merged into one bam file.
 * It was introduced for files that were processed outside of OTP and imported later,
 * but may also be used for as a short cut for files processed by OTP
 */
class FastqSet {

    static hasMany = [
        seqTracks: SeqTrack
    ]

    Project getProject() {
        // iterator().next() gets an arbitrary element of the seqTracks set
        return seqTracks.iterator().next().project
    }

    Individual getIndividual() {
        return seqTracks.iterator().next().sample.individual
    }

    Sample getSample() {
        return seqTracks.iterator().next().sample
    }

    SeqType getSeqType() {
        return seqTracks.iterator().next().seqType
    }

    static constraints = {
        seqTracks validator: {field, inst ->
            validateSeqTracks(field)
        }
    }

    /** check whether all seq tracks have the same seq type and sample */
    static private validateSeqTracks (Set<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return false
        } else if(seqTracks.size() > 1) {
            Sample sample = seqTracks.iterator().next().sample
            SeqType seqType = seqTracks.iterator().next().seqType
            for(SeqTrack seqTrack : seqTracks) {
                if(sample != seqTrack.sample || seqType != seqTrack.seqType) {
                    return false
                }
            }
        }
        return true
    }
}
