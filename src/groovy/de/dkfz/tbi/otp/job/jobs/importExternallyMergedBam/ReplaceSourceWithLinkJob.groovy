package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import java.nio.file.*

@Component
@Scope("prototype")
@UseJobLog
class ReplaceSourceWithLinkJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    FileService fileService


    @Override
    void execute() throws Exception {
        final ImportProcess importProcess = getProcessParameterObject()
        if (importProcess.replaceSourceWithLink) {
            importProcess.externallyProcessedMergedBamFiles.each { ExternallyProcessedMergedBamFile epmbf ->
                Realm realm = epmbf.project.realm

                File sourceBam = new File(epmbf.importedFrom)
                File sourceBaseDir = sourceBam.parentFile
                File sourceBai = new File(sourceBaseDir, epmbf.baiFileName)

                File targetBam = epmbf.bamFile
                File targetBai = epmbf.baiFile
                File targetBaseDir = epmbf.getImportFolder()

                Map linkMap = [:]
                createLinkMap(sourceBam, targetBam, linkMap)
                createLinkMap(sourceBai, targetBai, linkMap)


                epmbf.furtherFiles.each { String relativePath ->
                    File sourceFurtherFile = new File(sourceBaseDir, relativePath)
                    File targetFurtherFile = new File(targetBaseDir, relativePath)
                    createLinkMap(sourceFurtherFile, targetFurtherFile, linkMap)
                }

                Map filteredMap = linkMap.findAll { Path link, Path target ->
                    link != target
                }.collectEntries { Path link, Path target ->
                    [(fileService.toFile(link)): fileService.toFile(target)]
                }

                if (filteredMap) {
                    linkFileUtils.createAndValidateLinks(filteredMap, realm)
                }
            }
        }

        ImportProcess.withTransaction {
            importProcess.state = ImportProcess.State.FINISHED
            importProcess.save(flush: true)
        }
        succeed()
    }

    private void createLinkMap(File source, File target, Map linkMap) {
        FileSystem fs = fileSystemService.filesystemForBamImport
        createLinkMap(fs.getPath(source.absolutePath).toRealPath(), fs.getPath(target.absolutePath).toRealPath(), linkMap)
    }

    private void createLinkMap(Path source, Path target, Map linkMap) {
        if (Files.isSymbolicLink(source)) {
            createLinkMap(source.toRealPath(), target, linkMap)
        } else if (Files.isRegularFile(source)) {
            linkMap[target] = source
        } else if (Files.isDirectory(source)) {
            Files.newDirectoryStream(source).toList().each { Path sourceChild ->
                createLinkMap(sourceChild, target.resolve(sourceChild.fileName), linkMap)
            }
        } else if (!Files.exists(source)) {
            throw new RuntimeException("No file exists for name: ${source}")
        } else {
            throw new RuntimeException('Unknown file type, neither regular file, nor symbolic link nor directory')
        }
    }
}
