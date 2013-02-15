package de.dkfz.tbi.otp.ngsdata

class SeqTrackDataProviderController {
    def fetchInsertSize(String runName, String laneId) {
        Run run = Run.findByName(runName)
        SeqTrack seqTrack = SeqTrack.findByRunAndLaneId(run, laneId)
        render seqTrack.insertSize
    }
}
