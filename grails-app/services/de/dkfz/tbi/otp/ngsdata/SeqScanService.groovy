package de.dkfz.tbi.otp.ngsdata

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class SeqScanService {

    // locks for operation that are not tread safe
    private final Lock buildSeqScanLock = new ReentrantLock()

    /**
     *
     * Build SeqScans
     * This functions search sequencing tracks which where not assigned
     * to any SeqScan and calls a method to build a specific SeqScan
     *
     */
    void buildSeqScans() {
        List<SeqTrack> seqTracks = SeqTrack.list()
        println "- number of new tracks: ${seqTracks.size()}"
        seqTracks.each { buildSeqScan(it) }
    }

    /**
     * Build one SeqScan based on parameters in
     * the input SeqTrack. If SeqTrack is already used in other
     * SeqScan no new SeqScan will be created.
     *
     * Its prove synchronizing multiple {@link Thread}s for the helper
     * {@link #buildSeqScanPrivate(SeqTrack)}
     *
     * @param seqTrack - new Sequencing Track
     */
    void buildSeqScan(SeqTrack seqTrack) {
        buildSeqScanLock.lock()
        try {
            buildSeqScanPrivate(seqTrack)
        } finally {
            buildSeqScanLock.unlock()
        }
    }

    /**
     * Build one SeqScan based on parameters in
     * the input SeqTrack. If SeqTrack is already used in other
     * SeqScan no new SeqScan will be created.
     *
     * This method should never used directly, but only about
     * {@link #buildSeqScan(SeqTrack)} for synchronizing multiple {@link Thread}s
     *
     * @param seqTrack - new Sequencing Track
     */
    private void buildSeqScanPrivate(SeqTrack seqTrack) {
        // maybe track already consumed
        if (MergingAssignment.countBySeqTrack(seqTrack) > 0) {
            log.debug("seqTrack ${seqTrack} already used")
            return
        } else {
            log.debug("building secScan from ${seqTrack}")
        }
        // take parameters
        Sample sample = seqTrack.sample
        SeqPlatform seqPlatform = seqTrack.seqPlatform
        SeqType seqType = seqTrack.seqType
        // find all lanes
        def c = SeqTrack.createCriteria()
        def seqTracksToMerge = c.list {
            and {
                eq("sample", sample)
                eq("seqPlatform", seqPlatform)
                eq("seqType", seqType)
            }
        }
        log.debug("found lanes: ${seqTracksToMerge}")
        // find old seqScan and invalidate it
        def criteria = SeqScan.createCriteria()
        def oldSeqScans = criteria.list {
            and {
                eq("sample", sample)
                eq("seqPlatform", seqPlatform)
                eq("seqType", seqType)
            }
        }
        oldSeqScans.each { SeqScan old ->
            old.state = SeqScan.State.OBSOLETE
            old.save(flush: true)
        }
        log.debug("invalidating ${oldSeqScans.size()} seq scans")
        // create new seqScan
        SeqScan seqScan = new SeqScan(
                alignmentParams : null,
                sample: sample,
                seqPlatform: seqPlatform,
                seqType: seqType
                )
        seqScan.save(flush: true)
        seqTracksToMerge.each { SeqTrack iTrack ->
            MergingAssignment mergingAssignment = new MergingAssignment(seqTrack: iTrack, seqScan: seqScan)
            mergingAssignment.save(flush: true)
        }

        fillSeqScan(seqScan)
        fillSeqCenters(seqScan)
        fillInsertSize(seqScan)

        seqScan.save(flush: true)
        sample.save(flush: true)
        seqPlatform.save(flush: true)
        seqType.save(flush: true)
    }

    /**
     * Its prove synchronizing multiple {@link Thread}s for the helper
     * {@link #buildSeqScanPrivate(List<SeqTrack>, AlignmentParams)}
     */
    SeqScan buildSeqScan(List<SeqTrack> tracks, AlignmentParams alignParams) {
        buildSeqScanLock.lock()
        try {
            return buildSeqScanPrivate(tracks, alignParams)
        } finally {
            buildSeqScanLock.unlock()
        }
    }

    /**
     * This method should never used directly, but only about
     * {@link #buildSeqScan(List, AlignmentParams)} for synchronizing multiple {@link Thread}s
     *
     * @param seqTrack - new Sequencing Track
     */
    private SeqScan buildSeqScanPrivate(List<SeqTrack> tracks, AlignmentParams alignParams) {
        // take parameters
        SeqTrack seqTrack = tracks.get(0)
        Sample sample = seqTrack.sample
        SeqPlatform seqPlatform = seqTrack.seqPlatform
        SeqType seqType = getSeqType(tracks)
        // create new seqScan
        SeqScan seqScan = new SeqScan(
                alignmentParams : alignParams,
                sample: sample,
                seqPlatform: seqPlatform,
                seqType: seqType
                )
        seqScan.save(flush: true)
        tracks.each { SeqTrack iTrack ->
            MergingAssignment mergingAssignment = new MergingAssignment(seqTrack: iTrack, seqScan: seqScan)
            mergingAssignment.save(flush: true)
        }
        fillSeqScan(seqScan)
        fillSeqCenters(seqScan)
        fillInsertSize(seqScan)

        seqScan.save(flush: true)
        sample.save(flush: true)
        seqPlatform.save(flush: true)
        seqType.save(flush: true)

        return seqScan
    }

    private SeqType getSeqType(List<SeqTrack> tracks) {
        String name = tracks.get(0).seqType.name
        String library = tracks.get(0).seqType.libraryLayout
        for(SeqTrack track in tracks) {
            name = getNewType(name, track.seqType.name)
            library = getNewLibrary(library, track.seqType.libraryLayout)
        }
        SeqType type = SeqType.findByNameAndLibraryLayout(name, library)
        return type
    }

    private String getNewType(String oldType, String newType) {
        final String lower = SeqTypeNames.EXOME.seqTypeName
        final String higher = "WHOLE_GENOME"
        if (oldType == lower && newType == higher) {
            return higher
        }
        return oldType
    }

    private String getNewLibrary(String oldType, String newType) {
        final String lower = SeqType.LIBRARYLAYOUT_SINGLE
        final String higher = SeqType.LIBRARYLAYOUT_PAIRED
        if (oldType == lower && newType == higher) {
            return higher
        }
        return oldType
    }

    /**
     *
     * Fills SeqScan object with numbers derived from
     * its SeqTrack objects. Coverage is calculated as
     * number of base pairs divided by genome size (3e9).
     *
     * Coverage number will be replaced be a full calculation
     * from coverage analysis
     *
     * @param seqScan - SeqScan object
     */
    private void fillSeqScan(SeqScan seqScan) {
        seqScan.nLanes = MergingAssignment.countBySeqScan(seqScan)
        long nbp = 0
        MergingAssignment.findAllBySeqScan(seqScan).each { MergingAssignment mergingAssignment ->
            Long basePairs = mergingAssignment.seqTrack.nBasePairs
            if (basePairs) {
                nbp += basePairs
            }
        }
        seqScan.nBasePairs = nbp
    }

    /**
     *
     *  Most of samples are sequenced in one sequence center
     *  if this is the case the string seqCenter for a given
     *  SeqScan is filed.
     *
     * @param seqScan
     */
    private void fillSeqCenters(SeqScan seqScan) {
        SeqCenter seqCenter = null
        String name = ""
        MergingAssignment.findAllBySeqScan(seqScan).each { MergingAssignment mergingAssignment ->
            if (!seqCenter) {
                seqCenter = mergingAssignment.seqTrack.run.seqCenter
                name = seqCenter.name
            } else {
                if (mergingAssignment.seqTrack.run.seqCenter != seqCenter) {
                    name += "*"
                }
            }
        }
        seqScan.seqCenters = name
    }

    /**
     *
     * fills insert size as string
     * in case of mixed library "!" signs
     * are used
     *
     * @param SeqScan
     */
    private void fillInsertSize(SeqScan seqScan) {
        boolean defined = false
        int iSize = 0
        String insertSize = ""
        MergingAssignment.findAllBySeqScan(seqScan).each {MergingAssignment mergingAssignment ->
            if (!defined) {
                iSize = mergingAssignment.seqTrack.insertSize
                defined = true
                insertSize += iSize
            } else {
                int thisSize = mergingAssignment.seqTrack.insertSize
                if (thisSize > iSize + 5 || thisSize < iSize - 5) {
                    insertSize += "!"
                }
            }
        }
        seqScan.insertSize = insertSize
    }
}
