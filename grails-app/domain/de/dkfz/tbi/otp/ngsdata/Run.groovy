package de.dkfz.tbi.otp.ngsdata

class Run {

    String name                      // run name

    Date dateExecuted = null
    Date dateCreated  = new Date()   // do we need object creation ?

    boolean blacklisted = false      // run is known to be invalid
    boolean complete = false         // complete run was delivered
    boolean allFilesUsed = false     // flag if data if all files find relations
    boolean finalLocation = false    // was run moved to final destination from ftp

    boolean multipleSource           // for runs from a few projects

    SeqCenter seqCenter
    SeqPlatform seqPlatform

    enum Realm {DKFZ, BIOQUANT, MIXED}
    Realm realm

    static belongsTo = [
        Project,
        SeqCenter,
        SeqPlatform
    ]

    static constraints = {
        name(blank: false, unique: true)
        realm(nullable: true)
        allFilesUsed()
        dateExecuted(nullable: true)
        dateCreated()
        complete()
    }

    String toString() {
        name
    }

    String initialMDPaths() {
        String paths = ""
        RunInitialPath.findAllByRun(this).each {paths += it.mdPath + " "}
        return paths
    }
}
