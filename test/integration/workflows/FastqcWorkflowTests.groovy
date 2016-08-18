package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import org.junit.*

import java.util.zip.*
@Ignore
class FastqcWorkflowTests extends WorkflowTestCase {
    ProcessingOptionService processingOptionService
    LsdfFilesService lsdfFilesService
    FastqcDataFilesService fastqcDataFilesService
    LinkFileUtils linkFileUtils


    File sourceFastq
    File expectedFastqc
    DataFile dataFile
    SeqCenter seqCenter
    SeqTrack seqTrack

    @Before
    void setUp() {
        sourceFastq = new File(testDataDir, "35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz")
        expectedFastqc = new File(testDataDir, "expected_result_fastqc.zip")

        Project project = Project.build(
                realmName: realm.name
        )

        Individual individual = Individual.build(
                project: project,
                type: Individual.Type.REAL,
        )

        Sample sample = Sample.build(individual: individual)

        Run run = DomainFactory.createRun()

        RunSegment runSegment = DomainFactory.createRunSegment(
                run: run,
        )

        seqCenter = run.seqCenter

        seqTrack = SeqTrack.build(
                sample: sample,
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                seqType: SeqType.wholeGenomePairedSeqType,
                run: run,
        )

        dataFile = DomainFactory.createSequenceDataFile(
                fileExists: true,
                fileSize: 1,
                fileName: 'asdf.fastq.gz',
                vbpFileName: 'asdf.fastq.gz',
                seqTrack: seqTrack,
                run: run,
                runSegment: runSegment,
                initialDirectory: "${stagingRootPath}/${run.name}",
        )

        linkFileUtils.createAndValidateLinks([(sourceFastq): new File(lsdfFilesService.getFileFinalPath(dataFile))], realm)

        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate('basesPerBytesFastQ', null, null, "1", "description")
        }
    }


    @Test
    void testWorkflow_FastQcDataAvailable() {
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

    private void checkExistenceOfResultsFiles(){
        ZipFile expectedResult = new ZipFile(expectedFastqc)
        ZipFile actualResult = new ZipFile(fastqcDataFilesService.fastqcOutputFile(dataFile))

        LinkedHashMap actualFiles = [:]
        actualResult.entries().each { ZipEntry entry ->
            actualFiles.put(entry.name, entry.size)
        }

        expectedResult.entries().each { ZipEntry entry ->
            assert actualFiles[entry.name] == entry.size
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
        ["scripts/workflows/FastqcWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(20)
    }
}
