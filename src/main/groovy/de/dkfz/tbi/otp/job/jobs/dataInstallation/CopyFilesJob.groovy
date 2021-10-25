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
package de.dkfz.tbi.otp.job.jobs.dataInstallation

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

@Component
@Scope("prototype")
@Slf4j
class CopyFilesJob extends AbstractOtpJob implements AutoRestartableJob {


    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    FileService fileService

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        assert seqTrack : "No seqTrack found for id ${Long.parseLong(getProcessParameterValue())}."

        Realm realm = seqTrack.project.realm

        checkInitialSequenceFiles(seqTrack)

        NextAction returnValue

        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        seqTrack.dataFiles.each { DataFile dataFile ->
            File initialFile = new File(lsdfFilesService.getFileInitialPath(dataFile))
            File finalFile = new File(lsdfFilesService.getFileFinalPath(dataFile))

            String md5SumFileName = checksumFileService.md5FileName(dataFile)

            Path initialPath = fileService.toPath(initialFile, fileSystem)
            Path finalPath = fileService.toPath(finalFile, fileSystem)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(finalPath.parent, realm, seqTrack.project.unixGroup)

            if (seqTrack.linkedExternally) {
                fileService.createLink(finalPath, initialPath, realm, CreateLinkOption.DELETE_EXISTING_FILE)
                returnValue = NextAction.SUCCEED
            } else {
                String cmd = """
cd ${finalFile.parent}
if [ -e "${finalFile.path}" ]; then
    echo "File ${finalFile.path} already exists."
    rm ${finalFile.path}*
fi
cp ${initialFile} ${finalFile}
md5sum ${finalFile.name} > ${md5SumFileName}
chgrp -h ${seqTrack.project.unixGroup} ${finalFile} ${md5SumFileName}
chmod 440 ${finalFile} ${md5SumFileName}
"""
                clusterJobSchedulerService.executeJob(realm, cmd)
                returnValue = NextAction.WAIT_FOR_CLUSTER_JOBS
            }
        }
        if (returnValue == NextAction.SUCCEED) {
            validate()
        }
        return returnValue
    }

    @Override
    protected final void validate() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))

        final Collection<String> problems = seqTrack.dataFiles.collect { DataFile dataFile ->
            String targetFile = lsdfFilesService.getFileFinalPath(dataFile)
            try {
                FileSystem fs = seqTrack.linkedExternally ?
                        fileSystemService.filesystemForFastqImport :
                        fileSystemService.filesystemForProcessingForRealm
                Path targetPath = fs.getPath(targetFile)

                fileService.ensureFileIsReadableAndNotEmpty(targetPath)
                if (!seqTrack.linkedExternally) {
                    assert checksumFileService.compareMd5(dataFile)
                }

                dataFile.fileSize = Files.size(targetPath)
                dataFile.dateFileSystem = new Date(Files.getLastModifiedTime(targetPath).toMillis())
                dataFile.fileExists = true
                assert dataFile.save(flush: true)

                return null
            } catch (Throwable t) {
                return "Copying or linking of targetFile ${targetFile} from dataFile ${dataFile} failed, ${t.message}"
            }
        }.findAll()
        if (problems) {
            throw new ProcessingException(problems.join(","))
        }
    }

    protected void checkInitialSequenceFiles(SeqTrack seqTrack) {
        List<DataFile> dataFiles = seqTrack.dataFiles
        if (!dataFiles) {
            throw new ProcessingException("No files in processing for seqTrack ${seqTrack}")
        }
        final Collection<String> missingPaths = []
        for (DataFile file in dataFiles) {
            String path = lsdfFilesService.getFileInitialPath(file)
            FileSystem fs = fileSystemService.filesystemForFastqImport
            if (!FileService.isFileReadableAndNotEmpty(fs.getPath(path))) {
                missingPaths.add(path)
            }
        }
        if (!missingPaths.empty) {
            throw new ProcessingException("The following ${missingPaths.size()} files are missing:\n${missingPaths.join("\n")}")
        }
    }
}
