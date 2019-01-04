package de.dkfz.tbi.otp.dataprocessing.cellRanger


import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

@Mock([
        AbstractMergedBamFile,
        CellRangerMergingWorkPackage,
        CellRangerConfig,
        DataFile,
        Individual,
        LibraryPreparationKit,
        FileType,
        MergingCriteria,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        ReferenceGenomeIndex,
        Run,
        RunSegment,
        SeqTrack,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqType,
        SingleCellBamFile,
        SoftwareTool,
        ToolName,
])
class CellRangerWorkflowServiceSpec extends Specification implements CellRangerFactory {


    @Rule
    TemporaryFolder temporaryFolder


    void "linkResultFiles, if all target files exist, then create link for each"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        createResultFiles(singleCellBamFile)

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        when:
        service.linkResultFiles(singleCellBamFile)

        then:
        singleCellBamFile.getLinkedResultFiles().each {
            Path path = it.toPath()
            assert Files.exists(path, LinkOption.NOFOLLOW_LINKS)
            assert Files.isSymbolicLink(path)
            Path link = Files.readSymbolicLink(path)
            assert !link.isAbsolute()
        }
    }


    @Unroll
    void "linkResultFiles, if file/directory '#missingFile' does not exist, then throw an assert"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        createResultFiles(singleCellBamFile)

        Path result = singleCellBamFile.resultDirectory.toPath()

        new FileService().deleteDirectoryRecursively(result.resolve(missingFile))

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        when:
        service.linkResultFiles(singleCellBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(missingFile)

        where:
        missingFile << SingleCellBamFile.CREATED_RESULT_FILES_AND_DIRS
    }

    void "cleanupOutputDirectory, when other files/directories exists in the result directory, delete them, but not the results"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()

        File outputDirectory = singleCellBamFile.outputDirectory
        File resultDirectory = singleCellBamFile.resultDirectory

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : new FileService(),
        ])

        and: 'create result files/directories'
        createResultFiles(singleCellBamFile)

        and: 'create additional directory'
        File subDir = new File(outputDirectory, 'subDir')
        assert subDir.mkdir()

        and: 'create additional file'
        File file = new File(outputDirectory, 'file')
        CreateFileHelper.createFile(file)

        when:
        service.cleanupOutputDirectory(singleCellBamFile)

        then: 'additional files/directories deleted'
        !subDir.exists()
        !file.exists()

        and: 'result are not deleted'
        singleCellBamFile.outputDirectory.exists()
        SingleCellBamFile.CREATED_RESULT_FILES_AND_DIRS.each {
            assert new File(resultDirectory, it).exists()
        }
    }

    void "cleanupOutputDirectory, call correctPathPermissionRecursive for the work directory"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())

        SingleCellBamFile singleCellBamFile = createBamFile()
        Path workDirectory = singleCellBamFile.workDirectory.toPath()

        CellRangerWorkflowService service = new CellRangerWorkflowService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService      : Mock(FileService) {
                    1 * correctPathPermissionRecursive(workDirectory)
                    0 * _
                },
        ])

        when:
        service.correctFilePermissions(singleCellBamFile)

        then:
        noExceptionThrown()
    }
}
