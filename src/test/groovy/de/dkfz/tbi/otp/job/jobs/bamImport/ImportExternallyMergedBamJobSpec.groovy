/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.bamImport

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.ImportExternallyMergedBamJob
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper

import java.nio.file.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class ImportExternallyMergedBamJobSpec extends Specification implements DataTest, ExternalBamFactory {

    static private final String SUB_DIRECTORY = "subDirectory"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                BamImportInstance,
                Individual,
                JobDefinition,
                JobExecutionPlan,
                Pipeline,
                Process,
                ProcessingOption,
                ProcessingStep,
                ProcessParameter,
                Project,
                ReferenceGenome,
                Sample,
                SampleType,
                SeqType,
        ]
    }

    static private final long PROCESSING_STEP_ID = 1234567

    private ProcessingStep step
    private ImportExternallyMergedBamJob importExternallyMergedBamJob

    private ExternallyProcessedBamFile epmbfWithMd5sum
    private ExternallyProcessedBamFile epmbfWithoutMd5sum

    private File mainDirectory
    private File subDirectory
    private File file

    private TestConfigService configService

    @TempDir
    Path tempDir

    void setup() {
        String bamFileNameWithMd5sum = "epmbfWithMd5sum.bam"
        String bamFileNameWithoutMd5sum = "epmbfWithoutMd5sum.bam"
        mainDirectory = tempDir.toFile()
        subDirectory = new File(mainDirectory.path, SUB_DIRECTORY)
        assert subDirectory.mkdirs()
        file = new File(subDirectory, "something.txt")

        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)

        Project project = createProject()

        epmbfWithMd5sum = createBamFile(
                fileName: bamFileNameWithMd5sum,
                importedFrom: "${mainDirectory.path}/${bamFileNameWithMd5sum}",
                md5sum: HelperUtils.randomMd5sum,
                furtherFiles: [SUB_DIRECTORY]
        )
        epmbfWithMd5sum.individual.project = project
        assert epmbfWithMd5sum.individual.save(flush: true)

        epmbfWithoutMd5sum = createBamFile(
                fileName: bamFileNameWithoutMd5sum,
                importedFrom: "${mainDirectory.path}/${bamFileNameWithoutMd5sum}",
                furtherFiles: [SUB_DIRECTORY]
        )
        epmbfWithoutMd5sum.individual.project = project
        assert epmbfWithoutMd5sum.individual.save(flush: true)

        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_LOAD_MODULE_LOADER,
                type: null,
                value: "",
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_ACTIVATION_SAMTOOLS,
                type: null,
                value: "module load samtools"
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_SAMTOOLS,
                type: null,
                value: "samtools"
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_ACTIVATION_GROOVY,
                type: null,
                value: "module load groovy"
        )
        DomainFactory.createProcessingOptionLazy(
                name: COMMAND_GROOVY,
                type: null,
                value: "groovy"
        )
    }

    void cleanup() {
        configService.clean()
    }

    void "test maybe submit, when files have to be link and not yet linked, then create links"() {
        given:
        BamImportInstance importInstance = createImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum],
                linkOperation: BamImportInstance.LinkOperation.LINK_SOURCE,
        )
        createHelperObjects(importInstance)

        Path source = Paths.get(epmbfWithMd5sum.importedFrom).parent
        Path target = Paths.get(epmbfWithMd5sum.importFolder.absolutePath)

        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            0 * executeJob(_, _)
        }
        importExternallyMergedBamJob.fileService = Mock(FileService) {
            1 * createLink(target.resolve(epmbfWithMd5sum.bamFileName), source.resolve(epmbfWithMd5sum.bamFileName), _) >> {
                Path linkPath, Path existingPath, CreateLinkOption... options2 ->
                CreateFileHelper.createSymbolicLinkFile(linkPath, existingPath)
            }
            1 * createLink(target.resolve(epmbfWithMd5sum.baiFileName), source.resolve(epmbfWithMd5sum.baiFileName), _) >> {
                Path linkPath, Path existingPath, CreateLinkOption... options2 ->
                CreateFileHelper.createSymbolicLinkFile(linkPath, existingPath)
            }
            1 * createLink(target.resolve(SUB_DIRECTORY), source.resolve(SUB_DIRECTORY), _) >> {
                Path linkPath, Path existingPath, CreateLinkOption... options2 ->
                CreateFileHelper.createSymbolicLinkFile(linkPath, existingPath)
            }
            0 * _
        }

        expect:
        NextAction.SUCCEED == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be link and already linked, then do not create links again"() {
        given:
        BamImportInstance importInstance = createImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum],
                linkOperation: BamImportInstance.LinkOperation.LINK_SOURCE,
        )
        createHelperObjects(importInstance)

        CreateFileHelper.createSymbolicLinkFile(epmbfWithMd5sum.bamFile, new File(mainDirectory, epmbfWithMd5sum.bamFileName))
        CreateFileHelper.createSymbolicLinkFile(epmbfWithMd5sum.baiFile, new File(mainDirectory, epmbfWithMd5sum.baiFileName))
        CreateFileHelper.createSymbolicLinkFile(new File(epmbfWithMd5sum.importFolder, SUB_DIRECTORY), subDirectory)
        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            0 * executeJob(_, _)
        }
        importExternallyMergedBamJob.fileService = Mock(FileService) {
            0 * _
        }

        expect:
        NextAction.SUCCEED == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied, have a md5Sum and not exist already, then create copy job using given md5sum and wait for job"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        CreateFileHelper.createFile(epmbfWithMd5sum.bamFile)
        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_) >> { String command ->
                assert command ==~ getScript("echo [0-9a-f]{32}  \\S+.bam > \\S+.bam.md5sum")
            }
        }

        expect:
        NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied, not have a md5Sum and not exist already, then create copy job using calculate md5sum and wait for job"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithoutMd5sum]
        ).save(flush: true)
        createHelperObjects(importInstance)
        CreateFileHelper.createFile(epmbfWithoutMd5sum.bamFile)
        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_) >> { String command ->
                assert command ==~ getScript("md5sum .* \\| sed -e 's#.*#.*#' > .*")
            }
        }

        expect:
        NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied and exist already but no checkpoint file exist, then create normal copy job and wait for job"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithoutMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        CreateFileHelper.createFile(new File(epmbfWithoutMd5sum.importedFrom))
        CreateFileHelper.createFile(epmbfWithoutMd5sum.bamFile)
        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_) >> { String command ->
                assert command ==~ getScript("md5sum .* \\| sed -e 's#.*#.*#' > .*")
            }
        }

        expect:
        NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files already copied and a checkpoint file exists already, then create no copy job and return success"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        File targetBamFile = epmbfWithMd5sum.bamFile
        CreateFileHelper.createFile(new File(targetBamFile.parent, ".${targetBamFile.name}.checkpoint"))
        CreateFileHelper.createFile(epmbfWithMd5sum.bamMaxReadLengthFile, "123")
        CreateFileHelper.createFile(new File(epmbfWithMd5sum.importedFrom))
        CreateFileHelper.createFile(epmbfWithMd5sum.bamFile)
        CreateFileHelper.createFile(epmbfWithMd5sum.baiFile)
        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            0 * executeJob(_, _)
        }

        expect:
        NextAction.SUCCEED == importExternallyMergedBamJob.maybeSubmit()
    }

    void "test maybe submit, when files have to be copied and a checkpoint file exists already but not the files, then fail"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithoutMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        File targetBamFile = epmbfWithoutMd5sum.bamFile
        CreateFileHelper.createFile(new File(targetBamFile.parent, ".${targetBamFile.name}.checkpoint"))
        CreateFileHelper.createFile(epmbfWithoutMd5sum.bamMaxReadLengthFile, "123")

        when:
        importExternallyMergedBamJob.maybeSubmit()

        then:
        thrown(ProcessingException)
    }

    @Unroll
    void "test maybe submit, check that pattern '#furtherFilePattern' with link on '#linkFileName' and final file '#realFurtherFileName' works correctly"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithoutMd5sum]
        ).save(flush: true)

        epmbfWithoutMd5sum.furtherFiles = [
                furtherFilePattern,
        ]

        createHelperObjects(importInstance)

        assert Files.createDirectories(importExternallyMergedBamJob.projectService.getProjectDirectory(epmbfWithoutMd5sum.project))
        CreateFileHelper.createFile(new File(mainDirectory, epmbfWithoutMd5sum.bamFileName))
        CreateFileHelper.createFile(new File(mainDirectory, epmbfWithoutMd5sum.baiFileName))

        File targetDir = new File(mainDirectory, 'target')
        File real = new File(targetDir, realFurtherFileName)
        File targetOfLink = new File(targetDir, linkFileName)
        File link = new File(mainDirectory, linkFileName)

        File finalCopiedFile = new File(epmbfWithoutMd5sum.importFolder, realFurtherFileName)

        CreateFileHelper.createFile(real)
        link.parentFile.mkdirs()
        Files.createSymbolicLink(link.toPath(), targetOfLink.toPath())

        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_) >> { String command ->
                LocalShellHelper.executeAndWait(command)
            }
        }

        when:
        NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()

        then:
        finalCopiedFile.exists()

        where:
        furtherFilePattern | linkFileName   | realFurtherFileName
        'file'             | 'file'         | 'file'
        'dir'              | 'dir'          | 'dir/file'
        'dir'              | 'dir/file'     | 'dir/file'
        'dir'              | 'dir/dir'      | 'dir/dir/file'
        'dir'              | 'dir/dir/file' | 'dir/dir/file'
        'dir/file'         | 'dir'          | 'dir/file'
        'dir/file'         | 'dir/file'     | 'dir/file'
        'dir/dir'          | 'dir/dir'      | 'dir/dir/file'
        'dir/dir/file'     | 'dir/dir'      | 'dir/dir/file'
        'dir/dir/file'     | 'dir/dir/file' | 'dir/dir/file'
    }

    @Unroll
    void "test maybe submit, check that pattern '#furtherFilePattern' with link on '#linkFileName1' and second link on '#linkFileName2' and final file '#realFurtherFileName' works correctly"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithoutMd5sum]
        ).save(flush: true)

        epmbfWithoutMd5sum.furtherFiles = [
                furtherFilePattern,
        ]

        createHelperObjects(importInstance)

        assert Files.createDirectories(importExternallyMergedBamJob.projectService.getProjectDirectory(epmbfWithoutMd5sum.project))
        CreateFileHelper.createFile(new File(mainDirectory, epmbfWithoutMd5sum.bamFileName))
        CreateFileHelper.createFile(new File(mainDirectory, epmbfWithoutMd5sum.baiFileName))

        File targetDir1 = new File(mainDirectory, 'target1')
        File targetDir2 = new File(mainDirectory, 'target2')

        File real = new File(targetDir2, realFurtherFileName)

        File targetOfLink2 = new File(targetDir2, linkFileName2)
        File targetOfLink1 = new File(targetDir1, linkFileName1)

        File link2 = new File(targetDir1, linkFileName2)
        File link1 = new File(mainDirectory, linkFileName1)

        File finalCopiedFile = new File(epmbfWithoutMd5sum.importFolder, realFurtherFileName)

        CreateFileHelper.createFile(real)
        link2.parentFile.mkdirs()
        Files.createSymbolicLink(link2.toPath(), targetOfLink2.toPath())
        link1.parentFile.mkdirs()
        Files.createSymbolicLink(link1.toPath(), targetOfLink1.toPath())

        importExternallyMergedBamJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_) >> { String command ->
                LocalShellHelper.executeAndWait(command)
            }
        }

        when:
        NextAction.WAIT_FOR_CLUSTER_JOBS == importExternallyMergedBamJob.maybeSubmit()

        then:
        finalCopiedFile.exists()

        where:
        furtherFilePattern   | linkFileName1 | linkFileName2   | realFurtherFileName
        'file'               | 'file'        | 'file'          | 'file'
        'dir'                | 'dir'         | 'dir'           | 'dir/file'
        'dir'                | 'dir/dir2'    | 'dir/dir2/dir3' | 'dir/dir2/dir3/dir4/file'
        'dir/dir2/dir3/dir4' | 'dir/dir2'    | 'dir/dir2/dir3' | 'dir/dir2/dir3/dir4/file'
    }

    void "test validate when everything is valid and bam file already has a md5sum"() {
        given:
        BamImportInstance importInstance = createImportInstance([
                externallyProcessedBamFiles: [epmbfWithMd5sum],
                linkOperation                    : BamImportInstance.LinkOperation.COPY_AND_KEEP,
        ])

        createHelperObjects(importInstance)
        CreateFileHelper.createFile(epmbfWithMd5sum.bamFile)
        CreateFileHelper.createFile(epmbfWithMd5sum.baiFile)
        CreateFileHelper.createFile(epmbfWithMd5sum.bamMaxReadLengthFile, "123")

        when:
        importExternallyMergedBamJob.validate()

        then:
        noExceptionThrown()
        epmbfWithMd5sum.maximalReadLength == 123
        epmbfWithMd5sum.md5sum
        epmbfWithMd5sum.fileSize > 0
        epmbfWithMd5sum.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        epmbfWithMd5sum.workPackage.bamFileInProjectFolder == epmbfWithMd5sum
    }

    void "test validate when everything is valid and bam file has no md5sum yet"() {
        given:
        BamImportInstance importInstance = createImportInstance([
                externallyProcessedBamFiles: [epmbfWithoutMd5sum],
                linkOperation                    : BamImportInstance.LinkOperation.COPY_AND_KEEP,
        ])

        createHelperObjects(importInstance)
        File targetBamFile = epmbfWithoutMd5sum.bamFile
        CreateFileHelper.createFile(targetBamFile)
        CreateFileHelper.createFile(epmbfWithoutMd5sum.baiFile)
        CreateFileHelper.createFile(new File("${targetBamFile}.md5sum"),
                "${epmbfWithMd5sum.md5sum} epmbfName")
        CreateFileHelper.createFile(epmbfWithoutMd5sum.bamMaxReadLengthFile, "123")

        when:
        importExternallyMergedBamJob.validate()

        then:
        noExceptionThrown()
        epmbfWithoutMd5sum.maximalReadLength == 123
        epmbfWithoutMd5sum.md5sum
        epmbfWithoutMd5sum.fileSize > 0
        epmbfWithoutMd5sum.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        epmbfWithoutMd5sum.workPackage.bamFileInProjectFolder == epmbfWithoutMd5sum
    }

    void "test validate when everything is valid and bam file was only linked"() {
        given:
        BamImportInstance importInstance = createImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum],
                linkOperation: BamImportInstance.LinkOperation.LINK_SOURCE,
        )

        createHelperObjects(importInstance)
        CreateFileHelper.createSymbolicLinkFile(epmbfWithMd5sum.bamFile, new File(epmbfWithMd5sum.importedFrom))
        CreateFileHelper.createSymbolicLinkFile(epmbfWithMd5sum.baiFile, Paths.get(epmbfWithMd5sum.importedFrom).resolveSibling(epmbfWithMd5sum.baiFileName).toFile())
        CreateFileHelper.createSymbolicLinkFile(new File(epmbfWithMd5sum.importFolder, SUB_DIRECTORY), subDirectory)

        when:
        importExternallyMergedBamJob.validate()

        then:
        noExceptionThrown()
        epmbfWithMd5sum.maximalReadLength == null
        epmbfWithMd5sum.md5sum
        epmbfWithMd5sum.fileSize > 0
        epmbfWithMd5sum.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        epmbfWithMd5sum.workPackage.bamFileInProjectFolder == epmbfWithMd5sum
    }

    void "test validate when maxReadLength file doesn't exist and read length is already set"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        CreateFileHelper.createFile(epmbfWithMd5sum.bamFile)
        CreateFileHelper.createFile(epmbfWithMd5sum.baiFile)

        epmbfWithMd5sum.maximumReadLength = 123
        epmbfWithMd5sum.save(flush: true)

        when:
        importExternallyMergedBamJob.validate()

        then:
        epmbfWithMd5sum.maximalReadLength == 123
    }

    void "test validate when maxReadLength file doesn't exist and read length is not set"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        CreateFileHelper.createFile(epmbfWithMd5sum.bamFile)
        CreateFileHelper.createFile(epmbfWithMd5sum.baiFile)

        when:
        importExternallyMergedBamJob.validate()

        then:
        ProcessingException e = thrown()
        e.message.contains("epmbfWithMd5sum.bam.maxReadLength on local filesystem is not accessible or does not exist.")
    }

    void "test validate when everything is not equal"() {
        given:
        String importedMd5sum = HelperUtils.randomMd5sum
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithoutMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        File targetBamFile = epmbfWithoutMd5sum.bamFile
        CreateFileHelper.createFile(targetBamFile)
        CreateFileHelper.createFile(epmbfWithoutMd5sum.baiFile)
        CreateFileHelper.createFile(new File("${targetBamFile}.md5sum"),
                "${importedMd5sum} epmbfName")
        CreateFileHelper.createFile(epmbfWithoutMd5sum.bamMaxReadLengthFile, "123")

        when:
        importExternallyMergedBamJob.validate()

        then:
        epmbfWithoutMd5sum.md5sum == importedMd5sum
    }

    void "test validate when copying did not work"() {
        given:
        BamImportInstance importInstance = new BamImportInstance(
                externallyProcessedBamFiles: [epmbfWithMd5sum]
        ).save(flush: true)

        createHelperObjects(importInstance)
        CreateFileHelper.createFile(new File("${epmbfWithMd5sum.importedFrom}.md5sum"),
                "${epmbfWithMd5sum.md5sum} epmbfName")
        CreateFileHelper.createFile(epmbfWithMd5sum.bamMaxReadLengthFile, "123")

        when:
        importExternallyMergedBamJob.validate()

        then:
        ProcessingException processingException = thrown()
        processingException.message.contains("Copying of target")
        processingException.message.contains("on local filesystem is not accessible or does not exist.")
    }

    private void createHelperObjects(BamImportInstance importInstance) {
        importExternallyMergedBamJob = Spy(ImportExternallyMergedBamJob) {
            _ * getProcessParameterObject() >> importInstance
            _ * getProcessingStep() >> step
        }

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.resolve("root").toString(),
                (OtpProperty.PATH_TOOLS)       : "/asdf",
        ])

        importExternallyMergedBamJob.configService = configService
        importExternallyMergedBamJob.checksumFileService = new ChecksumFileService()
        importExternallyMergedBamJob.checksumFileService.fileService = new FileService()
        importExternallyMergedBamJob.checksumFileService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        importExternallyMergedBamJob.executionHelperService = Mock(ExecutionHelperService) {
            _ * getGroup(_, _) >> 'someGroup'
        }
        importExternallyMergedBamJob.fileSystemService = new TestFileSystemService()
        importExternallyMergedBamJob.fileSystemService.processingOptionService = new ProcessingOptionService()
        importExternallyMergedBamJob.fileService = new FileService()
        importExternallyMergedBamJob.processingOptionService = new ProcessingOptionService()
        importExternallyMergedBamJob.projectService = new ProjectService()
        importExternallyMergedBamJob.projectService.configService = configService
        importExternallyMergedBamJob.projectService.fileSystemService = new TestFileSystemService()

        ExternallyProcessedBamFile externallyProcessedBamFile = importInstance.externallyProcessedBamFiles[0]
        File bamFile = new File(mainDirectory, externallyProcessedBamFile.bamFileName)
        File baiFile = new File(mainDirectory, externallyProcessedBamFile.baiFileName)

        CreateFileHelper.createFile(bamFile)
        CreateFileHelper.createFile(baiFile)

        DomainFactory.createProcessParameter([
                process  : step.process,
                value    : importInstance.id.toString(),
                className: BamImportInstance.name,
        ])
    }

    @SuppressWarnings('ConsecutiveBlankLines') // is part of an script
    private static String getScript(String hasOrNotMd5SumCmd) {
        return """
set -o pipefail
set -v


module load samtools
module load groovy

if \\[ -e "\\S+" \\]; then
    echo "File \\S+.bam already exists."
    rm -rf \\S+\\* \\S+
fi

mkdir -p -m 2750 \\S+
# copy and calculate max read length at the same time
cat \\S+.bam \\| tee \\S+.bam \\| samtools view - \\| groovy /asdf/bamMaxReadLength.groovy > \\S+.bam.maxReadLength
cp -HL \\S+.bam.bai \\S+.bam.bai

mkdir -p -m 2750 \\S+
cp -HLR \\S+ \\S+

cd \\S+
${hasOrNotMd5SumCmd}
md5sum -c \\S+.bam.md5sum

md5sum .*.bam.bai \\| sed -e 's#.*#.*#' > \\S+.bam.bai.md5sum
md5sum -c \\S+.bam.bai.md5sum

md5sum `find -L .* -type f` \\| sed -e 's#.*#.*#' > \\S+\\/md5sum.md5sum
md5sum -c \\S+\\/md5sum.md5sum

chgrp -hR \\S+ \\S+
find \\S+ -type d -not -perm 2750 -print -exec chmod 2750 '\\{\\}' \\\\;
find \\S+ -type f -not -perm 440 -not -name "\\*.bam" -not -name "\\*.bai" -not -name ".roddyExecCache.txt" -not -name "zippedAnalysesMD5.txt" -print -exec chmod 440 '\\{\\}' \\\\;
find \\S+ -type f -not -perm 444 \\\\\\( -name "\\*.bam" -or -name "\\*.bai" \\\\\\) -print -exec chmod 444 '\\{\\}' \\\\;

touch \\S+
"""
    }
}
