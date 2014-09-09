package de.dkfz.tbi.otp.ngsdata


class SeqTypeService {

    SeqType getSeqType(Long id) {
        return SeqType.get(id)
    }

    List<SeqType> alignableSeqTypes() {
        final List<String> alignableSeqTypeNames = [
            SeqTypeNames.EXOME,
            SeqTypeNames.WHOLE_GENOME,
        ]*.seqTypeName
        List<SeqType> seqTypes = SeqType.findAllByNameInListAndLibraryLayout(alignableSeqTypeNames, "PAIRED")
        assert alignableSeqTypeNames.size() == seqTypes.size()
        return seqTypes
    }
}
