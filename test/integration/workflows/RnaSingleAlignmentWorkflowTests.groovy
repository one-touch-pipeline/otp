package workflows

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

        Map seqTrack2Properties = [
                laneId               : "readGroup2",
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
        ] + properties

        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithDataFiles(workPackage, seqTrack1Properties)
        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithDataFiles(workPackage, seqTrack2Properties)

        DataFile.findAllBySeqTrack(seqTrack1).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack1.individual.pid}_${seqTrack1.sampleType.name}_${seqTrack1.laneId}_${index + 1}.fastq.gz"
            dataFile.nReads = AbstractRoddyAlignmentWorkflowTests.NUMBER_OF_READS
            dataFile.save(flush: true)
        }

        DataFile.findAllBySeqTrack(seqTrack2).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack2.individual.pid}_${seqTrack2.sampleType.name}_${seqTrack2.laneId}_${index + 2}.fastq.gz"
            dataFile.nReads = AbstractRoddyAlignmentWorkflowTests.NUMBER_OF_READS
            dataFile.save(flush: true)
        }
        println "seqTrack1"
        println seqTrack1.readGroupName
        println DataFile.findAllBySeqTrack(seqTrack1)*.vbpFileName
        linkFastqFiles(seqTrack1, readGroupNum)

        println "seqTrack2"
        println seqTrack2.readGroupName
        println DataFile.findAllBySeqTrack(seqTrack2)*.vbpFileName
        linkFastqFiles(seqTrack2, "readGroup2")



        workPackage.needsProcessing = true
        workPackage.save(flush: true)
        return seqTrack1
    }
}
