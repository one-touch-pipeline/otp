package de.dkfz.tbi.otp.ngsdata

class ExomeSeqTrack extends SeqTrack {

    @Override
    String toString() {
        return "${super.toString()} ${kitInfoReliability} ${libraryPreparationKit}"
    }
}
