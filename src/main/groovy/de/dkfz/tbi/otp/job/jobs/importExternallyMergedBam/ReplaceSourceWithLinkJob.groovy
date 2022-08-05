/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import grails.gorm.transactions.NotTransactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LinkFileUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.*

@Component
@Scope("prototype")
@Slf4j
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

    @NotTransactional
    @Override
    void execute() throws Exception {
        SessionUtils.withNewSession {
            final ImportProcess importProcess = processParameterObject
            if (importProcess.linkOperation.replaceSourceWithLink) {
                importProcess.externallyProcessedMergedBamFiles.each { ExternallyProcessedMergedBamFile epmbf ->
                    Realm realm = epmbf.project.realm

                    File sourceBam = new File(epmbf.importedFrom)
                    File sourceBaseDir = sourceBam.parentFile
                    File sourceBai = new File(sourceBaseDir, epmbf.baiFileName)

                    File targetBam = epmbf.bamFile
                    File targetBai = epmbf.baiFile
                    File targetBaseDir = epmbf.importFolder

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
                        linkFileUtils.createAndValidateLinks(filteredMap, realm, epmbf.project.unixGroup)
                    }
                }
            }

            ImportProcess.withTransaction {
                importProcess.state = ImportProcess.State.FINISHED
                importProcess.save(flush: true)
            }
            succeed()
        }
    }

    protected void createLinkMap(File source, File target, Map linkMap) {
        FileSystem fs = fileSystemService.filesystemForBamImport
        createLinkMap(fs.getPath(source.absolutePath).toRealPath(), fs.getPath(target.absolutePath).toRealPath(), linkMap)
    }

    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    protected void createLinkMap(Path source, Path target, Map linkMap) {
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
