package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ReferenceGenomeFromProjectSeqTypeService {

    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    public ReferenceGenome getReferenceGenome(Project project, SeqType seqType) {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.
        findByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType) 
        return referenceGenomeProjectSeqType.referenceGenome
    }
}
