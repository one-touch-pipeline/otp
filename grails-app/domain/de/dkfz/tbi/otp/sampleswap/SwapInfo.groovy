package de.dkfz.tbi.otp.sampleswap

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*

class SwapInfo implements Entity {
    User user
    Date dateCreated
    Date dateFinished
    String comment
    String descriptionOfChanges
    List<FileSystemChanges> fileSystemChanges
    Set<SeqTrack> seqTracks

    static hasMany = [
            seqTracks: SeqTrack,
            fileSystemChanges: FileSystemChanges,
    ]

    static mappedBy = [
            fileSystemChanges: "swapInfo",
    ]


    static constraints = {
        user(nullable: false)
        dateFinished(nullable: true)
        comment(nullable: false, blank: false)
        descriptionOfChanges(nullable: false, blank: false)
    }

    static mapping = {
        comment type: "text"
        descriptionOfChanges type: "text"
    }
}
