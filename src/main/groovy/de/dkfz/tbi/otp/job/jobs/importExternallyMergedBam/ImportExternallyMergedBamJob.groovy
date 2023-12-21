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
package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
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
import de.dkfz.tbi.otp.project.ProjectService

import java.nio.file.*

@CompileDynamic
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

    @Autowired
    ProjectService projectService

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final ImportProcess importProcess = processParameterObject
        return importProcess.linkOperation.linkSource ? linkSource(importProcess) : copyFiles(importProcess)
    }

    private NextAction linkSource(ImportProcess importProcess) throws Throwable {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        importProcess.externallyProcessedBamFiles.each { ExternallyProcessedBamFile epmbf ->
            Path targetBaseDir = fileSystem.getPath(epmbf.importedFrom).parent
            Path linkBaseDir = fileSystem.getPath(epmbf.importFolder.absolutePath)

            linkMissingFiles(targetBaseDir, linkBaseDir, epmbf.bamFileName)
            linkMissingFiles(targetBaseDir, linkBaseDir, epmbf.baiFileName)

            epmbf.furtherFiles.each { String relativePath ->
                linkMissingFiles(targetBaseDir, linkBaseDir, relativePath)
            }
        }
        validate()
        return NextAction.SUCCEED
    }

    private void linkMissingFiles(Path targetBaseDir, Path linkBaseDir, String pathToLink) {
        Path target = targetBaseDir.resolve(pathToLink)
        Path link = linkBaseDir.resolve(pathToLink)
        if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
            fileService.createLink(link, target, CreateLinkOption.ABSOLUTE)
        }
    }

    @SuppressWarnings("LineLength") // suppressed because breaking the line would break the commands
    @SuppressWarnings('JavaIoPackageAccess') // method is about files
    private NextAction copyFiles(ImportProcess importProcess) throws Throwable {
        NextAction action = NextAction.SUCCEED

        String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String samtoolsActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_SAMTOOLS)
        String groovyActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY)
        String samtoolsCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_SAMTOOLS)
        String groovyCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_GROOVY)
        File otpScriptDir = fileService.toFile(configService.toolsPath)

        importProcess.externallyProcessedBamFiles.each { ExternallyProcessedBamFile epmbf ->
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

                String furtherFilesMd5sumCheck = furtherFilesSource ? """\
md5sum `find -L ${furtherFilesSource} -type f` | ${updateBaseDir} > ${targetBaseDir}/md5sum.md5sum
md5sum -c ${targetBaseDir}/md5sum.md5sum\
""".stripIndent() : ""

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

${furtherFilesMd5sumCheck}

chgrp -hR ${executionHelperService.getGroup(new File(projectService.getProjectDirectory(epmbf.project).toString()))} ${targetBaseDir}
find ${targetBaseDir} -type d -not -perm 2750 -print -exec chmod 2750 '{}' \\;
find ${targetBaseDir} -type f -not -perm 440 -not -name "*.bam" -not -name "*.bai" -not -name ".roddyExecCache.txt" -not -name "zippedAnalysesMD5.txt" -print -exec chmod 440 '{}' \\;
find ${targetBaseDir} -type f -not -perm 444 \\( -name "*.bam" -or -name "*.bai" \\) -print -exec chmod 444 '{}' \\;

touch ${checkpoint}
"""
                clusterJobSchedulerService.executeJob(cmd)
            }
        }
        if (action == NextAction.SUCCEED) {
            validate()
        }
        return action
    }

    @Override
    @Transactional
    protected void validate() throws Throwable {
        final ImportProcess importProcess = processParameterObject
        importProcess.linkOperation.linkSource ? validateLink(importProcess) : validateCopy(importProcess)
    }

    private void validateLink(ImportProcess importProcess) throws Throwable {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        importProcess.externallyProcessedBamFiles.each { ExternallyProcessedBamFile bamFile ->
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
        FileSystem fs = fileSystemService.remoteFileSystem

        final Collection<String> problems = importProcess.externallyProcessedBamFiles.collect {
            String path = it.bamFile.path
            Path target = fs.getPath(path)
            Path targetBai = fs.getPath(it.baiFile.path)

            try {
                FileService.ensureFileIsReadableAndNotEmptyStatic(target)
                FileService.ensureFileIsReadableAndNotEmptyStatic(targetBai)

                if (!it.maximumReadLength) {
                    Path maxReadLengthPath = fs.getPath(it.bamMaxReadLengthFile.absolutePath)
                    FileService.ensureFileIsReadableAndNotEmptyStatic(maxReadLengthPath)
                    it.maximumReadLength = maxReadLengthPath.text as Integer
                }

                if (!it.md5sum) {
                    Path md5Path = fs.getPath(checksumFileService.md5FileName(path))
                    FileService.ensureFileIsReadableAndNotEmptyStatic(md5Path)
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

    private void fillBamFile(ExternallyProcessedBamFile bamFile, Path bamFilePath) {
        bamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
        bamFile.fileSize = Files.size(bamFilePath)
        assert bamFile.save(flush: true)

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        assert bamFile.workPackage.save(flush: true)
    }
}
