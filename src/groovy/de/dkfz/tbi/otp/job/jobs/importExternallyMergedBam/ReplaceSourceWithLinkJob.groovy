package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

class ReplaceSourceWithLinkJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    PbsService pbsService

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

                String cmd = """
#!/bin/bash

set -evx

rm -f ${sourceBamFile.absolutePath}
rm -f ${sourceBaiFile.absolutePath}

ln -sf ${targetBamFile.absolutePath} ${sourceBamFile.absolutePath}
ln -sf ${targetBaiFile.absolutePath} ${sourceBaiFile.absolutePath}

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
