package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

import java.time.*
import java.util.zip.*

@Ignore
class FastqcWorkflowTests extends WorkflowTestCase {

    ProcessingOptionService processingOptionService
    LsdfFilesService lsdfFilesService
    FastqcDataFilesService fastqcDataFilesService


    File sourceFastq
    File expectedFastqc
    DataFile dataFile
    SeqCenter seqCenter
    SeqTrack seqTrack


    void setUpWorkFlow(String fileExtension) {
        sourceFastq = new File(inputRootDirectory, "fastqFiles/fastqc/input_fastqc.fastq.${fileExtension}")
        expectedFastqc = new File(inputRootDirectory, "fastqFiles/fastqc/asdf_fastqc.zip")

        Project project = Project.build(
                realm: realm
        )

        Individual individual = Individual.build(
                project: project,
                type: Individual.Type.REAL,
        )

        Sample sample = Sample.build(individual: individual)

        Run run = DomainFactory.createRun()

        seqCenter = run.seqCenter

        seqTrack = DomainFactory.createSeqTrack(
                sample: sample,
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                seqType: SeqTypeService.wholeGenomePairedSeqType,
                run: run,
        )

        dataFile = DomainFactory.createSequenceDataFile(
                fileExists: true,
                fileSize: 1,
                fileName: "asdf.fastq.${fileExtension}",
                vbpFileName: "asdf.fastq.${fileExtension}",
                seqTrack: seqTrack,
                run: run,
                initialDirectory: "${ftpDir}/${run.name}",
        )

        linkFileUtils.createAndValidateLinks([(sourceFastq): new File(lsdfFilesService.getFileFinalPath(dataFile))], realm)
    }


    @Test
    void testWorkflow_FastQcDataAvailable() {
        setUpWorkFlow('gz')
        String initialPath = new File(lsdfFilesService.getFileInitialPath(dataFile)).parent
        String fastqcFileName = fastqcDataFilesService.fastqcFileName(dataFile)

        linkFileUtils.createAndValidateLinks([(expectedFastqc): new File("${initialPath}/${fastqcFileName}")], realm)

        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Test
    void testWorkflow_FastQcDataNotAvailable() {
        setUpWorkFlow('gz')
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Test
    void testWorkflow_FastQcDataNotAvailable_bzip2() {
        setUpWorkFlow('bz2')
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Test
    void testWorkflow_FastQcDataNotAvailable_tar_bzip2() {
        setUpWorkFlow('tar.bz2')
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    private validateFastQcFileContent() {
        dataFile.refresh()
        assert null != dataFile.sequenceLength
        assert null != dataFile.nReads
        seqTrack.refresh()
        assert seqTrack.nBasePairs
    }

    private void checkExistenceOfResultsFiles() {
        ZipFile expectedResult = new ZipFile(expectedFastqc)
        ZipFile actualResult = new ZipFile(fastqcDataFilesService.fastqcOutputFile(dataFile))

        List<String> actualFiles = []
        actualResult.entries().each { ZipEntry entry ->
            actualFiles.add(entry.name)
        }

        expectedResult.entries().each { ZipEntry entry ->
            assert actualFiles.contains(entry.name)
            actualFiles.remove(entry.name)
        }
        assert actualFiles.isEmpty()
    }

    private validateFastqcProcessedFile() {
        FastqcProcessedFile fastqcProcessedFile = CollectionUtils.exactlyOneElement(FastqcProcessedFile.all)

        assert FastqcProcessedFile.all.size() == 1
        assert fastqcProcessedFile.fileExists
        assert fastqcProcessedFile.contentUploaded
        assert fastqcProcessedFile.dataFile == dataFile
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/FastqcWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        Duration.ofMinutes(20)
    }
}
