package de.dkfz.tbi.otp.ngsdata

class SeqScanController {

    def scaffold = SeqScan

    //def index() { 

    def show = {

        SeqScan scan = SeqScan.get(params.id)
        Set<String> runs = new HashSet<String>()
        List<SeqTrack> tracks = MergingAssignment.findAllBySeqScan(scan, [sort: "seqTrack.laneId"])

        tracks.each {seqTrack ->
            runs.add(seqTrack.seqTrack.run.name)
        }

        String[] runNames = runs.toArray()
        Arrays.sort(runNames)

        [scan : scan, tracks: tracks, runs: runNames]
    }
}
