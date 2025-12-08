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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Path
import java.nio.file.Paths

class WgbsAlignmentLinkFileServiceSpec extends Specification implements ServiceUnitTest<WgbsAlignmentLinkFileService>, WorkflowSystemDomainFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
        ]
    }

    static final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    static final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    static final String COMMON_PREFIX = "4_NoIndex_L004"

    RoddyBamFile bamFile

    void setupData(int count = 1) {
        bamFile = createBamFile(
                roddyExecutionDirectoryNames: [],
        )

        service.abstractBamFileService = Mock(AbstractBamFileService) {
            count * getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getQADirectory"() {
        given:
        setupData()

        expect:
        service.getQADirectory(bamFile) == Paths.get("/base-dir", RoddyBamFileNames.QUALITY_CONTROL_DIR)
    }

    void "test getRoddyExecutionStoreDirectory"() {
        given:
        setupData()

        expect:
        service.getExecutionStoreDirectory(bamFile) == Paths.get("/base-dir", RoddyBamFile.RODDY_EXECUTION_STORE_DIR)
    }

    void "test getBamFile"() {
        given:
        setupData()

        expect:
        service.getBamFile(bamFile) == Paths.get("/base-dir", bamFile.bamFileName)
    }

    void "test getBaiFile"() {
        given:
        setupData()

        expect:
        service.getBaiFile(bamFile) == Paths.get("/base-dir", bamFile.baiFileName)
    }

    void "test getMd5sumFile"() {
        given:
        setupData()

        expect:
        service.getMd5sumFile(bamFile) == Paths.get("/base-dir", "${bamFile.bamFileName}.md5")
    }

    void "test getMergedQADirectory"() {
        given:
        setupData()

        expect:
        service.getMergedQADirectory(bamFile) ==
                Paths.get("/base-dir", RoddyBamFileNames.QUALITY_CONTROL_DIR, RoddyBamFileNames.MERGED_DIR)
    }

    void "test getRoddyMergedQAJsonFile"() {
        given:
        setupData()

        expect:
        service.getMergedQAJsonFile(bamFile) ==
                Paths.get("/base-dir", RoddyBamFileNames.QUALITY_CONTROL_DIR, RoddyBamFileNames.MERGED_DIR, RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)
    }

    void "test getRoddySingleLaneQADirectories, no seq tracks"() {
        given:
        setupData()

        bamFile.seqTracks = null

        expect:
        service.getSingleLaneQADirectories(bamFile).isEmpty()
    }

    void "test getRoddySingleLaneQADirectories, one seq track"() {
        given:
        setupData()

        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("/base-dir", RoddyBamFileNames.QUALITY_CONTROL_DIR, "run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectories(bamFile)
    }

    void "test getRoddySingleLaneQADirectories, two seq tracks"() {
        given:
        setupData()

        updateRawSequenceFileNames(bamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(bamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        bamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir", RoddyBamFileNames.QUALITY_CONTROL_DIR, "run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getSingleLaneQADirectories(bamFile)
    }

    void "test getSingleLaneQAJsonFiles, one seq track"() {
        given:
        setupData()

        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get("/base-dir", RoddyBamFileNames.QUALITY_CONTROL_DIR, "run${seqTrack.run.name}_${COMMON_PREFIX}", RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)

        expect:
        [(seqTrack): file] == service.getSingleLaneQAJsonFiles(bamFile)
    }

    void "test finalRoddyExecutionDirectories"(List<String> roddyExecutionDirectoryNames, int count) {
        given:
        setupData(count)

        bamFile.roddyExecutionDirectoryNames.addAll(roddyExecutionDirectoryNames)
        List<Path> expectedResult = roddyExecutionDirectoryNames.collect {
            Paths.get("/base-dir", RoddyBamFile.RODDY_EXECUTION_STORE_DIR, it)
        }

        expect:
        expectedResult == service.getExecutionDirectories(bamFile)

        where:
        roddyExecutionDirectoryNames                                        | count
        ['exec_123456_123456789_bla_bla', 'exec_654321_987654321_bla_bla',] | 2
        []                                                                  | 0
    }

    void "test getPathForFurtherProcessingNoCheck, should call wgbsAlignmentWorkFileService.getBamFile"() {
        given:
        Path path = Paths.get("/some/path")
        service.wgbsAlignmentWorkFileService = Mock(WgbsAlignmentWorkFileService) {
            1 * getBamFile(_) >> path
        }

        expect:
        service.getPathForFurtherProcessingNoCheck(bamFile) == path
    }

    private void updateRawSequenceFileNames(SeqTrack seqTrack) {
        List<RawSequenceFile> rawSequenceFiles = FastqFile.findAllBySeqTrack(seqTrack)
        rawSequenceFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        rawSequenceFiles[1].vbpFileName = SECOND_DATAFILE_NAME
        rawSequenceFiles*.save(flush: true)
    }
}
