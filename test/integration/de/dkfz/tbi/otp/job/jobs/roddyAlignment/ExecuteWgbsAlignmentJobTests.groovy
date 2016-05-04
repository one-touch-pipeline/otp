package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired


class ExecuteWgbsAlignmentJobTests {

    @Autowired
    ExecuteWgbsAlignmentJob executeWgbsAlignmentJob

    LsdfFilesService lsdfFilesService

    RoddyBamFile roddyBamFile


    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()


    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        DomainFactory.changeSeqType(roddyBamFile, wgbsSeqType)
        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }

        executeWgbsAlignmentJob.executionService.metaClass.executeCommandReturnProcessOutput = { Realm realm, String cmd ->
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(cmd)
            return new ProcessOutput(
                    stdout: "stdout",
                    stderr: "",
                    exitCode: 0,
            )
        }

        DomainFactory.createRealmDataProcessing(tmpDir.root, [name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataManagement(tmpDir.root, [name: roddyBamFile.project.realmName])
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutionService, executeWgbsAlignmentJob.executionService)
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
    void testPrepareAndReturnWorkflowSpecificCValues() {
        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "M", "X", "Y"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        assert ",CHROMOSOME_INDICES:( ${chromosomeNames.join(' ')} )" == executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)
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

        executeWgbsAlignmentJob.executionService.metaClass.executeCommandReturnProcessOutput = { Realm realm, String cmd ->
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

        executeWgbsAlignmentJob.executionService.metaClass.executeCommandReturnProcessOutput = { Realm realm, String cmd ->
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


    void testPrepareAndReturnWorkflowSpecificParameter(String libraryName = null) {
        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            seqTrack.libraryName = libraryName
            seqTrack.normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            assert seqTrack.save(flush: true)
        }

        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.mkdirs()

        String expectedCommand = "--usemetadatatable=${roddyBamFile.workMetadataTableFile.path} "
        assert expectedCommand == executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)

        assert metaDataTableFile.exists()
        List<String> lines = metaDataTableFile.readLines()

        assert lines.size() == 3
        assert lines.get(0).trim() == executeWgbsAlignmentJob.HEADER.trim()


        DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks).each {
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
}
