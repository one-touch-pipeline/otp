package de.dkfz.tbi.otp.ngsdata

class SeqScanController {

    def scaffold = SeqScan

    //def index() { 

    def show = {

        SeqScan scan = SeqScan.get(params.id)
        Set<String> runs = new HashSet<String>()

        SeqTrack.findAllBySeqScan(scan).each {seqTrack ->
            runs.add(seqTrack.run.name)
        }

        String[] runNames = runs.toArray()
        Arrays.sort(runNames)

        [scan : scan, runs: runNames]
    }
}
