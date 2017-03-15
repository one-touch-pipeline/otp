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
class ImportExternallyMergedBamJobSpec extends Specification {


    final long PROCESSING_STEP_ID = 1234567

    ProcessingStep step
    ImportExternallyMergedBamJob importExternallyMergedBamJob

    ExternallyProcessedMergedBamFile epmbfWithMd5sum
    ExternallyProcessedMergedBamFile epmbfWithoutMd5sum

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        String bamFileNameWithMd5sum = "epmbfWithMd5sum.bam"
        String bamFileNameWithoutMd5sum = "epmbfWithoutMd5sum.bam"

        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)

        Project project = DomainFactory.createProject()

        epmbfWithMd5sum = DomainFactory.createExternallyProcessedMergedBamFile(
                fileName            : bamFileNameWithMd5sum,
                importedFrom        : "${temporaryFolder.newFolder().path}/${bamFileNameWithMd5sum}",
                md5sum              : DomainFactory.DEFAULT_MD5_SUM
        )
        epmbfWithMd5sum.individual.project = project
        assert epmbfWithMd5sum.individual.save(flush: true)

        epmbfWithoutMd5sum = DomainFactory.createExternallyProcessedMergedBamFile(
                fileName     : bamFileNameWithoutMd5sum,
                importedFrom : "${temporaryFolder.newFolder().path}/${bamFileNameWithoutMd5sum}",
        )
        epmbfWithoutMd5sum.individual.project = project
        assert epmbfWithoutMd5sum.individual.save(flush: true)
    }


    void "test maybe submit, when files have to be copied, have a md5Sum and not exist already"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithMd5sum]
        ).save()

        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithMd5sum.getFilePath().absoluteDataManagementPath}"))
        importExternallyMergedBamJob.pbsService = Mock(PbsService) {
            1 * executeJob(_, _) >> { Realm realm, String command ->
                assert command ==~ getScript("echo .*  .* > .*")
            }
        }

        expect:
        AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied, not have a md5Sum and not exist already"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithoutMd5sum]
        ).save()
        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath}"))
        importExternallyMergedBamJob.pbsService = Mock(PbsService) {
            1 * executeJob(_, _) >> { Realm realm, String command ->
                assert command ==~ getScript("md5sum .* > .*")
            }
        }

        expect:
        AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied and exist already"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithoutMd5sum]
        ).save()

        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.importedFrom}"))
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath}"))
        importExternallyMergedBamJob.pbsService = Mock(PbsService) {
            1 * executeJob(_, _) >> { Realm realm, String command ->
                assert command ==~ getScript("md5sum .* > .*")
            }
        }

        expect:
        AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied and a checkpoint file exists already"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithoutMd5sum]
        ).save()

        createHelperObjects(importProcess)
        File targetBamFile = epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath
        CreateFileHelper.createFile(new File(targetBamFile.parent, ".${targetBamFile.name}.checkpoint"))

        when:
        importExternallyMergedBamJob.maybeSubmit()

        then:
        !epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath.exists()
    }

    void "test validate when everything is valid and bam file already has a md5sum"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithMd5sum]
        ).save()

        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithMd5sum.getFilePath().absoluteDataManagementPath}"))

        when:
        importExternallyMergedBamJob.validate()

        then:
        noExceptionThrown()
    }

    void "test validate when everything is valid and bam file has no md5sum yet"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithoutMd5sum]
        ).save()

        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath}"))
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath}.md5sum"),
                "${epmbfWithMd5sum.md5sum} epmbfName")

        when:
        importExternallyMergedBamJob.validate()

        then:
        noExceptionThrown()
    }

    void "test validate when everything is not equal"() {
        given:
        String importedMd5sum = DomainFactory.DEFAULT_MD5_SUM
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithoutMd5sum]
        ).save()

        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath}"))
        CreateFileHelper.createFile(new File("${epmbfWithoutMd5sum.getFilePath().absoluteDataManagementPath}.md5sum"),
                "${importedMd5sum} epmbfName")

        when:
        importExternallyMergedBamJob.validate()

        then:
        epmbfWithoutMd5sum.md5sum == importedMd5sum
    }

    void "test validate when copying did not work"() {
        given:
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbfWithMd5sum]
        ).save()

        createHelperObjects(importProcess)
        CreateFileHelper.createFile(new File("${epmbfWithMd5sum.importedFrom}.md5sum"),
                "${epmbfWithMd5sum.md5sum} epmbfName")

        when:
        importExternallyMergedBamJob.validate()

        then:
        ProcessingException processingException = thrown()
        processingException.message.contains("Copying of target")
        processingException.message.contains("not found")
    }

    private void createHelperObjects(ImportProcess importProcess) {
        importExternallyMergedBamJob = [
                getProcessParameterObject : { -> importProcess },
                getProcessingStep         : { -> step }
        ] as ImportExternallyMergedBamJob

        importExternallyMergedBamJob.configService = new ConfigService()
        importExternallyMergedBamJob.checksumFileService = new ChecksumFileService()
        importExternallyMergedBamJob.executionHelperService = new ExecutionHelperService()

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
    }

    private static String getScript(String hasOrNotMd5SumCmd) {
        return """
#!/bin/bash

if \\[ -e ".*" \\]; then
    echo "File .* already exists."
    rm .*
fi

mkdir -p -m 2750 .*
cp .* .*
cp .* .*

cd .*
${hasOrNotMd5SumCmd}
md5sum -c .*

md5sum .* > .*
md5sum -c .*

chgrp .* .*
chmod 644 .*

touch .*
"""
    }
}