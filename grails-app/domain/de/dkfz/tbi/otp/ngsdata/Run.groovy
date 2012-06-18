package de.dkfz.tbi.otp.ngsdata

/**
 * Run represents one sequencing Run. It is one of the most important classes
 * in the NGS database. The run is typically submitted all at once, but it
 * could be submitted in parts from different locations and belonging to different
 * projects. The initial locations are stored in RunInitalLocation objects. 
 *
 * Runs arrive in three formats: files in a directory, TAR archive in a directory
 * with uncompressed MetaDataFiles and as TAR archive with a directory and MetaDataFiles
 * inside archive. The initialFormat is a variable to store this format. 
 * Depending on this variable proper uncompressing workflow will be started. 
 * As long as "compressedArchive" variable is set to true, no workflow 
 * other than uncompressing shall be started.
 *
 */

class Run {

    String name                      // run name

    Date dateExecuted = null
    Date dateCreated  = new Date()   // do we need object creation ?

    boolean blacklisted = false      // run is known to be invalid
    boolean multipleSource = false   // for runs for more than one projects

    SeqCenter seqCenter
    SeqPlatform seqPlatform

    enum StorageRealm {DKFZ, BIOQUANT, MIXED}
    StorageRealm storageRealm

    static belongsTo = [
        SeqCenter,
        SeqPlatform
    ]

    static constraints = {
        name(blank: false, unique: true)
        storageRealm(nullable: true)
        dateExecuted(nullable: true)
        dateCreated()
    }

    String toString() {
        name
    }

    String initialMDPaths() {
        String paths = ""
        RunSegment.findAllByRun(this).each {paths += it.mdPath + " "}
        return paths
    }
}
