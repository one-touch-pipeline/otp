package workflows

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LinkFileUtils
import grails.plugin.springsecurity.SpringSecurityUtils
import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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

        DomainFactory.createAlignableSeqTypes()

        Run run = DomainFactory.createRun()

        RunSegment runSegment = DomainFactory.createRunSegment(
                run: run,
                dataPath: stagingRootPath,
        )

        seqCenter = run.seqCenter

        SeqTrack seqTrack = SeqTrack.build(
                sample: sample,
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                seqType: SeqType.wholeGenomePairedSeqType,
                run: run,
        )

        dataFile = DomainFactory.buildSequenceDataFile(
                fileExists: true,
                fileSize: 1,
                fileName: 'asdf.fastq.gz',
                vbpFileName: 'asdf.fastq.gz',
                seqTrack: seqTrack,
                run: run,
                runSegment: runSegment,
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
