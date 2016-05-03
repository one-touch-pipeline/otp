package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ResultsDataFile implements Entity {

    String identifier
    Date dateCreated = new Date()
    String text

    static constraints = {
        text(size:1..1000000)
    }
}
