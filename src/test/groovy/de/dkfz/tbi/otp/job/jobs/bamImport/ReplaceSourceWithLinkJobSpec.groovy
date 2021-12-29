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
package de.dkfz.tbi.otp.job.jobs.bamImport

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.ReplaceSourceWithLinkJob
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystems
import java.nio.file.Files

class ReplaceSourceWithLinkJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
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
                SeqType,
        ]
    }

    static final long PROCESSING_STEP_ID = 1234567

    ReplaceSourceWithLinkJob linkingJob
    ProcessingStep step

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
                linkOperation: ImportProcess.LinkOperation.COPY_AND_LINK,
                triggerAnalysis: true,
        ).save(flush: true)
    }

    def cleanup() {
        configService.clean()
    }

    void "test execute when everything is fine"() {
        given:
        createHelperObjects()
        ExternallyProcessedMergedBamFile bamFile = importProcess.externallyProcessedMergedBamFiles[0]
        File sourceBam = new File(bamFile.importedFrom)
        File sourceBai = new File(sourceBam.parentFile, bamFile.baiFileName)
        File sourceFurtherFile = new File(new File(new File(bamFile.getImportedFrom()).parentFile, bamFile.furtherFiles.first()), 'file.txt')
        File targetFurtherFile = new File(new File(bamFile.importFolder, bamFile.furtherFiles.first()), 'file.txt')
        [
                sourceBam,
                sourceBai,
                sourceFurtherFile,
                bamFile.bamFile,
                bamFile.baiFile,
                targetFurtherFile,
        ].each {
            CreateFileHelper.createFile(it, HelperUtils.randomMd5sum)
        }

        when:
        linkingJob.execute()

        then:
        sourceBam.toPath().toRealPath() == bamFile.bamFile.toPath()
        sourceBai.toPath().toRealPath() == bamFile.baiFile.toPath()
        sourceFurtherFile.toPath().toRealPath() == targetFurtherFile.toPath()
        importProcess.state == ImportProcess.State.FINISHED
    }

    void "test execute when no linking needs"() {
        given:
        importProcess.linkOperation = ImportProcess.LinkOperation.COPY_AND_KEEP
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
                getProcessingStep        : { -> step },
        ] as ReplaceSourceWithLinkJob

        CreateFileHelper.createFile(new File("${importProcess.externallyProcessedMergedBamFiles[0].importedFrom}"))

        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder("root").path])

        DomainFactory.createProcessParameter([
                process  : step.process,
                value    : importProcess.id.toString(),
                className: ImportProcess.class.name,
        ])

        linkingJob.configService = configService
        linkingJob.linkFileUtils = new LinkFileUtils()
        linkingJob.linkFileUtils.fileService = new FileService()
        linkingJob.linkFileUtils.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem(_) >> FileSystems.default
            0 * _
        }
        linkingJob.fileSystemService = new TestFileSystemService()
        linkingJob.fileService = new FileService()
    }

    @Unroll
    void "check that linking works correctly for pattern '#furtherFilePattern' with link on '#linkFileName1' and second link on '#linkFileName2' and final file '#realFurtherFileName'"() {
        given:
        createHelperObjects()
        ExternallyProcessedMergedBamFile bamFile = importProcess.externallyProcessedMergedBamFiles[0]
        bamFile.furtherFiles = [
                furtherFilePattern,
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

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test execute when everything files already linked, do not delete copied files"() {
        given:
        createHelperObjects()
        linkingJob.linkFileUtils = Mock(LinkFileUtils) {
            0 * _
        }

        ExternallyProcessedMergedBamFile bamFile = importProcess.externallyProcessedMergedBamFiles[0]
        [
                bamFile.getBamFileName(),
                bamFile.getBaiFileName(),
                bamFile.getFurtherFiles().first(),
        ].each {
            File copied = new File(bamFile.getImportFolder(), it)
            File link = new File(mainDirectory, it)
            CreateFileHelper.createFile(copied)
            link.delete()
            Files.createSymbolicLink(link.toPath(), copied.toPath())
        }

        when:
        linkingJob.execute()

        then:
        importProcess.state == ImportProcess.State.FINISHED
    }
}
