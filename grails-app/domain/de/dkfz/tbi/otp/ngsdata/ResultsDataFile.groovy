package de.dkfz.tbi.otp.ngsdata

class ResultsDataFile {

    String identifier
    Date dateCreated = new Date()
    String text

    static constraints = {
        text(size:1..1000000)
    }
}
