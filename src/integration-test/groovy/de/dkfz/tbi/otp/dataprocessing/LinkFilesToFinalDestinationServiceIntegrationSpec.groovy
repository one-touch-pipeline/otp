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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils

import java.nio.file.Files
import java.nio.file.Path

@Rollback
@Integration
class LinkFilesToFinalDestinationServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    RoddyBamFile roddyBamFile
    TestConfigService configService

    PanCancerLinkFileService panCancerLinkFileService
    PanCancerWorkFileService panCancerWorkFileService
    WgbsAlignmentLinkFileService wgbsAlignmentLinkFileService
    WgbsAlignmentWorkFileService wgbsAlignmentWorkFileService

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

        linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        panCancerLinkFileService = new PanCancerLinkFileService()
        panCancerLinkFileService.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> tempDir.resolve('link')
        }
        panCancerWorkFileService = new PanCancerWorkFileService()
        panCancerWorkFileService.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> tempDir.resolve('work')
        }
        linkFilesToFinalDestinationService.panCancerLinkFileService = panCancerLinkFileService
        linkFilesToFinalDestinationService.panCancerWorkFileService = panCancerWorkFileService

        wgbsAlignmentLinkFileService = new WgbsAlignmentLinkFileService(abstractBamFileService: panCancerLinkFileService.abstractBamFileService)
        linkFilesToFinalDestinationService.wgbsAlignmentLinkFileService = wgbsAlignmentLinkFileService
        wgbsAlignmentWorkFileService = new WgbsAlignmentWorkFileService(abstractBamFileService: panCancerWorkFileService.abstractBamFileService)
        linkFilesToFinalDestinationService.wgbsAlignmentWorkFileService = wgbsAlignmentWorkFileService
    }

    void cleanup() {
        configService.clean()
    }

    void "test getUnusedResultFiles"(int countTmpFiles, int countTmpDir, boolean wgbs) {
        given:
        setupData()
        if (wgbs) {
            linkNewResults_methylation_setup()
        } else {
            CreateRoddyFileHelper.createRoddyAlignmentResultFiles(panCancerWorkFileService, roddyBamFile)
        }

        List<Path> filesNotToBeCalledFor = [
                panCancerWorkFileService.getBamFile(roddyBamFile),
                panCancerWorkFileService.getBaiFile(roddyBamFile),
                panCancerWorkFileService.getMd5sumFile(roddyBamFile),
                panCancerWorkFileService.getQADirectory(roddyBamFile),
                panCancerWorkFileService.getExecutionStoreDirectory(roddyBamFile),
        ]

        if (wgbs) {
            filesNotToBeCalledFor += [wgbsAlignmentWorkFileService.getMethylationDirectory(roddyBamFile),
                                      wgbsAlignmentWorkFileService.getMetadataTableFile(roddyBamFile),
            ]
        }

        List<Path> tmpFiles = []
        countTmpFiles.times {
            tmpFiles << Files.createTempFile(panCancerWorkFileService.getDirectoryPath(roddyBamFile), "tmp", ".tmp")
        }
        assert countTmpFiles == tmpFiles.size()

        List<Path> tmpDirectories = []
        countTmpDir.times {
            Path file = panCancerWorkFileService.getDirectoryPath(roddyBamFile).resolve(HelperUtils.uniqueString)
            Files.createDirectories(file)
            tmpDirectories << file
        }
        assert countTmpDir == tmpDirectories.size()
        assert (filesNotToBeCalledFor + tmpFiles + tmpDirectories) as Set == Files.list(panCancerWorkFileService.getDirectoryPath(roddyBamFile)).toList() as Set

        when:
        List<Path> files = linkFilesToFinalDestinationService.getUnusedResultFiles(roddyBamFile)

        then:
        filesNotToBeCalledFor.every {
            !files.contains(it)
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

        List<Path> linkedFiles = createLinkedFilesList()

        linkFilesToFinalDestinationService.fileService = Mock(FileService)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        linkedFiles.each {
            1 * linkFilesToFinalDestinationService.fileService.createLink(it, _, _, _) >> { Path link, x, y, z -> }
        }
        0 * linkFilesToFinalDestinationService.fileService._
    }

    void "test linkNewResults, methylation one library, all fine"() {
        given:
        setupData()
        linkNewResults_methylation_setup()

        List<Path> linkedFiles = createLinkedFilesList()
        linkedFiles.add(wgbsAlignmentLinkFileService.getMergedMethylationDirectory(roddyBamFile))
        linkedFiles.add(wgbsAlignmentLinkFileService.getMetadataTableFile(roddyBamFile))

        linkFilesToFinalDestinationService.fileService = Mock(FileService)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        linkedFiles.each {
            1 * linkFilesToFinalDestinationService.fileService.createLink(it, _, _, _) >> { Path link, x, y, z -> }
        }
        0 * linkFilesToFinalDestinationService.fileService._
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

        List<Path> linkedFiles = createLinkedFilesList()
        linkedFiles.add(wgbsAlignmentLinkFileService.getMergedMethylationDirectory(roddyBamFile))
        linkedFiles.add(wgbsAlignmentLinkFileService.getMetadataTableFile(roddyBamFile))
        linkedFiles.addAll(wgbsAlignmentLinkFileService.getLibraryMethylationDirectories(roddyBamFile).values())
        linkedFiles.addAll(wgbsAlignmentLinkFileService.getLibraryQADirectories(roddyBamFile).values())

        linkFilesToFinalDestinationService.fileService = Mock(FileService)

        when:
        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile)

        then:
        linkedFiles.each {
            1 * linkFilesToFinalDestinationService.fileService.createLink(it, _, _, _) >> { Path link, x, y, z -> }
        }
        0 * linkFilesToFinalDestinationService.fileService._
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

        CreateRoddyFileHelper.createRoddyAlignmentResultFiles(wgbsAlignmentWorkFileService, roddyBamFile)
    }

    private List<Path> createLinkedFilesList() {
        return [
                panCancerLinkFileService.getBamFile(roddyBamFile),
                panCancerLinkFileService.getBaiFile(roddyBamFile),
                panCancerLinkFileService.getMd5sumFile(roddyBamFile),
                panCancerLinkFileService.getMergedQADirectory(roddyBamFile),
                panCancerLinkFileService.getExecutionDirectories(roddyBamFile),
                panCancerLinkFileService.getSingleLaneQADirectories(roddyBamFile).values(),
        ].flatten()
    }

    void "getOldAdditionalResults, should be fine when all bam files of work package exist"() {
        given:
        setupData()
        DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        assert roddyBamFile.workDirectory.exists()

        when:
        List<Path> files = linkFilesToFinalDestinationService.getOldAdditionalResults(roddyBamFile2)

        then:
        files*.toString().contains(panCancerWorkFileService.getDirectoryPath(roddyBamFile).toString())
        !files*.toString().contains(roddyBamFile2.workDirectory.toString())
        files*.toString().contains(panCancerLinkFileService.getExecutionStoreDirectory(roddyBamFile).toString())
        files*.toString().contains(panCancerLinkFileService.getQADirectory(roddyBamFile).toString())
    }

    void 'getOldAdditionalResults, should return all bam files paths, when work packages exist in old structure and latest is old is #latestIsOld'() {
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
        List<Path> files = linkFilesToFinalDestinationService.getOldAdditionalResults(roddyBamFile2)

        then:
        !files*.toString().contains(roddyBamFile2.workDirectory.toString())
        panCancerLinkFileService.getExecutionDirectories(roddyBamFile).every {
            it.toString().startsWithAny(*(files*.toString()))
        }
        panCancerLinkFileService.getSingleLaneQADirectories(roddyBamFile).values().every {
            it.toString().startsWithAny(*(files*.toString()))
        }

        where:
        latestIsOld << [true, false]
    }

    void "test getOldAdditionalResults, withoutOtherBamFilesOfTheSameWorkPackage, allFine"() {
        given:
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        List<Path> files = linkFilesToFinalDestinationService.getOldAdditionalResults(roddyBamFile)

        then:
        !files*.toString().contains(roddyBamFile.workDirectory.toString())
    }

    void "test getOldAdditionalResults, bamFileIsNull, shouldFail"() {
        given:
        setupData()

        when:
        linkFilesToFinalDestinationService.getOldAdditionalResults(null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("roddyBamFile")
    }

    void "test getOldAdditionalResults, bamHasOldStructure, shouldFail"() {
        given:
        setupData()
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true)

        when:
        linkFilesToFinalDestinationService.getOldAdditionalResults(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("isOldStructureUsed")
    }
}
