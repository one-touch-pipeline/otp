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
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

@Rollback
@Integration
class ExecuteWgbsAlignmentJobIntegrationSpec extends Specification {

    LsdfFilesService lsdfFilesService

    RoddyBamFile roddyBamFile

    TestConfigService configService
    RemoteShellHelper remoteShellHelper
    ReferenceGenomeService referenceGenomeService
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService

    @TempDir
    Path tempDir

    File cpiFile

    void setupData(ExecuteWgbsAlignmentJob executeWgbsAlignmentJob) {
        configService.addOtpProperties(tempDir)

        roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        DomainFactory.changeSeqType(roddyBamFile, wgbsSeqType)
        roddyBamFile.workPackage.metaClass.seqTracks = SeqTrack.list()

        roddyBamFile.referenceGenome.cytosinePositionsIndex = "cytosine_idx.pos.gz"
        roddyBamFile.referenceGenome.save(flush: true)
        File referenceGenomeDirectory = new File("${tempDir}/processing/reference_genomes")
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

    void cleanup() {
        configService.clean()
    }

    void "testPrepareAndReturnWorkflowSpecificCValues_InputBamIsNull_ShouldFail"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("roddyBamFile must not be null")
    }

    void "testPrepareAndReturnWorkflowSpecificCValues_ChromosomeNamesCanNotBeFound_ShouldFail"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("No chromosome names could be found for reference genome")
    }

    void "testPrepareAndReturnWorkflowSpecificCValues_cytosinePositionIndexFilePath"() {
        given:
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

        when:
        List<String> actualCommand = executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        then:
        expectedCommand == actualCommand
    }

    void "testPrepareAndReturnWorkflowSpecificCValues_CytosinePositionIndexNotDefined_ShouldThrowException"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        roddyBamFile.referenceGenome.cytosinePositionsIndex = null
        roddyBamFile.referenceGenome.save(flush: true)

        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        then:
        RuntimeException e = thrown(RuntimeException)
        e.message.contains("Cytosine position index for reference genome")
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_InputBamIsNull_ShouldFail"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(null)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("roddyBamFile must not be null")
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_ParentDirDoesNotExist_ShouldFail"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains(roddyBamFile.workDirectory.path)
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_ExitCodeNotNull_ShouldFail"() {
        given:
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

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("output.exitCode == 0")
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_MetadataFileCreationDidNotWork_FileNotAvailable_ShouldFail"() {
        given:
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

        when:
        executeWgbsAlignmentJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains(roddyBamFile.workMetadataTableFile.path)
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_LibraryStoredInSeqTrack"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        expect:
        testPrepareAndReturnWorkflowSpecificParameter("lib1", executeWgbsAlignmentJob)
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_NoLibraryStoredInSeqTrack"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)

        expect:
        testPrepareAndReturnWorkflowSpecificParameter(executeWgbsAlignmentJob)
    }

    void "testPrepareAndReturnWorkflowSpecificParameter_NoLibraryStoredInSeqTrackAndFileAlreadyExist"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        File metaDataTableFile = roddyBamFile.workMetadataTableFile
        assert metaDataTableFile.parentFile.mkdirs()
        metaDataTableFile.text = 'DUMMY TEXT'

        expect:
        testPrepareAndReturnWorkflowSpecificParameter(executeWgbsAlignmentJob)
        metaDataTableFile.text != 'DUMMY TEXT'
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
            String oneLine = "${it.sampleType.dirName}\t${it.seqTrack.libraryDirectoryName}\t${it.individual.pid}\t${it.seqType.libraryLayoutDirName}\t${it.run.dirName}\t${it.mateNumber}\t${lsdfFilesService.getFileViewByPidPath(it)}"

            assert lines.contains(oneLine)
        }
    }

    void "testWorkflowSpecificValidation_MethylationMergedDirDoesNotExist_ShouldFail"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        File methylationMergedDir = new File(roddyBamFile.workMethylationDirectory, "merged")
        methylationMergedDir.deleteDir()

        when:
        executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains(methylationMergedDir.path)
    }

    void "testWorkflowSpecificValidation_MethylationLibraryDirDoesNotExist_ShouldFail"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        roddyBamFile.seqTracks.add(DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.workPackage, [libraryName: "lib1", normalizedLibraryName: "1"]))
        roddyBamFile.numberOfMergedLanes = 2
        MergingCriteria.list()*.useLibPrepKit = false
        roddyBamFile.save(flush: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        File methylationLibraryDir = new File(roddyBamFile.workMethylationDirectory, "libNA")
        methylationLibraryDir.deleteDir()

        when:
        executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "testWorkflowSpecificValidation_AllFine"() {
        given:
        ExecuteWgbsAlignmentJob executeWgbsAlignmentJob = new ExecuteWgbsAlignmentJob()
        setupData(executeWgbsAlignmentJob)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        expect:
        executeWgbsAlignmentJob.workflowSpecificValidation(roddyBamFile)
    }

    private void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }
}
