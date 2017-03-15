package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

class ImportExternallyMergedBamJob extends AbstractOtpJob {

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    PbsService pbsService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService


    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        final ImportProcess importProcess = getProcessParameterObject()

        importProcess.externallyProcessedMergedBamFiles.each {
            Realm realm = configService.getRealmDataManagement(it.project)
            File sourceBam = new File(it.importedFrom)
            File targetBam = it.getFilePath().absoluteDataManagementPath

            File checkpoint = new File(targetBam.parent, ".${targetBam.name}.checkpoint")
            File sourceBai = new File("${sourceBam}.bai")
            File targetBai = new File("${targetBam}.bai")

            if (!checkpoint.exists()) {
                String md5sumBam
                if (it.md5sum) {
                    md5sumBam = "echo ${it.md5sum}  ${targetBam.name} > ${targetBam}.md5sum"
                } else {
                    md5sumBam = "md5sum ${sourceBam} > ${targetBam}.md5sum"
                }

                String cmd = """
#!/bin/bash

if [ -e "${targetBam.path}" ]; then
    echo "File ${targetBam.path} already exists."
    rm ${targetBam.path}*
fi

mkdir -p -m 2750 ${targetBam.parent}
cp ${sourceBam} ${targetBam}
cp ${sourceBai} ${targetBai}

cd ${targetBam.parent}
${md5sumBam}
md5sum -c ${targetBam}.md5sum

md5sum ${sourceBai} > ${targetBai}.md5sum
md5sum -c ${targetBai}.md5sum

chgrp ${executionHelperService.getGroup(it.project.projectDirectory)} ${targetBam}*
chmod 644 ${targetBam}*

touch ${checkpoint}
"""

                pbsService.executeJob(realm, cmd)
            }
        }
        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    @Override
    protected void validate() throws Throwable {
        final ImportProcess importProcess = getProcessParameterObject()

        final Collection<String> problems = importProcess.externallyProcessedMergedBamFiles.collect {
            File target = it.getFilePath().absoluteDataManagementPath
            try {
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(target)
                if (!it.md5sum) {
                    it.md5sum = checksumFileService.firstMD5ChecksumFromFile(checksumFileService.md5FileName(target.absolutePath))
                    it.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
                    it.save(flush: true)
                }
                return null
            } catch (Throwable t) {
                return "Copying of target ${target} failed, ${t.message}"
            }
        }.findAll()
        if (problems) {
            throw new ProcessingException(problems.join(","))
        }
    }
}