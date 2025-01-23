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
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

import java.nio.file.Path
import java.nio.file.Paths

class WgbsAlignmentWorkFileServiceSpec extends Specification implements ServiceUnitTest<WgbsAlignmentWorkFileService>, WorkflowSystemDomainFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                WorkflowArtefact,
        ]
    }

    static final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    static final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    static final String COMMON_PREFIX = "4_NoIndex_L004"

    RoddyBamFile bamFile

    void setupNonUuid() {
        bamFile = createBamFile()
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            1 * getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void setupUuid() {
        bamFile = createBamFile([
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        service.filestoreService = Mock(FilestoreService) {
            1 * getWorkFolderPath(_) >> Paths.get("/base-dir-uuid")
        }
    }

    void "test getDirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getDirectoryPath(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}"
    }

    void "test getQADirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getQADirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}"
    }

    void "test getExecutionStoreDirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getExecutionStoreDirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}"
    }

    void "test getBamFile"() {
        given:
        setupNonUuid()

        expect:
        service.getBamFile(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.bamFileName}"
    }

    void "test getBaiFile"() {
        given:
        setupNonUuid()

        expect:
        service.getBaiFile(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.baiFileName}"
    }

    void "test getMd5sumFile"() {
        given:
        setupNonUuid()

        expect:
        service.getMd5sumFile(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${bamFile.bamFileName}.md5"
    }

    void "test getMergedQADirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getMergedQADirectory(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${RoddyBamFileNames.MERGED_DIR}"
    }

    void "test getMergedQAJsonFile"() {
        given:
        setupNonUuid()

        expect:
        service.getMergedQAJsonFile(bamFile).toString() == "/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/" +
                "${RoddyBamFileNames.MERGED_DIR}/${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}"
    }

    void "test getSingleLaneQADirectories, no seq tracks"() {
        given:
        setupNonUuid()

        bamFile.seqTracks = null

        expect:
        service.getSingleLaneQADirectories(bamFile).isEmpty()
    }

    void "test getSingleLaneQADirectories, one seq track"() {
        given:
        setupNonUuid()

        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectories(bamFile)
    }

    void "test getSingleLaneQADirectories, two seq tracks"() {
        given:
        setupNonUuid()

        updateRawSequenceFileNames(bamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(bamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        bamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getSingleLaneQADirectories(bamFile)
    }

    void "test getSingleLaneQAJsonFiles, one seq track"() {
        given:
        setupNonUuid()

        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get("/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == service.getSingleLaneQAJsonFiles(bamFile)
    }

    void "test getLibraryQADirectories"() {
        given:
        setupNonUuid()
        setupTwoLanesDifferentLibrary()

        Map<String, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${it.libraryDirectoryName}")
            expected.put((it.libraryDirectoryName), dir)
        }

        expect:
        expected == service.getLibraryQADirectories(bamFile)
    }

    void "test getLibraryMethylationDirectories"() {
        given:
        setupNonUuid()
        setupTwoLanesDifferentLibrary()

        Map<String, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir/${bamFile.workDirectoryName}/${WgbsAlignmentWorkFileService.METHYLATION_DIR}/${it.libraryDirectoryName}")
            expected.put((it.libraryDirectoryName), dir)
        }

        expect:
        expected == service.getLibraryMethylationDirectories(bamFile)
    }

    void "test getLibraryQAJsonFiles"() {
        given:
        setupNonUuid()
        setupTwoLanesDifferentLibrary()

        Map<String, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir/${bamFile.workDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${it.libraryDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}")
            expected.put((it.libraryDirectoryName), dir)
        }

        expect:
        expected == service.getLibraryQAJsonFiles(bamFile)
    }

    void "test getDirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getDirectoryPath(bamFile).toString() == "/base-dir-uuid"
    }

    void "test getQADirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getQADirectory(bamFile).toString() == "/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}"
    }

    void "test getExecutionStoreDirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getExecutionStoreDirectory(bamFile).toString() == "/base-dir-uuid/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}"
    }

    void "test getBamFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getBamFile(bamFile).toString() == "/base-dir-uuid/${bamFile.bamFileName}"
    }

    void "test getBaiFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getBaiFile(bamFile).toString() == "/base-dir-uuid/${bamFile.baiFileName}"
    }

    void "test getMd5sumFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getMd5sumFile(bamFile).toString() == "/base-dir-uuid/${bamFile.bamFileName}.md5"
    }

    void "test getMergedQADirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getMergedQADirectory(bamFile).toString() == "/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${RoddyBamFileNames.MERGED_DIR}"
    }

    void "test getMergedQAJsonFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getMergedQAJsonFile(bamFile).toString() == "/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/" +
                "${RoddyBamFileNames.MERGED_DIR}/${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}"
    }

    void "test getSingleLaneQADirectories for uuid structure, no seq tracks"() {
        given:
        setupUuid()

        bamFile.seqTracks = null

        expect:
        service.getSingleLaneQADirectories(bamFile).isEmpty()
    }

    void "test getSingleLaneQADirectories for uuid structure, one seq track"() {
        given:
        setupUuid()

        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path dir = Paths.get("/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == service.getSingleLaneQADirectories(bamFile)
    }

    void "test getSingleLaneQADirectories for uuid structure, two seq tracks"() {
        given:
        setupUuid()

        updateRawSequenceFileNames(bamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(bamFile.workPackage)
        updateRawSequenceFileNames(seqTrack)
        bamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        expect:
        expected == service.getSingleLaneQADirectories(bamFile)
    }

    void "test getSingleLaneQAJsonFiles for uuid structure, one seq track"() {
        given:
        setupUuid()

        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack)
        Path file = Paths.get("/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/" +
                "run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == service.getSingleLaneQAJsonFiles(bamFile)
    }

    void "test getLibraryQADirectories for uuid structure"() {
        given:
        setupUuid()
        setupTwoLanesDifferentLibrary()

        Map<String, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${it.libraryDirectoryName}")
            expected.put((it.libraryDirectoryName), dir)
        }

        expect:
        expected == service.getLibraryQADirectories(bamFile)
    }

    void "test getLibraryMethylationDirectories for uuid structure"() {
        given:
        setupUuid()
        setupTwoLanesDifferentLibrary()

        Map<String, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir-uuid/${WgbsAlignmentWorkFileService.METHYLATION_DIR}/${it.libraryDirectoryName}")
            expected.put((it.libraryDirectoryName), dir)
        }

        expect:
        expected == service.getLibraryMethylationDirectories(bamFile)
    }

    void "test getLibraryQAJsonFiles for uuid structure"() {
        given:
        setupUuid()
        setupTwoLanesDifferentLibrary()

        Map<String, Path> expected = [:]
        bamFile.seqTracks.each {
            Path dir = Paths.get("/base-dir-uuid/${RoddyBamFileNames.QUALITY_CONTROL_DIR}/${it.libraryDirectoryName}/${RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME}")
            expected.put((it.libraryDirectoryName), dir)
        }

        expect:
        expected == service.getLibraryQAJsonFiles(bamFile)
    }

    private void setupTwoLanesDifferentLibrary() {
        SeqTrack seqTrack1 = bamFile.seqTracks.iterator()[0]
        updateRawSequenceFileNames(seqTrack1)
        seqTrack1.libraryName = seqTrack1.normalizedLibraryName = "lib1"
        seqTrack1.save(flush: true)

        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithFastqFiles(bamFile.workPackage)
        updateRawSequenceFileNames(seqTrack2)
        seqTrack2.libraryName = seqTrack2.normalizedLibraryName = "lib2"
        seqTrack2.save(flush: true)
        bamFile.seqTracks.add(seqTrack2)
    }

    private void updateRawSequenceFileNames(SeqTrack seqTrack) {
        List<RawSequenceFile> rawSequenceFiles = FastqFile.findAllBySeqTrack(seqTrack)
        rawSequenceFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        rawSequenceFiles[1].vbpFileName = SECOND_DATAFILE_NAME
        rawSequenceFiles*.save(flush: true)
    }
}
