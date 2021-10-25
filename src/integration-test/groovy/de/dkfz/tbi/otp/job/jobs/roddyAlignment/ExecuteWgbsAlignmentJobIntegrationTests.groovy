/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.*

@Rollback
@Integration
class ExecuteWgbsAlignmentJobIntegrationTests {

    LsdfFilesService lsdfFilesService

    RoddyBamFile roddyBamFile

    TestConfigService configService
    RemoteShellHelper remoteShellHelper
    ReferenceGenomeService referenceGenomeService
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File cpiFile

    void setupData(ExecuteWgbsAlignmentJob executeWgbsAlignmentJob) {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        DomainFactory.changeSeqType(roddyBamFile, wgbsSeqType)
        roddyBamFile.workPackage.metaClass.seqTracks = SeqTrack.list()

        roddyBamFile.referenceGenome.cytosinePositionsIndex = "cytosine_idx.pos.gz"
        roddyBamFile.referenceGenome.save(flush: true)
        File referenceGenomeDirectory = new File("${tmpDir.root}/processing/reference_genomes")
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDirectory.path)
        cpiFile = CreateFileHelper.createFile(
                new File("${referenceGenomeDirectory}/${roddyBamFile.referenceGenome.path}",
                        roddyBamFile.referenceGenome.cytosinePositionsIndex)
        )

        executeWgbsAlignmentJob.remoteShellHelper = remoteShellHelper
        executeWgbsAlignmentJob.lsdfFilesService = lsdfFilesService
        executeWgbsAlignmentJob.referenceGenomeService = referenceGenomeService
        executeWgbsAlignmentJob.chromosomeIdentifierSortingService = chromosomeIdentifierSortingService

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


        // prepareDataFilesOnFileSystem
        assert 1 == roddyBamFile.seqTracks.size()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator().next()
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack, [sort: 'mateNumber'])
        assert 2 == dataFiles.size()
        File dataFile1File = new File(executeWgbsAlignmentJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0]))
        createFileAndAddFileSize(dataFile1File, dataFiles[0])
        File dataFile2File = new File(executeWgbsAlignmentJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1]))
        createFileAndAddFileSize(dataFile2File, dataFiles[1])

        CreateFileHelper.createFile(executeWgbsAlignmentJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome, false))
        CreateFileHelper.createFile(executeWgbsAlignmentJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false))
    }

    @After
    void tearDown() {
        configService.clean()
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_InputBamIsNull_ShouldFail() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(null)
        }.contains("roddyBamFile must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_ChromosomeNamesCanNotBeFound_ShouldFail() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)
        }.contains("No chromosome names could be found for reference genome")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_cytosinePositionIndexFilePath() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
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
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
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
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(null)
        }.contains("roddyBamFile must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_ParentDirDoesNotExist_ShouldFail() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)
        }.contains(roddyBamFile.workDirectory.path)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_ExitCodeNotNull_ShouldFail() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
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
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
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
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        testPrepareAndReturnWorkflowSpecificParameter("lib1", executeWgbsAlignmentJob)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_NoLibraryStoredInSeqTrack() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        testPrepareAndReturnWorkflowSpecificParameter(executeWgbsAlignmentJob)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_NoLibraryStoredInSeqTrackAndFileAlreadyExist() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.mkdirs()
        metaDataTableFile.text = 'DUMMY TEXT'

        testPrepareAndReturnWorkflowSpecificParameter(executeWgbsAlignmentJob)

        assert metaDataTableFile.text != 'DUMMY TEXT'
    }

    void testPrepareAndReturnWorkflowSpecificParameter(String libraryName = null, ExecuteWgbsAlignmentJob executeWgbsAlignmentJob) {
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
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        File methylationMergedDir = new File(roddyBamFile.getWorkMethylationDirectory(), "merged")
        methylationMergedDir.deleteDir()

        assert TestCase.shouldFail(AssertionError) {
            executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)
        }.contains(methylationMergedDir.path)
    }

    @Test
    void testWorkflowSpecificValidation_MethylationLibraryDirDoesNotExist_ShouldFail() {
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
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
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)
    }


    private void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }
}
