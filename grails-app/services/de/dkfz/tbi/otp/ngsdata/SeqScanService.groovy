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

        def seqTracks = SeqTrack.findAll()
        def seqTracksNew = []

        // create a list of new seqTracks
        seqTracks.each { seqTrack ->
            if (seqTrack.seqScan.size() == 0)
                seqTracksNew << seqTrack
        }

        println "- number of new tracks: ${seqTracksNew.size()}"

        seqTracksNew.each {
            buildSeqScan(it)
        }
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
        //
        // build one SeqScan
        //

        // maybe track already consumed
        if (seqTrack.seqScan.size() != 0) {
            println "seqTrack ${seqTrack} already used"
            return
        }
        // take parameters
        Sample sample = seqTrack.sample
        SeqTech seqTech = seqTrack.seqTech
        SeqType seqType = seqTrack.seqType

       //AlignmentParams params = AlignmentParams.get(1)


        // find all lanes
        def c = SeqTrack.createCriteria()
        def seqTracksToMerge = c.list {
            and {
                eq("sample", sample)
                eq("seqTech", seqTech)
                eq("seqType", seqType)
            }
        }
    
            println "found lanes: ${seqTracksToMerge}"
    
            /*
            // check if all have bam file
            boolean allBams = true
            seqTracksToMerge.each { SeqTrack iTrack ->
                if (!iTrack.hasFinalBam) allBams = false
            }
    
            if (!allBams) return
            */
    
    
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
    
    
            println "invalidating ${oldSeqScans.size()} seq scans"
    
    
            // create new seqScan
    
            SeqScan seqScan = new SeqScan(
                alignmentParams : null,
                sample: sample,
                seqTech: seqTech,
                seqType: seqType
            )
    
            //sample.addToSeqScans(seqScan)
            //seqTech.addToSeqScans(seqScan)
            //seqType.addToSeqScans(seqScan)
    
            seqTracksToMerge.each { SeqTrack iTrack ->
                seqScan.addToSeqTracks(iTrack)
                safeSave(iTrack)
            }
    
            fillSeqScan(seqScan)
            fillSeqCenters(seqScan)
    
            safeSave(seqScan)
            safeSave(sample)
            safeSave(seqTech)
            safeSave(seqType)
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
            //seqScan.coverage = nbp / 3.0e9
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
                }
    
                if (seqCenter != null) {
                    if (seqTrack.run.seqCenter != seqCenter)
                        name += "*"
                }
            }
            seqScan.seqCenters = name
        }


    /**
      *
      * probably will go to separate static class
      * no formal exception, information only
      *
      * @param obj
      */

    private void safeSave(def obj) {

           obj.validate()
           if (obj.hasErrors()) {
               println obj.errors
               return
           }

           if (!obj.save())
           println "can not save ${obj}"
    }

}
