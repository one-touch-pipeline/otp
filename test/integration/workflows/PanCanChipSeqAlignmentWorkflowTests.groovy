package workflows

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Ignore
class PanCanChipSeqAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    public String getRefGenFileNamePrefix() {
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
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        ))
    }

    @Override
    void setUpFilesVariables() {
        File baseTestDataDirChipSeq = new File(rootDirectory, 'ChipSeqTestFiles/view-by-pid')
        testFastqFiles = [
                readGroup1: [
                        new File(baseTestDataDirChipSeq, 'pid/replicate1-Input/paired/run1/sequence/L001_R1.fastq.gz'),
                        new File(baseTestDataDirChipSeq, 'pid/replicate1-Input/paired/run1/sequence/L001_R2.fastq.gz'),
                ].asImmutable(),
                readGroup2: [
                        new File(baseTestDataDirChipSeq, 'pid/replicate1-Input/paired/run1/sequence/L002_R1.fastq.gz'),
                        new File(baseTestDataDirChipSeq, 'pid/replicate1-Input/paired/run1/sequence/L002_R2.fastq.gz'),
                ].asImmutable(),
        ].asImmutable()
        baseTestDataDir = new File(rootDirectory, 'workflow-data')
        refGenDir = new File(baseTestDataDir, 'reference-genomes/bwa_hg38')
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes()
    }
}
