package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_OPERATOR'])
class OverviewMBController {

    def index() {

        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        Date date = cal.getTime()

        List stringCenters = new ArrayList()


        List<SeqCenter> centers = SeqCenter.findAll()
        for(SeqCenter center in centers) {
            List<String> text = new ArrayList<String>()
            int nRuns = Run.countBySeqCenter(center)
            int nNew = Run.countBySeqCenterAndDateExecutedGreaterThan(center, date)
            text << center
            text << nRuns
            text << nNew
            stringCenters << text
        }

        List<SeqType> wp = WorkPackagesDSL.make {
            seqType("WHOLE_GENOME", "PAIRED")
            seqType("EXON", "PAIRED")
            seqType("WHOLE_GENOME_BISULFITE", "PAIRED")
            seqType("WHOLE_GENOME", "MATE_PAIR")
            seqType("RNA", "PAIRED")
            seqType("MI_RNA", "SINGLE")
        }

        List stringTypes = new ArrayList()
        for(SeqType type in wp) {
            List<String> text = new ArrayList<String>()
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
        for(SeqTrack track in tracks) {
            if (track.run.dateExecuted > date) {
                N++
            }
        }
        return N
    }
}
