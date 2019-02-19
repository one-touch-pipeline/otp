package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.junit.*
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class ExecuteWgbsAlignmentJobTests {

    @Autowired
    ExecuteWgbsAlignmentJob executeWgbsAlignmentJob

    LsdfFilesService lsdfFilesService

    RoddyBamFile roddyBamFile

    TestConfigService configService

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File cpiFile

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        DomainFactory.changeSeqType(roddyBamFile, wgbsSeqType)
        roddyBamFile.workPackage.metaClass.seqTracks = SeqTrack.list()

        roddyBamFile.referenceGenome.cytosinePositionsIndex = "cytosine_idx.pos.gz"
        roddyBamFile.referenceGenome.save(flush: true, failOnError: true)
        File referenceGenomeDirectory = new File("${tmpDir.root}/processing/reference_genomes")
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDirectory.path)
        cpiFile = CreateFileHelper.createFile(
                new File("${referenceGenomeDirectory}/${roddyBamFile.referenceGenome.path}",
                        roddyBamFile.referenceGenome.cytosinePositionsIndex)
        )

        executeWgbsAlignmentJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm realm, String cmd ->
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(cmd)
            return new ProcessOutput(
                    stdout: "stdout",
                    stderr: "",
                    exitCode: 0,
            )
        }

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : tmpDir.root.path,
                (OtpProperty.PATH_PROCESSING_ROOT): tmpDir.root.path,
        ])

        prepareDataFilesOnFileSystem(roddyBamFile)

        CreateFileHelper.createFile(executeWgbsAlignmentJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome, false))
        CreateFileHelper.createFile(executeWgbsAlignmentJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false))
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(RemoteShellHelper, executeWgbsAlignmentJob.remoteShellHelper)
        configService.clean()
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_InputBamIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(null)
        }.contains("roddyBamFile must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_ChromosomeNamesCanNotBeFound_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)
        }.contains("No chromosome names could be found for reference genome")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_cytosinePositionIndexFilePath() {
        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        List<String> expectedCommand = [
                "INDEX_PREFIX:${executeWgbsAlignmentJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "GENOME_FA:${executeWgbsAlignmentJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "CHROM_SIZES_FILE:${executeWgbsAlignmentJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage).absolutePath}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:",
                "runFingerprinting:false",
                "CHROMOSOME_INDICES:( ${chromosomeNames.join(' ')} )",
                "CYTOSINE_POSITIONS_INDEX:${cpiFile.absolutePath}",
        ]

        List<String> actualCommand = executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        assert expectedCommand == actualCommand
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_CytosinePositionIndexNotDefined_ShouldThrowException() {
        roddyBamFile.referenceGenome.cytosinePositionsIndex = null
        roddyBamFile.referenceGenome.save(flush: true)

        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        assert TestCase.shouldFail(RuntimeException) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)
        }.contains("Cytosine position index for reference genome")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_InputBamIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(null)
        }.contains("roddyBamFile must not be null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_ParentDirDoesNotExist_ShouldFail() {

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)
        }.contains(roddyBamFile.workDirectory.path)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_ExitCodeNotNull_ShouldFail() {
        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.mkdirs()

        executeWgbsAlignmentJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm realm, String cmd ->
            return new ProcessOutput(
                    stdout: "stdout",
                    stderr: "",
                    exitCode: 3,
            )
        }

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)
        }.contains("output.exitCode == 0")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_FileNotAvailable_ShouldFail() {
        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.mkdirs()

        executeWgbsAlignmentJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm realm, String cmd ->
            return new ProcessOutput(
                    stdout: "stdout",
                    stderr: "",
                    exitCode: 0,
            )
        }

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)
        }.contains(roddyBamFile.workMetadataTableFile.path)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_LibraryStoredInSeqTrack() {
        testPrepareAndReturnWorkflowSpecificParameter("lib1")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_NoLibraryStoredInSeqTrack() {
        testPrepareAndReturnWorkflowSpecificParameter()
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_NoLibraryStoredInSeqTrackAndFileAlreadyExist() {
        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.mkdirs()
        metaDataTableFile.text = 'DUMMY TEXT'

        testPrepareAndReturnWorkflowSpecificParameter()

        assert metaDataTableFile.text != 'DUMMY TEXT'
    }

    void testPrepareAndReturnWorkflowSpecificParameter(String libraryName = null) {
        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            seqTrack.libraryName = libraryName
            seqTrack.normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            assert seqTrack.save(flush: true)
        }

        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.exists() || metaDataTableFile.parentFile.mkdirs()

        String expectedCommand = "--usemetadatatable=${roddyBamFile.workMetadataTableFile.path}"
        assert expectedCommand == executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)

        assert metaDataTableFile.exists()
        List<String> lines = metaDataTableFile.readLines()

        assert lines.size() == 3
        assert lines.get(0).trim() == executeWgbsAlignmentJob.HEADER.trim()


        DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks as List).each {
            String oneLine = "${it.sampleType.dirName}\t${it.seqTrack.getLibraryDirectoryName()}\t${it.individual.pid}\t${it.seqType.libraryLayoutDirName}\t${it.run.dirName}\t${it.mateNumber}\t${lsdfFilesService.getFileViewByPidPath(it)}"

            assert lines.contains(oneLine)
        }
    }


    @Test
    void testWorkflowSpecificValidation_MethylationMergedDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        File methylationMergedDir = new File(roddyBamFile.getWorkMethylationDirectory(), "merged")
        methylationMergedDir.deleteDir()

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)
        }.contains(methylationMergedDir.path)
    }


    @Test
    void testWorkflowSpecificValidation_MethylationLibraryDirDoesNotExist_ShouldFail() {
        roddyBamFile.seqTracks.add(DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.workPackage, [libraryName: "lib1", normalizedLibraryName: "1"]))
        roddyBamFile.numberOfMergedLanes = 2
        MergingCriteria.list()*.useLibPrepKit = false
        roddyBamFile.save(flush: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        File methylationLibraryDir = new File(roddyBamFile.getWorkMethylationDirectory(), "libNA")
        methylationLibraryDir.deleteDir()

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)
        }.contains(methylationLibraryDir.path)
    }


    @Test
    void testWorkflowSpecificValidation_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)
    }

    private void prepareDataFilesOnFileSystem(RoddyBamFile bamFile) {
        assert 1 == bamFile.seqTracks.size()
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack, [sort: 'mateNumber'])
        assert 2 == dataFiles.size()
        File dataFile1File = new File(executeWgbsAlignmentJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0]))
        createFileAndAddFileSize(dataFile1File, dataFiles[0])
        File dataFile2File = new File(executeWgbsAlignmentJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1]))
        createFileAndAddFileSize(dataFile2File, dataFiles[1])
    }


    private void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }
}
