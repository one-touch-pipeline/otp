package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import org.junit.*

import java.nio.file.*

@Ignore
class ImportExternallyMergedBamWorkflowTests extends WorkflowTestCase {

    ImportProcess importProcess

    @Before
    void setUp() {

        String bamFileName01 = "bamFile01.bam"
        String bamFileName02 = "bamFile02.bam"
        String bamFilePath01 = "${ftpDir}/${bamFileName01}"
        String bamFilePath02 = "${ftpDir}/${bamFileName02}"

        String baiFileName01 = "bamFile01.bam.bai"
        String baiFileName02 = "bamFile02.bam.bai"
        String baiFilePath01 = "${ftpDir}/${baiFileName01}"
        String baiFilePath02 = "${ftpDir}/${baiFileName02}"

        String furtherFileName = "furtherFile.txt"
        String furtherFolderName = "quality"
        File furtherFolder = new File(ftpDir, furtherFolderName)
        File furtherFile = new File(furtherFolder, furtherFileName)

        createDirectories([
                new File(ftpDir),
                furtherFolder
        ])

        createFilesWithContent([
                (new File(bamFilePath01)): 'bam1',
                (new File(bamFilePath02)): 'bam2',
                (new File(baiFilePath01)): 'bai1',
                (new File(baiFilePath02)): 'bai2',
                (furtherFile): 'something other',
        ])

        Project project = DomainFactory.createProject(realmName: realm.name)
        createDirectories([project.projectDirectory])

        ExternallyProcessedMergedBamFile epmbf01 = DomainFactory.createExternallyProcessedMergedBamFile(
                importedFrom : bamFilePath01,
                fileName     : bamFileName01,
                fileSize     : new File(bamFilePath01).size(),
                furtherFiles: [furtherFolderName]
        )
        epmbf01.individual.project = project
        assert epmbf01.individual.save(flush: true)

        ExternallyProcessedMergedBamFile epmbf02 = DomainFactory.createExternallyProcessedMergedBamFile(
                importedFrom : bamFilePath02,
                fileName     : bamFileName02,
                fileSize     : new File(bamFilePath02).size(),
                furtherFiles: [furtherFolderName]
        )
        epmbf02.individual.project = project
        assert epmbf02.individual.save(flush: true)

        importProcess = new ImportProcess (
                externallyProcessedMergedBamFiles : [epmbf01, epmbf02],
                state                             : ImportProcess.State.NOT_STARTED,
                replaceSourceWithLink             : true
        ).save(flush: true)
    }

    @Test
    void testImportProcess_FilesHaveToBeCopied() {
        importProcess.replaceSourceWithLink = false
        importProcess.save(flush: true)

        execute()
        checkThatFileCopyingWasSuccessful(importProcess)

        importProcess.externallyProcessedMergedBamFiles.each {
            assert !Files.isSymbolicLink(new File(it.importedFrom).toPath())
        }
    }

    @Test
    void testImportProcess_FilesHaveToBeLinkedAndDeleted() {
        execute()

        checkThatFileCopyingWasSuccessful(importProcess)
        checkThatFileLinkingWasSuccessful(importProcess)
    }

    private void checkThatFileCopyingWasSuccessful(ImportProcess importProcess) {
        importProcess.refresh()
        assert importProcess.state == ImportProcess.State.FINISHED
        assert 2 == importProcess.externallyProcessedMergedBamFiles.size()

        importProcess.externallyProcessedMergedBamFiles.each {
            it.refresh()
            assert it.fileSize > 0
            assert it.getFilePath().absoluteDataManagementPath.exists()
        }
    }

    private void checkThatFileLinkingWasSuccessful(ImportProcess importProcess) {
        importProcess.refresh()
        assert importProcess.replaceSourceWithLink

        importProcess.externallyProcessedMergedBamFiles.each {
            File importedBamFile = new File(it.importedFrom)
            assert importedBamFile.exists() && Files.isSymbolicLink(importedBamFile.toPath()) && importedBamFile.canRead()
            assert Files.readSymbolicLink(importedBamFile.toPath()) == it.getFilePath().absoluteDataManagementPath.toPath()
        }
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/ImportExternallyMergedBamWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        return Duration.standardMinutes(20)
    }
}
