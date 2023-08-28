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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.pancancer

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37

import java.nio.file.Files
import java.nio.file.Path

/**
 * base class for all PanCancer workflow test without antibody target
 */
abstract class AbstractPanCancerWorkflowWithoutAntibodySpec extends AbstractPanCancerWorkflowSpec implements ReferenceGenomeHg37 {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractPanCancerWorkflowWithoutAntibodySpec)

    /**
     * Path of the test bam file
     */
    protected Path firstBamFilePath

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        SessionUtils.withTransaction {
            firstBamFilePath = referenceDataDirectory.resolve('bamFiles/wgs/first-bam-file/control_merged.mdup.bam')
        }
        log.debug("Finish setup ${this.class.simpleName}")
    }

    void "test align lanes only, no base bam file exists, one lane, with fingerPrinting, all fine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            setUpFingerPrintingFile()
            decide(3, 1)
        }

        when:
        execute(1, 2)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "test align lanes only, no base bam file exists, two lanes, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack

        SessionUtils.withTransaction {
            firstSeqTrack = createSeqTrack("readGroup1")
            secondSeqTrack = createSeqTrack("readGroup2")
            decide(6, 1)
        }

        when:
        execute(1, 4)

        then:
        verify_alignLanesOnly_NoBaseBamExist_TwoLanes(firstSeqTrack, secondSeqTrack)
    }

    void "test, align with base bam file and new lanes, all fine"() {
        given:
        SessionUtils.withTransaction {
            createFirstRoddyBamFile()
            createSeqTrack("readGroup2")
            decide(7, 1)
        }

        when:
        execute(1, 5)

        then:
        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    void "test align with withdrawn base bam file, all fine"() {
        given:
        RoddyBamFile roddyBamFile
        SessionUtils.withTransaction {
            roddyBamFile = createFirstRoddyBamFile()
            roddyBamFile.withdrawn = true
            roddyBamFile.save(flush: true)

            decide(4, 1)
        }

        when:
        execute(1, 3)

        then:
        SessionUtils.withTransaction {
            assert !roddyBamFile.workDirectory.exists()
            checkWorkPackageState()

            List<RoddyBamFile> bamFiles = RoddyBamFile.findAll().sort { it.id }
            assert bamFiles.size() == 2
            assert roddyBamFile == bamFiles.first()
            assert !bamFiles[1].baseBamFile
            checkFirstBamFileState(bamFiles[1], true, [identifier: 1])
            assertBamFileFileSystemPropertiesSet(bamFiles[1])
            assertBaseFileSystemState(bamFiles[1])
            return true
        }
    }

    RoddyBamFile createFirstRoddyBamFile() {
        assert Files.exists(firstBamFilePath)

        MergingWorkPackage workPackage = createMergingWorkPackageOfFirstBamFile()
        SeqTrack seqTrack = createSeqTrack("readGroup1")

        RoddyBamFile firstBamFile = createBamFile([
                workPackage                 : workPackage,
                identifier                  : RoddyBamFile.nextIdentifier(workPackage),
                seqTracks                   : [seqTrack] as Set,
                config                      : null,
                numberOfMergedLanes         : 1,
                fileOperationStatus         : AbstractBamFile.FileOperationStatus.PROCESSED,
                md5sum                      : HelperUtils.randomMd5sum,
                fileSize                    : Files.size(firstBamFilePath),
                dateFromFileSystem          : new Date(Files.getLastModifiedTime(firstBamFilePath).toMillis()),
                roddyExecutionDirectoryNames: ['exec_123456_123456789_bla_bla'],
                workDirectoryName           : "${RoddyBamFile.WORK_DIR_PREFIX}_0",
        ])

        workPackage.bamFileInProjectFolder = firstBamFile
        workPackage.needsProcessing = false
        workPackage.save(flush: true)

        createWorkflowArtefacts(workflowAlignment, firstBamFile, ArtefactType.BAM)

        createWorkFilesForFirstBamFile(firstBamFile)
        createLinksForFirstBamFile(firstBamFile)

        return firstBamFile
    }

    private MergingWorkPackage createMergingWorkPackageOfFirstBamFile() {
        return createMergingWorkPackage([
                sample               : sample,
                seqType              : seqType,
                pipeline             : pipeline,
                libraryPreparationKit: seqType.wgbs ? null : libraryPreparationKit,
                seqPlatformGroup     : seqPlatformGroup,
                referenceGenome      : referenceGenome,
                needsProcessing      : false,
                statSizeFileName     : null,
        ])
    }

    private void createWorkFilesForFirstBamFile(RoddyBamFile firstBamFile) {
        Path workBamFile = roddyBamFileService.getWorkBamFile(firstBamFile)
        fileService.createLink(workBamFile, firstBamFilePath, realm)

        List<Path> files = [
                roddyBamFileService.getWorkBaiFile(firstBamFile),
                roddyBamFileService.getWorkMd5sumFile(firstBamFile),
                roddyBamFileService.getWorkMergedQAJsonFile(firstBamFile),
                roddyConfigService.getConfigFile(workBamFile.parent),
        ]

        files.addAll(roddyBamFileService.getWorkSingleLaneQAJsonFiles(firstBamFile).values())

        roddyBamFileService.getWorkExecutionDirectories(firstBamFile).each { Path path ->
            filesInRoddyExecutionDir.each {
                files << path.resolve(it)
            }
        }

        files.each { Path path ->
            fileService.createFileWithContent(path, "DUMMY content", realm)
        }
    }

    private void createLinksForFirstBamFile(RoddyBamFile firstBamFile) {
        Map<Path, Path> finalToWork = [
                (roddyBamFileService.getFinalBamFile(firstBamFile))          : roddyBamFileService.getWorkBamFile(firstBamFile),
                (roddyBamFileService.getFinalBaiFile(firstBamFile))          : roddyBamFileService.getWorkBaiFile(firstBamFile),
                (roddyBamFileService.getFinalMd5sumFile(firstBamFile))       : roddyBamFileService.getWorkMd5sumFile(firstBamFile),
                (roddyBamFileService.getFinalMergedQADirectory(firstBamFile)): roddyBamFileService.getWorkMergedQADirectory(firstBamFile),
        ]

        Map<SeqTrack, Path> finalSingleLaneQa = roddyBamFileService.getFinalSingleLaneQADirectories(firstBamFile)
        Map<SeqTrack, Path> workSingleLaneQa = roddyBamFileService.getWorkSingleLaneQADirectories(firstBamFile)
        finalSingleLaneQa.each { SeqTrack seqTrack, Path path ->
            finalToWork[(path)] = workSingleLaneQa[seqTrack]
        }

        List<Path> finalRoddyExecutionDirectory = roddyBamFileService.getFinalExecutionDirectories(firstBamFile)
        List<Path> workRoddyExecutionDirectory = roddyBamFileService.getWorkExecutionDirectories(firstBamFile)
        finalRoddyExecutionDirectory.eachWithIndex { Path path, int i ->
            finalToWork[(path)] = workRoddyExecutionDirectory[i]
        }

        finalToWork.each { Path finalPath, Path workPath ->
            fileService.createLink(finalPath, workPath, realm)
        }
    }
}
