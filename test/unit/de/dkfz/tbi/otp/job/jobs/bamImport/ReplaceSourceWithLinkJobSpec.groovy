package de.dkfz.tbi.otp.job.jobs.bamImport

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

@Mock([
        ExternalMergingWorkPackage,
        ExternallyProcessedMergedBamFile,
        ImportProcess,
        Individual,
        JobDefinition,
        JobExecutionPlan,
        Pipeline,
        Process,
        ProcessingStep,
        ProcessParameter,
        Project,
        Realm,
        ReferenceGenome,
        Sample,
        SampleType,
        SeqType
])
class ReplaceSourceWithLinkJobSpec extends Specification {

    final long PROCESSING_STEP_ID = 1234567

    ReplaceSourceWithLinkJob linkingJob
    ProcessingStep step
    ExecutionService executionService

    ImportProcess importProcess

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        String bamFileName = "epmbfWithMd5sum.bam"
        File importedFile = new File(temporaryFolder.newFolder().absolutePath, bamFileName)
        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)

        ExternallyProcessedMergedBamFile epmbf = DomainFactory.createExternallyProcessedMergedBamFile(
                fileName            : bamFileName,
                importedFrom        : importedFile,
                md5sum              : DomainFactory.DEFAULT_MD5_SUM
        )

        importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles : [epmbf],
                replaceSourceWithLink             : true
        ).save()
    }

    void "test execute when everything is fine"() {
        given:
        createHelperObjects()
        CreateFileHelper.createFile(new File("${importProcess.externallyProcessedMergedBamFiles[0].getFilePath().absoluteDataManagementPath}"))
        linkingJob.executionService = Mock(ExecutionService) {
            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                assert command ==~ """
#!/bin/bash

set -evx

rm -f .*
rm -f .*

ln -sf .* .*
ln -sf .* .*

echo OK
"""
                return new ProcessHelperService.ProcessOutput('OK','',0)
            }
        }

        when:
        linkingJob.execute()

        then:
        importProcess.state == ImportProcess.State.FINISHED
    }

    void "test execute when no linking needs"() {
        given:
        importProcess.replaceSourceWithLink = false
        importProcess.save(flush: true)
        createHelperObjects()

        when:
        linkingJob.execute()

        then:
        importProcess.externallyProcessedMergedBamFiles.each {
            assert !Files.isSymbolicLink(new File(it.importedFrom).toPath())
        }
        importProcess.state == ImportProcess.State.FINISHED
    }

    private void createHelperObjects() {
        linkingJob = [
                getProcessParameterObject : { -> importProcess },
                getProcessingStep         : { -> step }
        ] as ReplaceSourceWithLinkJob

        CreateFileHelper.createFile(new File("${importProcess.externallyProcessedMergedBamFiles[0].importedFrom}"))

        DomainFactory.createRealmDataManagement([
                name     : importProcess.externallyProcessedMergedBamFiles.first().project.realmName,
                rootPath : temporaryFolder.newFolder("root").path
        ])

        DomainFactory.createProcessParameter([
                process   : step.process,
                value     : importProcess.id.toString(),
                className : ImportProcess.class.name
        ])

        linkingJob.configService = new ConfigService()
        linkingJob.executionService = new ExecutionService()
    }
}