package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class ImportExternallyMergedBamJob extends AbstractOtpJob {

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService


    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        final ImportProcess importProcess = getProcessParameterObject()
        AbstractMultiJob.NextAction action = AbstractMultiJob.NextAction.SUCCEED

        importProcess.externallyProcessedMergedBamFiles.each {ExternallyProcessedMergedBamFile epmbf ->
            Realm realm = epmbf.project.realm
            File sourceBam = new File(epmbf.importedFrom)
            File targetBam = epmbf.getFilePath().absoluteDataManagementPath

            File checkpoint = new File(targetBam.parent, ".${targetBam.name}.checkpoint")
            File sourceBai = new File("${sourceBam}.bai")
            File targetBai = new File("${targetBam}.bai")

            if (checkpoint.exists()) {
                log.debug("Checkpoint found for ${sourceBam}, skip copying")
            } else {
                action = AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
                String md5sumBam
                if (epmbf.md5sum) {
                    md5sumBam = "echo ${epmbf.md5sum}  ${targetBam.name} > ${targetBam}.md5sum"
                } else {
                    md5sumBam = "md5sum ${sourceBam} > ${targetBam}.md5sum"
                }

                String furtherFilesCopy = epmbf.furtherFiles.collect { String relativePath ->
                    File sourceFurtherFile = new File(sourceBam.parent, relativePath)
                    File targetFurtherFile = new File(targetBam.parent, relativePath)
                    return "mkdir -p -m 2750 ${sourceFurtherFile.isDirectory() ? targetFurtherFile : targetFurtherFile.parent}\n" +
                            "cp -R ${sourceFurtherFile} ${targetFurtherFile}"
                }.join("\n")

                String furtherFiles = epmbf.furtherFiles.collect{new File(targetBam.parent, it)}.join(' ')

                String cmd = """
#!/bin/bash

if [ -e "${targetBam.path}" ]; then
    echo "File ${targetBam.path} already exists."
    rm ${targetBam.path}*
fi

mkdir -p -m 2750 ${targetBam.parent}
cp ${sourceBam} ${targetBam}
cp ${sourceBai} ${targetBai}

${furtherFilesCopy}

cd ${targetBam.parent}
${md5sumBam}
md5sum -c ${targetBam}.md5sum

md5sum ${sourceBai} > ${targetBai}.md5sum
md5sum -c ${targetBai}.md5sum

md5sum `find ${furtherFiles} -type f  ` > ${targetBam.parent}/md5sum.md5sum
md5sum -c ${targetBam.parent}/md5sum.md5sum

chgrp -R ${executionHelperService.getGroup(epmbf.project.projectDirectory)} ${targetBam}* ${furtherFiles}
chmod 644 ${targetBam}* `find ${furtherFiles} -type f  `
chmod 750  `find ${furtherFiles} -type d `

touch ${checkpoint}
"""

                clusterJobSchedulerService.executeJob(realm, cmd)
            }
        }
        if (action == AbstractMultiJob.NextAction.SUCCEED) {
            validate()
        }
        return action
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
