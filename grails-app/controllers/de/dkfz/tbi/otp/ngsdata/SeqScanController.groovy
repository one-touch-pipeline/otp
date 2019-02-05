package de.dkfz.tbi.otp.ngsdata

class SeqScanController {

    def show = {
        SeqScan scan = SeqScan.get(params.id)
        Set<String> runs = [] as Set
        List<SeqTrack> tracks = MergingAssignment.findAllBySeqScan(scan, [sort: "seqTrack.laneId"])

        tracks.each { seqTrack ->
            runs.add(seqTrack.seqTrack.run.name)
        }

        String[] runNames = runs.toArray()
        Arrays.sort(runNames)

        [scan: scan, tracks: tracks, runs: runNames]
    }
}
