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

    Double dataQuality = null
    boolean qualityEvaluated = false

    static belongsTo = [
        SeqCenter,
        SeqPlatform
    ]

    static constraints = {
        name(blank: false, unique: true)
        storageRealm(nullable: true)
        dateExecuted(nullable: true)
        dateCreated()
        dataQuality(nullable: true)
        qualityEvaluated(nullable: true)
    }

    String toString() {
        name
    }

    String initialMDPaths() {
        String paths = ""
        RunSegment.findAllByRun(this).each {paths += it.mdPath + " "}
        return paths
    }

    /*
     * is used in ExecutionService to create Cluster Jobs,
     * returns null if a run has more than one sequencing type,
     * because this case is unusable for creating Cluster Jobs
     */
    SeqType getSeqType() {
        List<SeqType> seqTypes = SeqTrack.findAllByRun(this)*.seqType
        if (seqTypes.unique().size() == 1) {
            return seqTypes.get(0)
        } else {
            return null
        }
    }

    /**
     * It returns the highest priority of the corresponding projects.
     */
    short getProcessingPriority() {
        return DataFile.findAllByRun(this)*.project*.processingPriority.max()
    }

    static mapping = {
        seqCenter index: "run_seq_center_idx"
        seqPlatform index: "run_seq_platform_idx"
    }
}
