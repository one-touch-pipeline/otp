package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.InformationReliability

class ExomeSeqTrack extends SeqTrack {


    public String toString() {
        return "${super.toString()} ${kitInfoReliability} ${libraryPreparationKit}"
    }

}
