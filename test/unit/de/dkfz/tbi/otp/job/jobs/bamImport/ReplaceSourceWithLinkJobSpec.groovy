package de.dkfz.tbi.otp.job.jobs.bamImport

import de.dkfz.tbi.otp.*
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

    TestConfigService configService

    File mainDirectory

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        String bamFileName = "epmbfWithMd5sum.bam"
        File importedFile = new File(temporaryFolder.newFolder().absolutePath, bamFileName)
        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)

        mainDirectory = importedFile.parentFile

        ExternallyProcessedMergedBamFile epmbf = DomainFactory.createExternallyProcessedMergedBamFile(
                fileName: bamFileName,
                importedFrom: importedFile,
                md5sum: DomainFactory.DEFAULT_MD5_SUM,
                furtherFiles: ["subDirectory"]
        )

        importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [epmbf],
                replaceSourceWithLink: true,
                triggerAnalysis: true,
        ).save()
    }

    def cleanup() {
        configService.clean()
    }

    void "test execute when everything is fine"() {
        given:
        createHelperObjects()
        ExternallyProcessedMergedBamFile bamFile = importProcess.externallyProcessedMergedBamFiles[0]
        [
                new File(bamFile.getImportedFrom()),
                new File("${bamFile.getImportedFrom()}.bai"),
                new File(new File(new File(bamFile.getImportedFrom()).parentFile, bamFile.furtherFiles.first()), 'file.txt'),
                bamFile.getBamFile(),
                bamFile.getBaiFile(),
                new File(new File(bamFile.getImportFolder(), bamFile.furtherFiles.first()), 'file.txt'),
        ].each {
            CreateFileHelper.createFile(it)
        }
        linkingJob.linkFileUtils.executionService = Mock(ExecutionService) {
            1 * executeCommand(_, _) >> { Realm realm, String command ->
                assert command ==~ """\
umask 027; mkdir --parents --mode 2750 [^ ]+ [^ ]+ &>\\/dev\\/null; echo \\\$\\?
ln -sf [^ ]+.bam [^ ]+.bam
ln -sf [^ ]+.bam.bai [^ ]+.bam.bai
ln -sf [^ ]+\\/subDirectory\\/file.txt [^ ]+\\/subDirectory\\/file.txt
"""
                return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
            }
            0 * _
        }
        linkingJob.linkFileUtils.lsdfFilesService.executionService = Mock(ExecutionService) {
            1 * executeCommand(_, _) >> { Realm realm, String command ->
                assert command ==~ "rm -rf [^ ]+.bam [^ ]+.bam.bai [^ ]+\\/subDirectory\\/file.txt &>\\/dev\\/null\necho \\\$\\?"
                return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
            }
            0 * _
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
                getProcessParameterObject: { -> importProcess },
                getProcessingStep        : { -> step }
        ] as ReplaceSourceWithLinkJob

        CreateFileHelper.createFile(new File("${importProcess.externallyProcessedMergedBamFiles[0].importedFrom}"))

        configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder("root").path])

        DomainFactory.createProcessParameter([
                process  : step.process,
                value    : importProcess.id.toString(),
                className: ImportProcess.class.name
        ])

        linkingJob.configService = configService
        linkingJob.executionService = new ExecutionService()
        linkingJob.linkFileUtils = new LinkFileUtils()
        linkingJob.linkFileUtils.lsdfFilesService = new LsdfFilesService()
        linkingJob.linkFileUtils.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        linkingJob.linkFileUtils.createClusterScriptService = new CreateClusterScriptService()
    }

    @Unroll
    void "check that linking works correctly for pattern '#furtherFilePattern' with link on '#linkFileName1' and second link on '#linkFileName2' and final file '#realFurtherFileName'"() {
        given:
        createHelperObjects()
        ExternallyProcessedMergedBamFile bamFile = importProcess.externallyProcessedMergedBamFiles[0]
        bamFile.furtherFiles = [
                furtherFilePattern
        ]

        File targetDir1 = new File(mainDirectory, 'target1')
        File targetDir2 = new File(mainDirectory, 'target2')

        File real = new File(targetDir2, realFurtherFileName)

        File targetOfLink2 = new File(targetDir2, linkFileName2)
        File targetOfLink1 = new File(targetDir1, linkFileName1)

        File link2 = new File(targetDir1, linkFileName2)
        File link1 = new File(mainDirectory, linkFileName1)

        File copiedFile = new File(bamFile.getImportFolder(), realFurtherFileName)

        [
                new File(mainDirectory, bamFile.getBamFileName()),
                new File(mainDirectory, bamFile.getBaiFileName()),
                real,

                bamFile.getBamFile(),
                bamFile.getBaiFile(),
                copiedFile,
        ].each {
            CreateFileHelper.createFile(it)
        }

        link2.parentFile.mkdirs()
        Files.createSymbolicLink(link2.toPath(), targetOfLink2.toPath())
        link1.parentFile.mkdirs()
        Files.createSymbolicLink(link1.toPath(), targetOfLink1.toPath())

        linkingJob.linkFileUtils.executionService = Mock(ExecutionService) {
            1 * executeCommand(_, _) >> { Realm realm, String command ->
                assert command ==~ """\
umask 027; mkdir --parents --mode 2750 [^ ]+ [^ ]+ &>\\/dev\\/null; echo \\\$\\?
ln -sf [^ ]+.bam [^ ]+.bam
ln -sf [^ ]+.bam.bai [^ ]+.bam.bai
ln -sf [^ ]+ [^ ]+
"""
                return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
            }
            0 * _
        }
        linkingJob.linkFileUtils.lsdfFilesService.executionService = Mock(ExecutionService) {
            1 * executeCommand(_, _) >> { Realm realm, String command ->
                assert command ==~ "rm -rf [^ ]+.bam [^ ]+.bam.bai [^ ]+ &>\\/dev\\/null\necho \\\$\\?"
                return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
            }
            0 * _
        }

        when:
        linkingJob.execute()

        then:
        importProcess.state == ImportProcess.State.FINISHED
        real.toPath().toRealPath() == copiedFile.toPath()

        where:
        furtherFilePattern   | linkFileName1 | linkFileName2   | realFurtherFileName
        'file'               | 'file'        | 'file'          | 'file'
        'dir'                | 'dir'         | 'dir'           | 'dir/file'
        'dir'                | 'dir/dir2'    | 'dir/dir2/dir3' | 'dir/dir2/dir3/dir4/file'
        'dir/dir2'           | 'dir/dir2'    | 'dir/dir2/dir3' | 'dir/dir2/dir3/dir4/file'
        'dir/dir2/dir3/dir4' | 'dir/dir2'    | 'dir/dir2/dir3' | 'dir/dir2/dir3/dir4/file'
    }


    void "test execute when everything files already linked, do not delete copied files"() {
        given:
        createHelperObjects()
        ExternallyProcessedMergedBamFile bamFile = importProcess.externallyProcessedMergedBamFiles[0]
        [
                bamFile.getBamFileName(),
                bamFile.getBaiFileName(),
                bamFile.getFurtherFiles().first()
        ].each {
            File copied = new File(bamFile.getImportFolder(), it)
            File link = new File(mainDirectory, it)
            CreateFileHelper.createFile(copied)
            link.delete()
            Files.createSymbolicLink(link.toPath(), copied.toPath())
        }

        linkingJob.linkFileUtils.executionService = Mock(ExecutionService) {
            0 * _
        }
        linkingJob.linkFileUtils.lsdfFilesService.executionService = Mock(ExecutionService) {
            0 * _
        }

        when:
        linkingJob.execute()

        then:
        importProcess.state == ImportProcess.State.FINISHED
    }
}
