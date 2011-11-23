package de.dkfz.tbi.otp.ngsdata

class Run {

    String name                      // run name

    Date dateExecuted = null
    Date dateCreated  = new Date()   // do we need object creation ?

    String dataPath                  // path to data (ftp area)
    String mdPath                    // path to meta-data

    boolean complete                 // complete run was delivered
    boolean allFilesUsed = false     // flag if data if all files find relations
    boolean finalLocation = false    // was run moved to final destination from ftp

    boolean multipleSource           // for runs from a few projects 

    SeqCenter seqCenter
    SeqTech seqTech

    static belongsTo = [
        Project,
        SeqCenter,
        SeqTech
    ]

    static hasMany = [
        projects  : Project,
        dataFiles : DataFile,
        seqTracks : SeqTrack,
    ]

    static constraints = {
        name(blank: false, unique: true)
        allFilesUsed()
        projects(nullable: true)
        dateExecuted(nullable: true)
        dateCreated()
        complete()
        dataPath()
        mdPath()
    }

    static mapping = {
        dataFiles sort:'fileName'
        seqTracks sort:'laneId'
    }


    String toString() {
        name
    }
}
