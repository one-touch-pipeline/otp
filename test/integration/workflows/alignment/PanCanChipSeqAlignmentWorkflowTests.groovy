package workflows.alignment

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Ignore
class PanCanChipSeqAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    String getRefGenFileNamePrefix() {
        return 'hg_GRCh38'
    }

    protected String getChromosomeStatFileName() {
        return 'hg_GRCh38.fa.chrLenOnlyACGT.tab'
    }

    @Override
    protected String getReferenceGenomeSpecificPath() {
        'bwa_hg38'
    }


    @Override
    SeqType findSeqType() {
        return exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.CHIP_SEQ.seqTypeName,
                libraryLayout: LibraryLayout.PAIRED,
        ))
    }

    @Override
    void setUpFilesVariables() {
        File baseTestDataDirChipSeq = new File(inputRootDirectory, 'fastqFiles/wgs')
        testFastqFiles = [
                readGroup1: [
                        new File(baseTestDataDirChipSeq, 'normal/paired/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2'),
                        new File(baseTestDataDirChipSeq, 'normal/paired/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2'),
                ].asImmutable(),
                readGroup2: [
                        new File(baseTestDataDirChipSeq, 'normal/paired/run2/sequence/gerald_D1VCPACXX_7_R1.fastq.bz2'),
                        new File(baseTestDataDirChipSeq, 'normal/paired/run2/sequence/gerald_D1VCPACXX_7_R2.fastq.bz2'),
                ].asImmutable(),
        ].asImmutable()
        refGenDir = new File(referenceGenomeDirectory, 'bwa_hg38')
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_SameAntibody_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_DifferentAntibody_allFine() {

        SeqTrack firstSeqTrack = createSeqTrack("readGroup1")

        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
        MergingWorkPackage secondMergingWorkPackage = DomainFactory.createMergingWorkPackage([
                sample               : workPackage.sample,
                pipeline             : workPackage.pipeline,
                seqType              : workPackage.seqType,
                seqPlatformGroup     : workPackage.seqPlatformGroup,
                referenceGenome      : workPackage.referenceGenome,
                needsProcessing      : false,
                statSizeFileName     : workPackage.statSizeFileName,
                libraryPreparationKit: workPackage.libraryPreparationKit,
                antibodyTarget       : DomainFactory.createAntibodyTarget()
        ])

        SeqTrack secondSeqTrack = DomainFactory.createSeqTrackWithDataFiles(secondMergingWorkPackage, [
                laneId               : "readGroup2",
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                libraryPreparationKit: exactlyOneElement(LibraryPreparationKit.findAll()),
                kitInfoReliability   : InformationReliability.KNOWN,
        ])

        DataFile.findAllBySeqTrack(secondSeqTrack).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${workPackage.individual.pid}_${workPackage.sampleType.name}_${secondSeqTrack.laneId}_${index + 1}.fastq.gz"
            dataFile.nReads = AbstractRoddyAlignmentWorkflowTests.NUMBER_OF_READS
            dataFile.save(flush: true)
        }
        linkFastqFiles(secondSeqTrack, testFastqFiles.get(secondSeqTrack.laneId))

        secondMergingWorkPackage.needsProcessing = true
        secondMergingWorkPackage.save(flush: true)


        // run
        execute(2)

        // check
        workPackage.refresh()
        assert false == workPackage.needsProcessing
        secondMergingWorkPackage.refresh()
        assert false == secondMergingWorkPackage.needsProcessing

        List<RoddyBamFile> bamFiles = RoddyBamFile.findAll()
        assert 2 == bamFiles.size()
        checkLatestBamFileState(bamFiles[0], null, [mergingWorkPackage: workPackage, seqTracks: [firstSeqTrack], containedSeqTracks: [firstSeqTrack], identifier: 0L,])
        checkLatestBamFileState(bamFiles[1], null, [mergingWorkPackage: secondMergingWorkPackage, seqTracks: [secondSeqTrack], containedSeqTracks: [secondSeqTrack], identifier: 0L,])

        bamFiles.each { RoddyBamFile bamFile ->
            assertBamFileFileSystemPropertiesSet(bamFile)

            checkFileSystemState(bamFile)

            checkQC(bamFile)
        }
    }
}
