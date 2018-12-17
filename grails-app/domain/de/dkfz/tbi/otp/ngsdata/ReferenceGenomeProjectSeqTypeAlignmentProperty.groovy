package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ReferenceGenomeProjectSeqTypeAlignmentProperty implements Entity {

    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    String name

    String value


    static belongsTo = [
            referenceGenomeProjectSeqType: ReferenceGenomeProjectSeqType,
    ]

    static constraints = {
        name blank: false, unique: ['referenceGenomeProjectSeqType']
        value blank: false, maxSize: 500
    }
}
