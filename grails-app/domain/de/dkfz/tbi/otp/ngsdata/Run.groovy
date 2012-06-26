package de.dkfz.tbi.otp.ngsdata

/**
 * Run represents one sequencing Run. It is one of the most important classes
 * in the NGS database. The run is typically submitted all at once, but it
 * could be submitted in parts from different locations and belonging to different
 * projects. The initial locations are stored in RunSegment objects. 
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
