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
class ReplaceSourceWithLinkJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionService executionService


    @Override
    void execute() throws Exception {
        final ImportProcess importProcess = getProcessParameterObject()
        if (importProcess.replaceSourceWithLink) {
            importProcess.externallyProcessedMergedBamFiles.each {
                Realm realm = configService.getRealmDataManagement(it.project)
                File sourceBamFile = new File(it.importedFrom)
                File sourceBaiFile = new File("${it.importedFrom}.bai")
                File targetBamFile = it.getFilePath().absoluteDataManagementPath
                File targetBaiFile = new File("${it.getFilePath().absoluteDataManagementPath}.bai")

                String furtherFilesCopy = it.furtherFiles.collect { String relativePath ->
                    File sourceFurtherFile = new File(sourceBamFile.parent, relativePath)
                    File targetFurtherFile = new File(targetBamFile.parent, relativePath)
                    return "rm -rf ${sourceFurtherFile.absolutePath}\n" +
                            "ln -sf ${targetFurtherFile.absolutePath} ${sourceFurtherFile.absolutePath}"
                }.join("\n")

                String cmd = """
#!/bin/bash

set -evx

rm -f ${sourceBamFile.absolutePath}
rm -f ${sourceBaiFile.absolutePath}

ln -sf ${targetBamFile.absolutePath} ${sourceBamFile.absolutePath}
ln -sf ${targetBaiFile.absolutePath} ${sourceBaiFile.absolutePath}

${furtherFilesCopy}

echo OK
"""

                assert executionService.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZero().stdout.trim() == 'OK'
            }
        }
        importProcess.state = ImportProcess.State.FINISHED
        importProcess.save(flush: true)
        succeed()
    }
}
