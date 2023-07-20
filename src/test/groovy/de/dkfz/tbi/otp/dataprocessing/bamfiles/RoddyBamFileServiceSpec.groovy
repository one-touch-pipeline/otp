/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.*

class RoddyBamFileServiceSpec extends Specification implements ServiceUnitTest<RoddyBamFileService>, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
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
                Realm,
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

    def setup() {
        roddyBamFile = createBamFile(
                roddyExecutionDirectoryNames: [],
        )

        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get(baseDir)
        }
    }

    void "test getFinalInsertSizeDirectory"() {
        given:
        Path expectedPath = Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}/" +
                "${RoddyBamFileService.INSERT_SIZE_FILE_DIRECTORY}")

        expect:
        expectedPath == service.getFinalInsertSizeDirectory(roddyBamFile)
    }

    void "test getFinalInsertSizeFile"() {
        given:
        Path expectedPath = Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}/" +
                "${RoddyBamFileService.INSERT_SIZE_FILE_DIRECTORY}/${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_" +
                "${RoddyBamFileService.INSERT_SIZE_FILE_SUFFIX}")

        expect:
        expectedPath == service.getFinalInsertSizeFile(roddyBamFile)
    }

    void "test getWorkDirectory"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}") == service.getWorkDirectory(roddyBamFile)
    }

    void "test getWorkQADirectory"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}") ==
                service.getWorkQADirectory(roddyBamFile)
    }

    void "test getFinalQADirectory"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}") ==
                service.getFinalQADirectory(roddyBamFile)
    }

    void "test getWorkExecutionStoreDirectory"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}") ==
                service.getWorkExecutionStoreDirectory(roddyBamFile)
    }

    void "test getFinalRoddyExecutionStoreDirectory"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}") ==
                service.getFinalExecutionStoreDirectory(roddyBamFile)
    }

    void "test getWorkBamFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.bamFileName}") ==
                service.getWorkBamFile(roddyBamFile)
    }

    void "test getWorkBaiFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.baiFileName}") ==
                service.getWorkBaiFile(roddyBamFile)
    }

    void "test getWorkMd5sumFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.md5sumFileName}") ==
                service.getWorkMd5sumFile(roddyBamFile)
    }

    void "test getFinalBamFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.bamFileName}") ==
                service.getFinalBamFile(roddyBamFile)
    }

    void "test getFinalBaiFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.baiFileName}") ==
                service.getFinalBaiFile(roddyBamFile)
    }

    void "test getFinalMd5sumFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.md5sumFileName}") ==
                service.getFinalMd5sumFile(roddyBamFile)
    }

    void "test getWorkMergedQADirectory"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}") ==
                service.getWorkMergedQADirectory(roddyBamFile)
    }

    void "test getWorkMergedQAJsonFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}/" +
                "${RoddyBamFileService.QUALITY_CONTROL_JSON_FILE_NAME}") ==
                service.getWorkMergedQAJsonFile(roddyBamFile)
    }

    void "test getFinalMergedQADirectory"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}") ==
                service.getFinalMergedQADirectory(roddyBamFile)
    }

    void "test getFinalRoddyMergedQAJsonFile"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}/" +
                "${RoddyBamFileService.QUALITY_CONTROL_JSON_FILE_NAME}") ==
                service.getFinalMergedQAJsonFile(roddyBamFile)
    }

    void "test getWorkSingleLaneQADirectories, no seq tracks"() {
        given:
        roddyBamFile.seqTracks = null

        expect:
        service.getWorkSingleLaneQADirectories(roddyBamFile).isEmpty()
    }

    void "test getWorkSingleLaneQADirectories, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getWorkSingleLaneQADirectories(roddyBamFile)
    }

    void "test getWorkSingleLaneQADirectories, two seq tracks"() {
        given:
        updateRawSequenceFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        roddyBamFile.seqTracks.each {
            Path dir = Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getWorkSingleLaneQADirectories(roddyBamFile)
    }

    void "test getWorkSingleLaneQAJsonFiles, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFileService.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == service.getWorkSingleLaneQAJsonFiles(roddyBamFile)
    }

    void "test getFinalRoddySingleLaneQADirectories, no seq tracks"() {
        given:
        roddyBamFile.seqTracks = null

        expect:
        service.getFinalSingleLaneQADirectories(roddyBamFile).isEmpty()
    }

    void "test getFinalRoddySingleLaneQADirectories, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getFinalSingleLaneQADirectories(roddyBamFile)
    }

    void "test getFinalRoddySingleLaneQADirectories, two seq tracks"() {
        given:
        updateRawSequenceFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        roddyBamFile.seqTracks.each {
            Path dir = Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getFinalSingleLaneQADirectories(roddyBamFile)
    }

    void "test getFinalRoddySingleLaneQAJsonFiles, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/" +
                "${RoddyBamFileService.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == service.getFinalSingleLaneQAJsonFiles(roddyBamFile)
    }

    void "test getRoddySingleLaneQADirectoriesHelper, final folder, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("${baseDir}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectoriesHelper(roddyBamFile, service.getFinalQADirectory(roddyBamFile))
    }

    void "test getRoddySingleLaneQADirectoriesHelper, work folder, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("${baseDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectoriesHelper(roddyBamFile, service.getWorkQADirectory(roddyBamFile))
    }

    void "test getLatestWorkExecutionDirectory, when roddyExecutionDirectoryNames are empty, should fail"() {
        when:
        service.getLatestWorkExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestWorkExecutionDirectory, when latestDirectoryName is not last name in roddyExecutionDirectoryNames, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.addAll(["exec_100000_000000000_a_a", "exec_000000_000000000_a_a"])
        roddyBamFile.save(flush: true)

        when:
        service.getLatestWorkExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestWorkExecutionDirectory, when latestDirectoryName does not match, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.add("someName")

        when:
        service.getLatestWorkExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestWorkExecutionDirectory, when latestDirectoryName does not exist on file system, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)

        when:
        service.getLatestWorkExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestWorkExecutionDirectory, when latestDirectoryName is not directory, should fail"() {
        given:
        String fileName = RODDY_EXECUTION_DIR_NAME

        CreateFileHelper.createFile(tempDir.resolve(fileName))

        roddyBamFile.roddyExecutionDirectoryNames.add(fileName)

        when:
        service.getLatestWorkExecutionDirectory(roddyBamFile)

        then:
        thrown(AssertionError)
    }

    void "test getLatestWorkExecutionDirectory, all fine"(String roddyExecutionDirName) {
        given:
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> tempDir.resolve("base-dir")
        }

        roddyBamFile.roddyExecutionDirectoryNames.add(roddyExecutionDirName)

        Path file = service.getWorkExecutionStoreDirectory(roddyBamFile).resolve(roddyExecutionDirName)
        Files.createDirectories(file)

        expect:
        file == service.getLatestWorkExecutionDirectory(roddyBamFile)

        where:
        roddyExecutionDirName      | _
        'exec_000000_00000000_a_a' | _
        RODDY_EXECUTION_DIR_NAME   | _
    }

    void "test finalRoddyExecutionDirectories"(List<String> roddyExecutionDirectoryNames) {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.addAll(roddyExecutionDirectoryNames)
        List<Path> expectedResult = roddyExecutionDirectoryNames.collect {
            Paths.get("${baseDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/${it}")
        }

        expect:
        expectedResult == service.getFinalExecutionDirectories(roddyBamFile)

        where:
        roddyExecutionDirectoryNames                                        | _
        ['exec_123456_123456789_bla_bla', 'exec_654321_987654321_bla_bla',] | _
        []                                                                  | _
    }

    void "test isOldStructureUsed, when old structure is used, should return true"() {
        given:
        roddyBamFile.workDirectoryName = null

        expect:
        service.isOldStructureUsed(roddyBamFile)
    }

    void "test isOldStructureUsed, when link structure is used, should return false"() {
        given:
        roddyBamFile.workDirectoryName = 'someWorkDirectory'

        expect:
        !service.isOldStructureUsed(roddyBamFile)
    }

    void "test getPathForFurtherProcessing, returns null since qcTrafficLightStatus is #status"() {
        given:
        roddyBamFile.qcTrafficLightStatus = status
        roddyBamFile.comment = DomainFactory.createComment()

        expect:
        !service.getPathForFurtherProcessing(roddyBamFile)

        where:
        status << [AbstractBamFile.QcTrafficLightStatus.BLOCKED, AbstractBamFile.QcTrafficLightStatus.REJECTED]
    }

    void "test getPathForFurtherProcessing, when old structure is used, should return final directory"() {
        given:
        roddyBamFile.workDirectoryName = null
        assert roddyBamFile.save(flush: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        expect:
        service.getFinalBamFile(roddyBamFile) == service.getPathForFurtherProcessing(roddyBamFile)
    }

    void "test getPathForFurtherProcessing, when new structure is used, should return work directory"() {
        given:
        roddyBamFile.workDirectoryName = 'someDir'
        assert roddyBamFile.save(flush: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        expect:
        service.getWorkBamFile(roddyBamFile) == service.getPathForFurtherProcessing(roddyBamFile)
    }

    void "test getPathForFurtherProcessing, when new structure is used and not set in mergingWorkPackage, should throw exception"() {
        given:
        roddyBamFile.workDirectoryName = 'someDir'
        roddyBamFile.save(flush: true)

        when:
        service.getPathForFurtherProcessing(roddyBamFile)

        then:
        thrown(IllegalStateException)
    }

    private void updateRawSequenceFileNames(SeqTrack seqTrack) {
        List<RawSequenceFile> rawSequenceFiles = FastqFile.findAllBySeqTrack(seqTrack)
        rawSequenceFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        rawSequenceFiles[1].vbpFileName = SECOND_DATAFILE_NAME
        rawSequenceFiles*.save(flush: true)
    }
}
