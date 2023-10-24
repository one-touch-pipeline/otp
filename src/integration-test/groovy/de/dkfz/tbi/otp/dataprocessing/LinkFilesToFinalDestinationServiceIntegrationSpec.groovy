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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

@Rollback
@Integration
class LinkFilesToFinalDestinationServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    RoddyBamFile roddyBamFile
    TestConfigService configService

    @TempDir
    Path tempDir

    void setupData() {
        final int numberOfReads = DomainFactory.counter++

        roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.md5sum = null
        roddyBamFile.fileSize = -1
        roddyBamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING
        roddyBamFile.qcTrafficLightStatus = AbstractBamFile.QcTrafficLightStatus.QC_PASSED
        roddyBamFile.roddyExecutionDirectoryNames = ["exec_123456_123456789_test_test"]
        roddyBamFile.save(flush: true)

        configService.addOtpProperties(tempDir)

        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        seqTrack.save(flush: true)

        RawSequenceFile.findAllBySeqTrack(seqTrack).each {
            it.nReads = numberOfReads
            it.save(flush: true)
        }
        DomainFactory.createRoddyMergedBamQa(roddyBamFile, [pairedRead1: numberOfReads, pairedRead2: numberOfReads])

        DomainFactory.createRoddyProcessingOptions(tempDir.toFile())

        roddyBamFile.project.unixGroup = configService.workflowProjectUnixGroup
    }

    void cleanup() {
        configService.clean()
    }

    void "test getFilesToCleanup"(int countTmpFiles, int countTmpDir, boolean wgbs) {
        given:
        setupData()
        if (wgbs) {
            SeqType seqType = roddyBamFile.mergingWorkPackage.seqType
            seqType.name = SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName
            seqType.save(flush: true)
        }

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        List<File> filesNotToBeCalledFor = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory,
        ]

        if (wgbs) {
            filesNotToBeCalledFor += [roddyBamFile.workMethylationDirectory,
                                      roddyBamFile.workMetadataTableFile,
            ]
        }

        List<File> tmpFiles = []
        countTmpFiles.times {
            tmpFiles << File.createTempFile("tmp", ".tmp", roddyBamFile.workDirectory)
        }
        assert countTmpFiles == tmpFiles.size()

        List<File> tmpDirectories = []
        countTmpDir.times {
            File file = new File(roddyBamFile.workDirectory, HelperUtils.uniqueString)
            assert file.mkdir()
            tmpDirectories << file
        }
        assert countTmpDir == tmpDirectories.size()

        linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        linkFilesToFinalDestinationService.roddyConfigService = new RoddyConfigService()
        linkFilesToFinalDestinationService.fileSystemService = new TestFileSystemService()
        linkFilesToFinalDestinationService.fileService = new FileService()
        assert (filesNotToBeCalledFor + tmpFiles + tmpDirectories) as Set == roddyBamFile.workDirectory.listFiles() as Set

        when:
        List<Path> files = linkFilesToFinalDestinationService.getFilesToCleanup(roddyBamFile)

        then:
        filesNotToBeCalledFor.every {
            !files.contains(it.path)
        }
        tmpDirectories.every {
            files*.toString().contains(it.toString())
        }
        tmpFiles.every {
            files*.toString().contains(it.toString())
        }

        where:
        countTmpFiles | countTmpDir | wgbs  || _
        1             | 1           | false || _
        1             | 1           | true  || _
        1             | 0           | false || _
        0             | 1           | false || _
        0             | 0           | false || _
        2             | 3           | false || _
    }

    void "test linkNewResults, all fine"() {
        given:
        setupData()

        List<File> linkedFiles = createLinkedFilesList()

        linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        1 * linkFilesToFinalDestinationService.linkFileUtils.createAndValidateLinks(_, _) >> { Map<File, File> targetLinkMap, String group ->
            TestCase.assertContainSame(targetLinkMap.values(), linkedFiles)
        }
    }

    void "test linkNewResults, methylation one library, all fine"() {
        given:
        setupData()
        linkNewResults_methylation_setup()

        List<File> linkedFiles = createLinkedFilesList()
        linkedFiles.addAll(roddyBamFile.finalMergedMethylationDirectory)
        linkedFiles.addAll(roddyBamFile.finalMetadataTableFile)

        linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        1 * linkFilesToFinalDestinationService.linkFileUtils.createAndValidateLinks(_, _) >> { Map<File, File> targetLinkMap, String group ->
            TestCase.assertContainSame(targetLinkMap.values(), linkedFiles)
        }
    }

    void "test linkNewResults_methylation_TwoLibraries_AllFine"() {
        given:
        setupData()
        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(workPackage, [libraryName: 'library14', normalizedLibraryName: SeqTrack.normalizeLibraryName('library14')])
        seqTrack.save(flush: true)

        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes = 2
        roddyBamFile.save(flush: true)

        linkNewResults_methylation_setup()

        List<File> linkedFiles = createLinkedFilesList()
        linkedFiles.addAll(roddyBamFile.finalMergedMethylationDirectory)
        linkedFiles.addAll(roddyBamFile.finalMetadataTableFile)
        linkedFiles.addAll(roddyBamFile.finalLibraryMethylationDirectories.values())
        linkedFiles.addAll(roddyBamFile.finalLibraryQADirectories.values())

        linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        1 * linkFilesToFinalDestinationService.linkFileUtils.createAndValidateLinks(_, _) >> { Map<File, File> targetLinkMap, String group ->
            TestCase.assertContainSame(targetLinkMap.values(), linkedFiles)
        }
    }

    void "test linkNewResults_bamFileIsNull_shouldFail"() {
        given:
        setupData()

        when:
        linkFilesToFinalDestinationService.linkNewResults(null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("roddyBamFile")
    }

    void "test linkNewResults_bamHasOldStructure_shouldFail"() {
        given:
        setupData()
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("isOldStructureUsed")
    }

    private void linkNewResults_methylation_setup() {
        SeqType seqType = roddyBamFile.mergingWorkPackage.seqType
        seqType.name = SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName
        seqType.save(flush: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
    }

    private List<File> createLinkedFilesList() {
        return [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalMergedQADirectory,
                roddyBamFile.finalExecutionDirectories,
                roddyBamFile.finalSingleLaneQADirectories.values(),
        ].flatten()
    }

    void "test getOldResultsToCleanup, with base bam file, all fine"() {
        given:
        setupData()
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)

        List<File> filesToDelete = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
        ]
        List<File> filesToKeep = [
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workExecutionDirectories,
                roddyBamFile.workMergedQADirectory,
                roddyBamFile.workSingleLaneQADirectories.values(),
        ].flatten()
        [filesToKeep, filesToDelete].flatten().each {
            assert it.exists()
        }

        linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        linkFilesToFinalDestinationService.fileSystemService = new TestFileSystemService()
        linkFilesToFinalDestinationService.fileService = new FileService()

        when:
        List<Path> files = linkFilesToFinalDestinationService.getOldResultsToCleanup(roddyBamFile2)

        then:
        filesToDelete.every {
            files*.toString().contains(it.toString())
        }
        filesToKeep.every {
            !files*.toString().contains(it.toString())
        }
    }

    void "test getOldResultsToCleanup, withBaseBamFileOfOldStructure, allFine"() {
        given:
        setupData()
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)

        expect:
        [] == linkFilesToFinalDestinationService.getOldResultsToCleanup(roddyBamFile2)
    }

    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExist_allFine() {
        given:
        setupData()
        DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        assert roddyBamFile.workDirectory.exists()

        when:
        List<Path> files = linkFilesToFinalDestinationService.getOldResultsToCleanup(roddyBamFile2)

        then:
        files*.toString().contains(roddyBamFile.workDirectory.toString())
        !files*.toString().contains(roddyBamFile2.workDirectory.toString())
        files*.toString().contains(roddyBamFile.finalExecutionStoreDirectory.toString())
        files*.toString().contains(roddyBamFile.finalQADirectory.toString())
    }

    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(boolean latestIsOld) {
        given:
        setupData()
        if (latestIsOld) {
            DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, workDirectoryName: null, config: roddyBamFile.config)
        } else {
            DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        }
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        assert roddyBamFile2.workDirectory.exists()

        when:
        List<Path> files = linkFilesToFinalDestinationService.getOldResultsToCleanup(roddyBamFile2)

        then:
        !files*.toString().contains(roddyBamFile2.workDirectory.toString())
        roddyBamFile.finalExecutionDirectories.every {
            it.toString().startsWithAny(*(files*.toString()))
        }
        roddyBamFile.finalSingleLaneQADirectories.values().every {
            it.toString().startsWithAny(*(files*.toString()))
        }

        where:
        latestIsOld << [true, false]
    }

    void "test getOldResultsToCleanup, withoutBaseBamFileAndWithoutOtherBamFilesOfTheSameWorkPackage, allFine"() {
        given:
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        List<Path> files = linkFilesToFinalDestinationService.getOldResultsToCleanup(roddyBamFile)

        then:
        !files*.toString().contains(roddyBamFile.workDirectory.toString())
    }

    void "test getOldResultsToCleanup, bamFileIsNull, shouldFail"() {
        given:
        setupData()

        when:
        linkFilesToFinalDestinationService.getOldResultsToCleanup(null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("roddyBamFile")
    }

    void "test getOldResultsToCleanup, bamHasOldStructure, shouldFail"() {
        given:
        setupData()
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true)

        when:
        linkFilesToFinalDestinationService.getOldResultsToCleanup(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("isOldStructureUsed")
    }

    void "test prepareRoddyBamFile, adapterTrimmingRoddyBamFileHasLessNumberOfReadsThenAllSeqTracksTogether, ShouldBeFine"() {
        given:
        setupData()
        roddyBamFile.config.adapterTrimmingNeeded = true
        roddyBamFile.config.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        RoddyQualityAssessment qa = roddyBamFile.qualityAssessment
        qa.pairedRead1--
        qa.save(flush: true)
        assert roddyBamFile.numberOfReadsFromQa < roddyBamFile.numberOfReadsFromFastQc

        when:
        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)

        then:
        notThrown(Throwable)
    }

    void "test prepareRoddyBamFile, roddyBamFileHasLessNumberOfReadsThenAllSeqTracksTogether, ShouldFail"() {
        given:
        setupData()
        RoddyQualityAssessment qa = roddyBamFile.qualityAssessment
        qa.pairedRead1--
        qa.save(flush: true)
        assert roddyBamFile.numberOfReadsFromQa < roddyBamFile.numberOfReadsFromFastQc

        when:
        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message ==~ /.*bam file (.*) has less number of reads than the sum of all fastqc (.*).*/
    }

    void "test prepareRoddyBamFile, RoddyBamFileIsNotLatestBamFile, ShouldFail"() {
        given:
        setupData()
        roddyBamFile.metaClass.isMostRecentBamFile = { -> false }

        when:
        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message ==~ /.*The BamFile .* is not the most recent one.*/
    }

    void "test prepareRoddyBamFile, RoddyBamFileHasWrongState, ShouldFail"() {
        given:
        setupData()
        roddyBamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.DECLARED
        roddyBamFile.save(flush: true)

        when:
        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('assert [FileOperationStatus.NEEDS_PROCESSING, FileOperationStatus.INPROGRESS].contains(roddyBamFile.fileOperationStatus)')
    }

    void "test prepareRoddyBamFile, RoddyBamFileHasSecondCandidate, ShouldFail"() {
        given:
        setupData()
        DomainFactory.createRoddyBamFile([
                workPackage        : roddyBamFile.workPackage,
                withdrawn          : false,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
                md5sum             : null,
                fileSize           : -1,
                identifier         : roddyBamFile.identifier - 1,
                config             : roddyBamFile.config,
        ])

        when:
        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('Collection contains 2 elements. Expected 1')
    }

    private void finishOperationStateOfRoddyBamFile(RoddyBamFile roddyBamFile) {
        roddyBamFile.md5sum = HelperUtils.randomMd5sum
        roddyBamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.fileSize = 1000
        roddyBamFile.save(flush: true)
    }
}
