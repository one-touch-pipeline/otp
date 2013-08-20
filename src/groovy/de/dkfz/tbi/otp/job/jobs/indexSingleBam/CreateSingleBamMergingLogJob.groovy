package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*

//This Job is not used anymore in the workflow "IndexSingleBamWorkflow".
//It will be kept because of historical reasons and perhaps can be reused later.
@Deprecated
class CreateSingleBamMergingLogJob extends AbstractEndStateAwareJobImpl {

    SeqScan scan = null

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        assertNoMergingLog()
        assertSingleSeqTrack()
        AlignmentParams alignParams = getAlignParams()
        MergingLog mergingLog = new MergingLog(
            qcState: MergingLog.QCState.NON,
            executedBy: MergingLog.Execution.SYSTEM,
            status: MergingLog.Status.PROCESSING,
            seqScan: scan,
            alignmentParams: alignParams,
        )
        if (mergingLog.validate()) {
            mergingLog.save(flush:true)
            succeed()
        } else {
            log.debug mergingLog.errors
            fail()
        }
    }

    private void assertNoMergingLog() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        if (mergingLog) {
            fail()
        }
    }

    private void assertSingleSeqTrack() {
        int n = MergingAssignment.countBySeqScan(scan)
        if (n != 1) {
            fail()
        }
    }

    private AlignmentParams getAlignParams() {
        SeqTrack seqTrack = MergingAssignment.findBySeqScan(scan).seqTrack
        int nLog = AlignmentLog.countBySeqTrack(seqTrack)
        if (nLog != 1) {
            fail()
        }
        return AlignmentLog.findBySeqTrack(seqTrack).alignmentParams
    }
}
