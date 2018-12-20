package workflows.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Ignore
class RnaSingleAlignmentWorkflowTests extends AbstractRnaAlignmentWorkflowTests {

    @Override
    SeqType findSeqType() {
        DomainFactory.createRnaSingleSeqType()
    }

    @Override
    SeqTrack createSeqTrack(String readGroupNum, Map properties = [:]) {
        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())

        Map seqTrack1Properties = [
                laneId               : readGroupNum,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
        ] + properties

        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithDataFiles(workPackage, seqTrack1Properties)

        DataFile.findAllBySeqTrack(seqTrack1).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack1.individual.pid}_${seqTrack1.sampleType.name}_${seqTrack1.laneId}_${index + 1}.fastq.gz"
            dataFile.nReads = AbstractRoddyAlignmentWorkflowTests.NUMBER_OF_READS
            dataFile.save(flush: true)
        }

        linkFastqFiles(seqTrack1, testFastqFiles.get(readGroupNum))

        workPackage.needsProcessing = true
        workPackage.save(flush: true)
        return seqTrack1
    }
}
