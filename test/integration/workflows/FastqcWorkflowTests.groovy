package workflows

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LinkFileUtils
import grails.plugin.springsecurity.SpringSecurityUtils
import org.joda.time.Duration
import org.junit.After
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

        [
                "BASIC_STATISTICS": "Basic Statistics",
                "PER_BASE_SEQUENCE_QUALITY": "Per base sequence quality",
                "PER_SEQUENCE_QUALITY_SCORES": "Per sequence quality scores",
                "PER_BASE_SEQUENCE_CONTENT": "Per base sequence content",
                "PER_BASE_GC_CONTENT": "Per base GC content",
                "PER_SEQUENCE_GC_CONTENT": "Per sequence GC content",
                "PER_BASE_N_CONTENT": "Per base N content",
                "SEQUENCE_LENGTH_DISTRIBUTION": "Sequence Length Distribution",
                "SEQUENCE_DUPLICATION_LEVELS": "Sequence Duplication Levels",
                "OVERREPRESENTED_SEQUENCES": "Overrepresented sequences",
                "KMER_CONTENT": "Kmer Content",
        ].each { k, v ->
            assert new FastqcModule(identifier: k, name: v).save(flush: true, failOnError: true)
        }
    }

    @After
    void tearDown() {
        seqCenter = null
        dataFile = null
    }

    @Test
    void testWorkflow_FastQcDataAvailable() {
        String initialPath = new File(lsdfFilesService.getFileInitialPath(dataFile)).parent
        String fastqcFileName = fastqcDataFilesService.fastqcFileName(dataFile)

        linkFileUtils.createAndValidateLinks([(expectedFastqc): new File("${initialPath}/${fastqcFileName}")], realm)

        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
    }


    @Test
    void testWorkflow_FastQcDataNotAvailable() {
        execute()

        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()

        checkModuleStatus("BASIC_STATISTICS", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("PER_BASE_SEQUENCE_QUALITY", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("PER_SEQUENCE_QUALITY_SCORES", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("PER_BASE_SEQUENCE_CONTENT", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("PER_BASE_GC_CONTENT", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("PER_SEQUENCE_GC_CONTENT", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("PER_BASE_N_CONTENT", FastqcModuleStatus.Status.WARN)
        checkModuleStatus("SEQUENCE_LENGTH_DISTRIBUTION", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("SEQUENCE_DUPLICATION_LEVELS", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("OVERREPRESENTED_SEQUENCES", FastqcModuleStatus.Status.PASS)
        checkModuleStatus("KMER_CONTENT", FastqcModuleStatus.Status.WARN)

        assert FastqcPerBaseSequenceAnalysis.all.size() == 101
        assert FastqcKmerContent.all.size() == 71
        assert FastqcOverrepresentedSequences.all.size() == 0
        assert FastqcSequenceDuplicationLevels.all.size() == 10
        assert FastqcPerSequenceGCContent.all.size() == 101
        assert FastqcPerSequenceQualityScores.all.size() == 33
        assert FastqcBasicStatistics.all.size() == 1
        assert FastqcSequenceLengthDistribution.all.size() == 1
    }

    private void checkModuleStatus(String moduleName, FastqcModuleStatus.Status status) {
        assert CollectionUtils.exactlyOneElement(FastqcModuleStatus.findAllByModule(FastqcModule.findByIdentifier(moduleName))).status == status
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
