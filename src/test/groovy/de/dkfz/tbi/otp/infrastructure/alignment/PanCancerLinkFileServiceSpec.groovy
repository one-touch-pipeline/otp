/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path
import java.nio.file.Paths

class PanCancerLinkFileServiceSpec extends Specification implements ServiceUnitTest<PanCancerLinkFileService>, WorkflowSystemDomainFactory, IsRoddy, DataTest {

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

    static final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    static final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    static final String COMMON_PREFIX = "4_NoIndex_L004"

    RoddyBamFile roddyBamFile
    String baseDir = "/base-dir"

    void setup() {
        roddyBamFile = createBamFile(
                roddyExecutionDirectoryNames: [],
        )

        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get(baseDir)
        }
    }

    void "test getQADirectory"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}") ==
                service.getQADirectory(roddyBamFile)
    }

    void "test getRoddyExecutionStoreDirectory"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}") ==
                service.getExecutionStoreDirectory(roddyBamFile)
    }

    void "test getBamFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.bamFileName}") ==
                service.getBamFile(roddyBamFile)
    }

    void "test getBaiFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.baiFileName}") ==
                service.getBaiFile(roddyBamFile)
    }

    void "test getMd5sumFile"() {
        expect:
        Paths.get("${baseDir}/${roddyBamFile.bamFileName}.md5") ==
                service.getMd5sumFile(roddyBamFile)
    }

    void "test getMergedQADirectory"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${RoddyBamFileNames.MERGED_DIR}") ==
                service.getMergedQADirectory(roddyBamFile)
    }

    void "test getRoddyMergedQAJsonFile"() {
        expect:
        Paths.get("${baseDir}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${RoddyBamFileNames.MERGED_DIR}/" +
                "${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}") ==
                service.getMergedQAJsonFile(roddyBamFile)
    }

    void "test getRoddySingleLaneQADirectories, no seq tracks"() {
        given:
        roddyBamFile.seqTracks = null

        expect:
        service.getSingleLaneQADirectories(roddyBamFile).isEmpty()
    }

    void "test getRoddySingleLaneQADirectories, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("${baseDir}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectories(roddyBamFile)
    }

    void "test getRoddySingleLaneQADirectories, two seq tracks"() {
        given:
        updateRawSequenceFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        roddyBamFile.seqTracks.each {
            Path dir = Paths.get("${baseDir}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getSingleLaneQADirectories(roddyBamFile)
    }

    void "test getRoddySingleLaneQAJsonFiles, one seq track"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get("${baseDir}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/" +
                "${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == service.getSingleLaneQAJsonFiles(roddyBamFile)
    }

    void "test finalRoddyExecutionDirectories"(List<String> roddyExecutionDirectoryNames) {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.addAll(roddyExecutionDirectoryNames)
        List<Path> expectedResult = roddyExecutionDirectoryNames.collect {
            Paths.get("${baseDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/${it}")
        }

        expect:
        expectedResult == service.getExecutionDirectories(roddyBamFile)

        where:
        roddyExecutionDirectoryNames                                        | _
        ['exec_123456_123456789_bla_bla', 'exec_654321_987654321_bla_bla',] | _
        []                                                                  | _
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

    void "test getPathForFurtherProcessingNoCheck, when new structure is used, should call panCancerWorkFileService.getBamFile"() {
        given:
        Path path = Paths.get("/some/path")
        service.panCancerWorkFileService = Mock(PanCancerWorkFileService) {
            1 * getBamFile(_) >> path
        }

        expect:
        service.getPathForFurtherProcessingNoCheck(roddyBamFile) == path
    }

    void "test getPathForFurtherProcessingNoCheck, when old structure is used, should use getBamFile"() {
        given:
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true)

        expect:
        Paths.get("${baseDir}/${roddyBamFile.bamFileName}") ==
                service.getPathForFurtherProcessingNoCheck(roddyBamFile)
    }

    private void updateRawSequenceFileNames(SeqTrack seqTrack) {
        List<RawSequenceFile> rawSequenceFiles = FastqFile.findAllBySeqTrack(seqTrack)
        rawSequenceFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        rawSequenceFiles[1].vbpFileName = SECOND_DATAFILE_NAME
        rawSequenceFiles*.save(flush: true)
    }
}
