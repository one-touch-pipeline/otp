package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_OPERATOR'])
class OverviewMBController {

    def index() {

        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        Date date = cal.getTime()

        List stringCenters = []


        List<SeqCenter> centers = SeqCenter.findAll()
        for (SeqCenter center in centers) {
            List<String> text = []
            int nRuns = Run.countBySeqCenter(center)
            int nNew = Run.countBySeqCenterAndDateExecutedGreaterThan(center, date)
            text << center
            text << nRuns
            text << nNew
            stringCenters << text
        }

        List<SeqType> wp = WorkPackagesDSL.make {
            seqType("WHOLE_GENOME", LibraryLayout.PAIRED)
            seqType("EXON", LibraryLayout.PAIRED)
            seqType("WHOLE_GENOME_BISULFITE", LibraryLayout.PAIRED)
            seqType("WHOLE_GENOME", LibraryLayout.MATE_PAIR)
            seqType("RNA", LibraryLayout.PAIRED)
            seqType("MI_RNA", LibraryLayout.SINGLE)
        }

        List stringTypes = []
        for (SeqType type in wp) {
            List<String> text = []
            int nTracks = SeqTrack.findAllBySeqType(type).findAll { !it.isWithdrawn() }.size()
            int nTracksNew = countNewSeqTracks(type)
            text << type
            text << nTracks
            text << nTracksNew
            stringTypes << text
        }
        [centers: stringCenters, types: stringTypes]
    }

    private int countNewSeqTracks(SeqType type) {
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        Date date = cal.getTime()

        int N = 0;
        List<SeqTrack> tracks = SeqTrack.findAllBySeqType(type).findAll { !it.isWithdrawn() }
        for (SeqTrack track in tracks) {
            if (track.run.dateExecuted > date) {
                N++
            }
        }
        return N
    }
}
