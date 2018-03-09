package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

import java.nio.file.*

@Ignore
class ImportExternallyMergedBamWorkflowTests extends WorkflowTestCase {

    @Autowired
    LinkFileUtils linkFileUtils

    ImportProcess importProcess


    private final static String FURTHER_FILE_NAME = "furtherFile.txt"

    private final static String DIRECTORY1 = "directory1"
    private final static String DIRECTORY2 = "directory2"
    private final static String SUBDIRECTORY11 = "${DIRECTORY1}/subdirectory1"
    private final static String SUBDIRECTORY21 = "${DIRECTORY2}/subdirectory1"
    private final static String SUBDIRECTORY22 = "${DIRECTORY2}/subdirectory2"
    private final static String SUBSUBDIRECTORY221 = "${SUBDIRECTORY22}/subsubdirectory1"

    private final static String FURTHER_FILE_NAME_DIRECTORY1 = "${DIRECTORY1}/${FURTHER_FILE_NAME}"
    private final static String FURTHER_FILE_NAME_DIRECTORY2 = "${DIRECTORY2}/${FURTHER_FILE_NAME}"
    private final static String FURTHER_FILE_NAME_SUBDIRECTORY11 = "${SUBDIRECTORY11}/${FURTHER_FILE_NAME}"
    private final static String FURTHER_FILE_NAME_SUBDIRECTORY21 = "${SUBDIRECTORY21}/${FURTHER_FILE_NAME}"
    private final static String FURTHER_FILE_NAME_SUBDIRECTORY22 = "${SUBDIRECTORY22}/${FURTHER_FILE_NAME}"
    private final static String FURTHER_FILE_NAME_SUBSUBDIRECTORY221 = "${SUBSUBDIRECTORY221}/${FURTHER_FILE_NAME}"


    private final static List<String> ALL_FILES = [
            FURTHER_FILE_NAME,
            FURTHER_FILE_NAME_DIRECTORY1,
            FURTHER_FILE_NAME_DIRECTORY2,
            FURTHER_FILE_NAME_SUBDIRECTORY11,
            FURTHER_FILE_NAME_SUBDIRECTORY21,
            FURTHER_FILE_NAME_SUBDIRECTORY22,
            FURTHER_FILE_NAME_SUBSUBDIRECTORY221,
    ].asImmutable()


    private ExternallyProcessedMergedBamFile createFile(Project project, String nameInfix, boolean useLink = false) {
        File baseDir = new File(ftpDir, nameInfix)
        File targetDir = useLink ? new File(baseDir, 'target') : baseDir

        String bamFileName = "bamFile${nameInfix}.bam"
        String baiFileName = "${bamFileName}.bai"

        List<String> fileNames = [
                bamFileName,
                baiFileName,
        ] + ALL_FILES

        createFilesWithContent(fileNames.collectEntries {
            [(new File(targetDir, it)): it]
        })

        if (useLink) {
            linkFileUtils.createAndValidateLinks([
                    (bamFileName)                 : bamFileName,
                    (baiFileName)                 : baiFileName,
                    (FURTHER_FILE_NAME)           : FURTHER_FILE_NAME,
                    (DIRECTORY1)                  : DIRECTORY1,
                    (FURTHER_FILE_NAME_DIRECTORY2): FURTHER_FILE_NAME_DIRECTORY2,
                    (SUBDIRECTORY21)              : SUBDIRECTORY21,
                    (SUBDIRECTORY22)              : SUBDIRECTORY22,
            ].collectEntries { key, value ->
                [(new File(targetDir, key)): new File(baseDir, value)]
            }, realm)
        }

        File bamFile = new File(baseDir, bamFileName)
        ExternallyProcessedMergedBamFile epmbf = DomainFactory.createExternallyProcessedMergedBamFile(
                importedFrom: bamFile.path,
                fileName: bamFileName,
                fileSize: bamFile.size(),
                furtherFiles: [
                        FURTHER_FILE_NAME,
                        DIRECTORY1,
                        DIRECTORY2,
                ]
        )
        epmbf.individual.project = project
        assert epmbf.individual.save(flush: true)
        return epmbf
    }

    @Before
    void setUp() {
        Project project = DomainFactory.createProject(realm: realm)
        createDirectories([new File(ftpDir), project.projectDirectory])

        ExternallyProcessedMergedBamFile epmbf01 = createFile(project, '1', false)
        ExternallyProcessedMergedBamFile epmbf02 = createFile(project, '2', true)

        importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbf01, epmbf02],
                state: ImportProcess.State.NOT_STARTED,
                replaceSourceWithLink: true
        ).save(flush: true)
    }

    @Test
    void testImportProcess_FilesHaveToBeCopied() {
        importProcess.replaceSourceWithLink = false
        importProcess.save(flush: true)

        execute()
        checkThatFileCopyingWasSuccessful(importProcess)

        Thread.sleep(1000) //needs a sleep, otherwise the file system cache has not yet the new value
        importProcess.externallyProcessedMergedBamFiles.each {
            File baseDirSource = new File(it.importedFrom).parentFile
            File baseDirTarget = it.importFolder

            [
                    it.bamFileName,
                    it.baiFileName,
                    it.furtherFiles,
            ].flatten().each {
                Path source = new File(baseDirSource, it).toPath()
                Path target = new File(baseDirTarget, it).toPath()
                assert !Files.isSymbolicLink(source) || Files.readSymbolicLink(source) != target
            }
        }
    }

    @Test
    void testImportProcess_FilesHaveToBeLinkedAndDeleted() {
        execute()

        checkThatFileCopyingWasSuccessful(importProcess)

        Thread.sleep(1000) //needs a sleep, otherwise the file system cache has not yet the new value
        importProcess.externallyProcessedMergedBamFiles.each {
            File baseDirSource = new File(it.importedFrom).parentFile
            File baseDirTarget = it.importFolder

            [
                    it.bamFileName,
                    it.baiFileName,
                    ALL_FILES,
            ].flatten().each {
                Path source = new File(baseDirSource, it).toPath()
                Path target = new File(baseDirTarget, it).toPath()
                assert Files.isSymbolicLink(source)
                assert source.toRealPath() == target
            }
        }
    }

    private void checkThatFileCopyingWasSuccessful(ImportProcess importProcess) {
        ImportProcess.withNewSession {
            importProcess = ImportProcess.get(importProcess.id)
            assert importProcess.state == ImportProcess.State.FINISHED
            assert 2 == importProcess.externallyProcessedMergedBamFiles.size()

            importProcess.externallyProcessedMergedBamFiles.each {
                it.refresh()
                assert it.fileSize > 0
                File baseDirectory = it.importFolder

                [
                        it.bamFileName,
                        it.baiFileName,
                        ALL_FILES
                ].flatten().each {
                    checkThatFileExistAndIsNotLink(new File(baseDirectory, it))
                }

                [
                        DIRECTORY1,
                        DIRECTORY2,
                        SUBDIRECTORY21,
                        SUBDIRECTORY22,
                ].each {
                    checkThatDirectoryExistAndIsNotLink(new File(baseDirectory, it))
                }
            }
        }
    }

    private void checkThatDirectoryExistAndIsNotLink(File file) {
        Path path = file.toPath()
        assert Files.exists(path, LinkOption.NOFOLLOW_LINKS)
        assert Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
    }

    private void checkThatFileExistAndIsNotLink(File file) {
        Path path = file.toPath()
        assert Files.exists(path, LinkOption.NOFOLLOW_LINKS)
        assert Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        assert Files.size(path) > 0
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
