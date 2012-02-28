package de.dkfz.tbi.otp.ngsdata

class MetaDataFile {

    String fileName
    String filePath
    boolean used = false
    Date dateCreated = null

    static belongsTo = [
        runInitialPath : RunInitialPath
    ]
    static constraints = {
        fileName()
        filePath()
        runInitialPath()
        dateCreated()
    }
}
