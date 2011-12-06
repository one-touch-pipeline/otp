package de.dkfz.tbi.otp.ngsdata

class SeqScanService {

    /**
     *
     * Build SeqScans
     * This functions search sequencing tracks which where not assigned
     * to any SeqScan and calls a method to build a specific SeqScan
     *
     */
    void buildSeqScans() {
        List<SeqTrack> seqTracks = SeqTrack.findAll()
        List<SeqTrack> seqTracksNew = []
        // create a list of new seqTracks
        seqTracks.each { seqTrack ->
            if (seqTrack.seqScan.size() == 0) {
                seqTracksNew << seqTrack
            }
        }
        log.debug("- number of new tracks: ${seqTracksNew.size()}")
        seqTracksNew.each { buildSeqScan(it) }
    }

    /**
     *
     * Build one SeqScan based on parameters in
     * the input SeqTrack. If SeqTrack is already used in other
     * SeqScan no new SeqScan will be created
     *
     * @param seqTrack - new Sequencing Track
     */
    void buildSeqScan(SeqTrack seqTrack) {
        // maybe track already consumed
        if (seqTrack.seqScan.size() != 0) {
            log.debug("seqTrack ${seqTrack} already used")
            return
        }
        // take parameters
        Sample sample = seqTrack.sample
        SeqTech seqTech = seqTrack.seqTech
        SeqType seqType = seqTrack.seqType
        // find all lanes
        def c = SeqTrack.createCriteria()
        def seqTracksToMerge = c.list {
            and {
                eq("sample", sample)
                eq("seqTech", seqTech)
                eq("seqType", seqType)
            }
        }
        log.debug("found lanes: ${seqTracksToMerge}")
        // find old seqScan and invalidate it
        def criteria = SeqScan.createCriteria()
        def oldSeqScans = criteria.list {
            and {
                eq("sample", sample)
                eq("seqTech", seqTech)
                eq("seqType", seqType)
            }
        }
        oldSeqScans.each {SeqScan old ->
            old.state = SeqScan.State.OBSOLETE
            safeSave(old)
        }
        log.debug("invalidating ${oldSeqScans.size()} seq scans")
        // create new seqScan
        SeqScan seqScan = new SeqScan(
                alignmentParams : null,
                sample: sample,
                seqTech: seqTech,
                seqType: seqType
                )
        seqTracksToMerge.each { SeqTrack iTrack ->
            seqScan.addToSeqTracks(iTrack)
            safeSave(iTrack)
        }

        fillSeqScan(seqScan)
        fillSeqCenters(seqScan)
        fillInsertSize(seqScan)

        seqScan.save()
        sample.save()
        seqTech.save()
        seqType.save()
    }

    /**
     *
     * @param tracks
     * @param alignParams
     * @return
     */
    SeqScan buildSeqScan(List<SeqTrack> tracks, AlignmentParams alignParams) {
        // take parameters
        SeqTrack seqTrack = tracks.get(0)
        Sample sample = seqTrack.sample
        SeqTech seqTech = seqTrack.seqTech
        SeqType seqType = seqTrack.seqType
        // create new seqScan
        SeqScan seqScan = new SeqScan(
                alignmentParams : alignParams,
                sample: sample,
                seqTech: seqTech,
                seqType: seqType
                )
        tracks.each { SeqTrack iTrack ->
            seqScan.addToSeqTracks(iTrack)
            safeSave(iTrack)
        }
        fillSeqScan(seqScan)
        fillSeqCenters(seqScan)
        fillInsertSize(seqScan)

        seqScan.save()
        sample.save()
        seqTech.save()
        seqType.save()

        return seqScan
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
        seqScan.nLanes = seqScan.seqTracks.size()
        long nbp = 0
        seqScan.seqTracks.each {SeqTrack seqTrack ->
            nbp += seqTrack.nBasePairs
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
        seqScan.seqTracks.each {SeqTrack seqTrack ->
            if (seqCenter == null) {
                seqCenter = seqTrack.run.seqCenter
                name = seqCenter.name
            } else if (seqCenter != null) {
                if (seqTrack.run.seqCenter != seqCenter)
                    name += "*"
            }
        }
        seqScan.seqCenters = name
    }

    /**
     *
     * fills insert size as string
     * in case of mized library "!" signs
     * are used
     *
     * @param SeqScan
     */
    private void fillInsertSize(SeqScan seqScan) {
        boolean defined = false
        int iSize = 0
        String insertSize = ""
        seqScan.seqTracks.each {SeqTrack seqTrack ->
            if (!defined) {
                iSize = seqTrack.insertSize
                defined = true
                insertSize += iSize
            } else {
                if (seqTrack.insertSize != iSize) {
                    insertSize += "!"
                }
            }
        }
        seqScan.insertSize = insertSize
    }
}
