package de.dkfz.tbi.otp.ngsdata


class SeqTypeService {

    SeqType getSeqType(Long id) {
        return SeqType.get(id)
    }
}
