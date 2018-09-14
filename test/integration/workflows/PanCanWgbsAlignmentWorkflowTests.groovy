package workflows

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement


@Ignore
class PanCanWgbsAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    public String getRefGenFileNamePrefix() {
        return 'hs37d5_PhiX_Lambda.conv'
    }

    protected String getChromosomeStatFileName() {
        return 'hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab'
    }

    protected String getCytosinePositionsIndex() {
        return 'hs37d5_PhiX_Lambda.pos.gz'
    }

    @Override
    SeqType findSeqType() {
        return exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        ))
    }

    @Override
    void setUpFilesVariables() {
        File baseTestDataDirWGBS = new File(rootDirectory, 'WgbsAlignmentSetupFiles/view-by-pid')
        testFastqFiles = [
                readGroup1: [
                        new File(baseTestDataDirWGBS, 'methylationpid/blood/lib1/paired/run1/sequence/id3_R1.fastq.gz'),
                        new File(baseTestDataDirWGBS, 'methylationpid/blood/lib1/paired/run1/sequence/id3_R2.fastq.gz'),
                ].asImmutable(),
                readGroup2: [
                        new File(baseTestDataDirWGBS, 'methylationpid/blood/lib5/paired/run1/sequence/id1_R1.fastq.gz'),
                        new File(baseTestDataDirWGBS, 'methylationpid/blood/lib5/paired/run1/sequence/id1_R2.fastq.gz'),
                ].asImmutable(),
        ].asImmutable()
        baseTestDataDir = new File(rootDirectory, 'WgbsAlignmentSetupFiles')
        refGenDir = new File(baseTestDataDir, 'reference-genomes/bwa06_methylCtools_hs37d5_PhiX_Lambda')
        chromosomeNamesFile = new File(baseTestDataDir, 'reference-genomes/chromosome-names.txt')
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/WgbsAlignmentWorkflow.groovy",
        ]
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLibraries_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes(true)
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes(false)
    }
}
