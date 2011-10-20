package de.dkfz.tbi.otp.ngsdata

class Run {

    String name

    Date dateExecuted = null
    Date dateCreated  = new Date()

    String dataPath
    String mdPath

    boolean complete             // complete run was delivered
    boolean allFilesUsed = false
    boolean finalLocation = false

    boolean multipleSource

    //ProcessDefinition lastStep = null
    //boolean isBlocked = false    // under processing
    //int lastStep = 0             // last processing step

    /*
    static constraints = {
        name(blank: false, unique: true)
                allFilesUsed()
                projects(nullable: true)
                dateExecuted(nullable: true)
                dateCreated()
                complete()
                //isBlocked()
                dataPath()
                mdPath()
                //lastStep(nullable: true)
        }

        static belongsTo = [projects : Project, seqCenter : SeqCenter, seqTech : SeqTech]
        static hasMany = [
                projects  : Project,
                dataFiles : DataFile,
                seqTracks : SeqTrack,
        ]
	*/

        String toString() {
                name
        }

}
