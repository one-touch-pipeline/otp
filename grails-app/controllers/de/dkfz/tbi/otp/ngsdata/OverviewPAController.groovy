package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.WorkPackagesDSL

class OverviewPAController {

    def index() {
        Project project = Project.findByName("astrocytome")
        String pattern = "PA%"
        List<Individual> inds = Individual.findAllByProjectAndMockPidLike(project, pattern)

        //println project
        //println inds

        List<SeqType> wp = WorkPackagesDSL.make {
            seqType("WHOLE_GENOME", SeqType.LIBRARYLAYOUT_PAIRED)
            seqType("WHOLE_GENOME", SeqType.LIBRARYLAYOUT_MATE_PAIR)
            seqType("RNA", SeqType.LIBRARYLAYOUT_PAIRED)
            seqType("MI_RNA", SeqType.LIBRARYLAYOUT_SINGLE)
        }

        List table = new ArrayList()
        for(Individual ind in inds) {
            List person = new ArrayList()
            person << ind.mockPid
            Sample sample = Sample.findByIndividualAndType(ind, Sample.Type.TUMOR)
            for(SeqType type in wp) {
                SeqScan scan = SeqScan.findBySampleAndSeqType(sample, type)
                person << statusSeqScan(scan)
            }
            table << person
        }
        [table: table, scans: wp]
    }

    private String statusSeqScan(SeqScan scan) {
        String str = ""
        if (!scan) {
            return ""
        }
        for(int i=0; i<scan.nLanes; i++) {
            str += "o"
        }
        return str
    }
}
