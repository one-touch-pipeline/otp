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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.ChecksumFileService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.*

@Component
@Scope("prototype")
@Slf4j
class ImportExternallyMergedBamJob extends AbstractOtpJob {

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    FileService fileService

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final ImportProcess importProcess = processParameterObject
        return importProcess.linkOperation.linkSource ? linkSource(importProcess) : copyFiles(importProcess)
    }

    private NextAction linkSource(ImportProcess importProcess) throws Throwable {
        FileSystem fileSystem = fileSystemService.filesystemForBamImport
        importProcess.externallyProcessedMergedBamFiles.each { ExternallyProcessedMergedBamFile epmbf ->
            Realm realm = epmbf.realm
            Path sourceBaseDir = fileSystem.getPath(epmbf.importedFrom).parent
            Path targetBaseDir = fileSystem.getPath(epmbf.importFolder.absolutePath)

            linkMissingFiles(sourceBaseDir, targetBaseDir, epmbf.bamFileName, realm)
            linkMissingFiles(sourceBaseDir, targetBaseDir, epmbf.baiFileName, realm)

            epmbf.furtherFiles.each { String relativePath ->
                linkMissingFiles(sourceBaseDir, targetBaseDir, relativePath, realm)
            }
        }
        validate()
        return NextAction.SUCCEED
    }

    private void linkMissingFiles(Path sourceBaseDir, Path targetBaseDir, String pathToLink, Realm realm) {
        Path source = sourceBaseDir.resolve(pathToLink)
        Path target = targetBaseDir.resolve(pathToLink)
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            fileService.createLink(target, source, realm, CreateLinkOption.ABSOLUTE)
        }
    }

    @SuppressWarnings('JavaIoPackageAccess') //method is about files
    private NextAction copyFiles(ImportProcess importProcess) throws Throwable {
        NextAction action = NextAction.SUCCEED

        String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String samtoolsActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_SAMTOOLS)
        String groovyActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY)
        String samtoolsCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_SAMTOOLS)
        String groovyCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_GROOVY)
        File otpScriptDir = configService.toolsPath

        importProcess.externallyProcessedMergedBamFiles.each { ExternallyProcessedMergedBamFile epmbf ->
            Realm realm = epmbf.project.realm
            File sourceBam = new File(epmbf.importedFrom)
            File sourceBaseDir = sourceBam.parentFile
            File sourceBai = new File(sourceBaseDir, epmbf.baiFileName)

            File targetBam = epmbf.bamFile
            File targetBai = epmbf.baiFile
            File targetBaseDir = epmbf.importFolder
            File checkpoint = new File(targetBaseDir, ".${epmbf.bamFileName}.checkpoint")

            String updateBaseDir = "sed -e 's#${sourceBaseDir}#${targetBaseDir}#'"

            if (checkpoint.exists()) {
                log.debug("Checkpoint found for ${sourceBam}, skip copying")
            } else {
                action = NextAction.WAIT_FOR_CLUSTER_JOBS
                String md5sumBam
                if (epmbf.md5sum) {
                    md5sumBam = "echo ${epmbf.md5sum}  ${targetBam} > ${targetBam}.md5sum"
                } else {
                    md5sumBam = "md5sum ${sourceBam} | ${updateBaseDir} > ${targetBam}.md5sum"
                }

                String furtherFilesSource = epmbf.furtherFiles.collect {
                    new File(sourceBaseDir, it)
                }.join(' ')
                String furtherFilesTarget = epmbf.furtherFiles.collect {
                    new File(targetBaseDir, it)
                }.join(' ')

                String furtherFilesCopy = epmbf.furtherFiles.collect { String relativePath ->
                    File sourceFurtherFile = new File(sourceBaseDir, relativePath)
                    File targetFurtherFile = new File(targetBaseDir, relativePath)
                    return "mkdir -p -m 2750 ${targetFurtherFile.parent}\n" +
                            "cp -HLR ${sourceFurtherFile} ${targetFurtherFile}"
                }.join("\n")

                String cmd = """
set -o pipefail
set -v

${moduleLoader}
${samtoolsActivation}
${groovyActivation}

if [ -e "${targetBam.path}" ]; then
    echo "File ${targetBam.path} already exists."
    rm -rf ${targetBam.path}* ${furtherFilesTarget}
fi

mkdir -p -m 2750 ${targetBam.parent}
# copy and calculate max read length at the same time
cat ${sourceBam} | tee ${targetBam} | ${samtoolsCommand} view - | ${groovyCommand} ${otpScriptDir}/bamMaxReadLength.groovy > ${epmbf.bamMaxReadLengthFile}
cp -HL ${sourceBai} ${targetBai}

${furtherFilesCopy}

cd ${targetBam.parent}
${md5sumBam}
md5sum -c ${targetBam}.md5sum

md5sum ${sourceBai} | ${updateBaseDir} > ${targetBai}.md5sum
md5sum -c ${targetBai}.md5sum

md5sum `find -L ${furtherFilesSource} -type f` | ${updateBaseDir} > ${targetBaseDir}/md5sum.md5sum
md5sum -c ${targetBaseDir}/md5sum.md5sum

chgrp -hR ${executionHelperService.getGroup(epmbf.project.realm, epmbf.project.projectDirectory)} ${targetBaseDir}
chmod 644 `find ${targetBaseDir} -type f`
chmod 750 `find ${targetBaseDir} -type d`

touch ${checkpoint}
"""
                clusterJobSchedulerService.executeJob(realm, cmd)
            }
        }
        if (action == NextAction.SUCCEED) {
            validate()
        }
        return action
    }

    @Override
    protected void validate() throws Throwable {
        final ImportProcess importProcess = processParameterObject
        importProcess.linkOperation.linkSource ? validateLink(importProcess) : validateCopy(importProcess)
    }

    private void validateLink(ImportProcess importProcess) throws Throwable {
        FileSystem fileSystem = fileSystemService.filesystemForBamImport
        importProcess.externallyProcessedMergedBamFiles.each { ExternallyProcessedMergedBamFile bamFile ->
            Path sourceBaseDir = fileSystem.getPath(bamFile.importedFrom).parent
            Path targetBaseDir = fileSystem.getPath(bamFile.importFolder.absolutePath)

            checkLink(sourceBaseDir, targetBaseDir, bamFile.bamFileName)
            checkLink(sourceBaseDir, targetBaseDir, bamFile.baiFileName)

            bamFile.furtherFiles.each { String relativePath ->
                checkLink(sourceBaseDir, targetBaseDir, relativePath)
            }

            fillBamFile(bamFile, targetBaseDir.resolve(bamFile.bamFileName))
        }
    }

    private void checkLink(Path sourceBaseDir, Path targetBaseDir, String pathToLink) {
        Path source = sourceBaseDir.resolve(pathToLink)
        Path target = targetBaseDir.resolve(pathToLink)
        assert Files.exists(target, LinkOption.NOFOLLOW_LINKS)
        assert Files.isSymbolicLink(target)
        assert target.toRealPath() == source.toRealPath()
    }

    private void validateCopy(ImportProcess importProcess) throws Throwable {
        FileSystem fs = fileSystemService.filesystemForBamImport

        final Collection<String> problems = importProcess.externallyProcessedMergedBamFiles.collect {
            String path = it.bamFile.path
            Path target = fs.getPath(path)
            Path targetBai = fs.getPath(it.baiFile.path)

            try {
                FileService.ensureFileIsReadableAndNotEmpty(target)
                FileService.ensureFileIsReadableAndNotEmpty(targetBai)

                if (!it.maximumReadLength) {
                    Path maxReadLengthPath = fs.getPath(it.bamMaxReadLengthFile.absolutePath)
                    FileService.ensureFileIsReadableAndNotEmpty(maxReadLengthPath)
                    it.maximumReadLength = maxReadLengthPath.text as Integer
                }

                if (!it.md5sum) {
                    Path md5Path = fs.getPath(checksumFileService.md5FileName(path))
                    FileService.ensureFileIsReadableAndNotEmpty(md5Path)
                    it.md5sum = checksumFileService.firstMD5ChecksumFromFile(md5Path)
                }

                fillBamFile(it, target)

                return null
            } catch (Throwable t) {
                log.error(t.message, t)
                return "Copying of target ${target} failed, ${t.message}"
            }
        }.findAll()
        if (problems) {
            throw new ProcessingException(problems.join(","))
        }
}

    private void fillBamFile(ExternallyProcessedMergedBamFile bamFile, Path bamFilePath) {
        bamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        bamFile.fileSize = Files.size(bamFilePath)
        bamFile.fileExists = true
        assert bamFile.save(flush: true)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        assert bamFile.workPackage.save(flush: true)
    }
}
