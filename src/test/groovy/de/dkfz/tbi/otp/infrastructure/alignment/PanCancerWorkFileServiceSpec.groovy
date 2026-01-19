/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.alignment

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.*

class PanCancerWorkFileServiceSpec extends Specification implements ServiceUnitTest<PanCancerWorkFileService>, WorkflowSystemDomainFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                Comment,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                Project,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    static final String RODDY_EXECUTION_DIR_NAME = "exec_000000_000000000_a_a"
    static final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    static final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    static final String COMMON_PREFIX = "4_NoIndex_L004"

    @TempDir
    Path tempDir

    RoddyBamFile roddyBamFile
    String baseDir = "/base-dir"
    String workFolder = "/work-folder"

    void setup() {
        roddyBamFile = createBamFile(
                roddyExecutionDirectoryNames: [],
        )

        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get(baseDir)
        }
        service.filestoreService = Mock(FilestoreService) {
            getWorkFolderPath(_) >> Paths.get(workFolder)
        }
    }

    void "test getDirectory, when workFolder doesn't exist"() {
        expect:
        service.getDirectoryPath(roddyBamFile) == Paths.get(baseDir, roddyBamFile.workDirectoryName)
    }

    void "test getDirectory, when workFolder exists"() {
        given:
        roddyBamFile.workflowArtefact = createWorkflowArtefact(producedBy: createWorkflowRun(workFolder: createWorkFolder()))

        expect:
        service.getDirectoryPath(roddyBamFile) == Paths.get(workFolder)
    }

    void "test getQADirectory"() {
        expect:
        service.getQADirectory(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR)
    }

    void "test getExecutionStoreDirectory"() {
        expect:
        service.getExecutionStoreDirectory(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFile.RODDY_EXECUTION_STORE_DIR)
    }

    void "test getBamFile"() {
        expect:
        service.getBamFile(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, roddyBamFile.bamFileName)
    }

    void "test getBaiFile"() {
        expect:
        service.getBaiFile(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, roddyBamFile.baiFileName)
    }

    void "test getMd5sumFile"() {
        expect:
        service.getMd5sumFile(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, "${roddyBamFile.bamFileName}.md5")
    }

    void "test getMergedQADirectory"() {
        expect:
        service.getMergedQADirectory(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, RoddyBamFileNames.MERGED_DIR)
    }

    void "test getMergedQAJsonFile"() {
        expect:
        service.getMergedQAJsonFile(roddyBamFile) ==
                Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, RoddyBamFileNames.MERGED_DIR, RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)
    }

    void "test getSingleLaneQADirectories, no seq tracks"() {
        given:
        roddyBamFile.seqTracks = null

        expect:
        service.getSingleLaneQADirectories(roddyBamFile).isEmpty()
    }

    void "test getSingleLaneQADirectories, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, "run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectories(roddyBamFile)
    }

    void "test getSingleLaneQADirectories, two seq tracks"() {
        given:
        updateRawSequenceFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        roddyBamFile.seqTracks.each {
            Path dir = Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, "run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getSingleLaneQADirectories(roddyBamFile)
    }

    void "test getSingleLaneQAJsonFiles, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get(baseDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, "run${seqTrack.run.name}_${COMMON_PREFIX}", RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)

        expect:
        [(seqTrack): file] == service.getSingleLaneQAJsonFiles(roddyBamFile)
    }

    void "test getLatestExecutionDirectory, when roddyExecutionDirectoryNames are empty, should fail"() {
        when:
        service.getLatestExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestExecutionDirectory, when latestDirectoryName is not last name in roddyExecutionDirectoryNames, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.addAll(["exec_100000_000000000_a_a", "exec_000000_000000000_a_a"])
        roddyBamFile.save(flush: true)

        when:
        service.getLatestExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestExecutionDirectory, when latestDirectoryName does not match, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.add("someName")

        when:
        service.getLatestExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestExecutionDirectory, when latestDirectoryName does not exist on file system, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)

        when:
        service.getLatestExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestExecutionDirectory, when latestDirectoryName is not directory, should fail"() {
        given:
        String fileName = RODDY_EXECUTION_DIR_NAME

        CreateFileHelper.createFile(tempDir.resolve(fileName))

        roddyBamFile.roddyExecutionDirectoryNames.add(fileName)

        when:
        service.getLatestExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestExecutionDirectory, all fine"(String roddyExecutionDirName) {
        given:
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> tempDir.resolve("base-dir")
        }

        roddyBamFile.roddyExecutionDirectoryNames.add(roddyExecutionDirName)

        Path file = service.getExecutionStoreDirectory(roddyBamFile).resolve(roddyExecutionDirName)
        Files.createDirectories(file)

        expect:
        file == service.getLatestExecutionDirectory(roddyBamFile)

        where:
        roddyExecutionDirName      | _
        'exec_000000_00000000_a_a' | _
        RODDY_EXECUTION_DIR_NAME   | _
    }

    private void updateRawSequenceFileNames(SeqTrack seqTrack) {
        List<RawSequenceFile> rawSequenceFiles = FastqFile.findAllBySeqTrack(seqTrack)
        rawSequenceFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        rawSequenceFiles[1].vbpFileName = SECOND_DATAFILE_NAME
        rawSequenceFiles*.save(flush: true)
    }
}
