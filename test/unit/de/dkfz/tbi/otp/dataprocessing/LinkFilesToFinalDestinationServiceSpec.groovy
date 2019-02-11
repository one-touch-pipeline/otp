package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

@Mock([
        AbstractMergedBamFile,
        Comment,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        ProcessingOption,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyMergedBamQa,
        RoddyWorkflowConfig,
        RoddyQualityAssessment,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class LinkFilesToFinalDestinationServiceSpec extends Specification implements IsRoddy {

    @SuppressWarnings('JUnitPublicProperty')
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    void "linkToFinalDestinationAndCleanup, all Fine"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())
        final String md5sum = HelperUtils.randomMd5sum

        RoddyBamFile roddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                md5sum             : null,
                fileSize           : -1,
        ])
        Realm realm = roddyBamFile.realm

        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService([
                lsdfFilesService            : Mock(LsdfFilesService) {
                    1 * deleteFilesRecursive(realm, _) >> { Realm realm2, Collection<File> filesOrDirectories ->
                        String basePath = roddyBamFile.workDirectory.path
                        filesOrDirectories.each { File file ->
                            assert file.path.startsWith(basePath)
                        }
                    }
                },
                executeRoddyCommandService  : Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(roddyBamFile, realm)
                },
                qcTrafficLightCheckService  : Mock(QcTrafficLightCheckService) {
                    1 * handleQcCheck(roddyBamFile, _) >> { AbstractMergedBamFile bamFile, Closure callbackIfAllFine ->
                        bamFile.workDirectory.mkdirs()
                        bamFile.workBamFile.text = "something"
                    }
                },
                md5SumService               : Mock(Md5SumService) {
                    1 * extractMd5Sum(_ as Path) >> md5sum
                },
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    1 * setSamplePairStatusToNeedProcessing(roddyBamFile)
                },
        ])

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        then:
        roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum == md5sum
        roddyBamFile.fileSize > 0
        roddyBamFile.fileExists
        roddyBamFile.dateFromFileSystem != null
        roddyBamFile.dateFromFileSystem instanceof Date
    }
}
