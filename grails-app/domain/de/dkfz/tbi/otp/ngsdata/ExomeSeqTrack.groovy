package de.dkfz.tbi.otp.ngsdata

class ExomeSeqTrack extends SeqTrack {

    public String toString() {
        return "${super.toString()} ${kitInfoReliability} ${libraryPreparationKit}"
    }
}
