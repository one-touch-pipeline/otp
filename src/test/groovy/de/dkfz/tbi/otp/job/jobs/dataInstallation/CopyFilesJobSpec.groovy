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

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

class CopyFilesJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                FileType,
                Individual,
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingStep,
                ProcessParameter,
                Project,
                Realm,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatformGroup,
                SeqPlatform,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                SoftwareToolIdentifier,
        ]
    }

    static final long PROCESSING_STEP_ID = 1234567

    TestConfigService configService
    CopyFilesJob copyFilesJob
    ProcessingStep step

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder("root").path])
        copyFilesJob = new CopyFilesJob()
        copyFilesJob.processingStep = step
        copyFilesJob.configService = configService
        copyFilesJob.lsdfFilesService = new LsdfFilesService()
        copyFilesJob.checksumFileService = new ChecksumFileService()
        copyFilesJob.checksumFileService.fileSystemService  = new TestFileSystemService()
        copyFilesJob.checksumFileService.lsdfFilesService = copyFilesJob.lsdfFilesService
        copyFilesJob.fileService = new FileService()
        copyFilesJob.fileSystemService = new TestFileSystemService()
    }

    def cleanup() {
        configService.clean()
    }

    void "test checkInitialSequenceFiles where no data files are connected, should fail"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        when:
        copyFilesJob.checkInitialSequenceFiles(seqTrack)

        then:
        ProcessingException e = thrown()
        e.message.contains("No files in processing for seqTrack")
    }


    void "test checkInitialSequenceFiles where no files exist on the filesystem, should fail"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile()

        when:
        copyFilesJob.checkInitialSequenceFiles(seqTrack)

        then:
        ProcessingException e = thrown()
        e.message.contains("files are missing")
    }


    void "test checkInitialSequenceFiles where files exist on the filesystem, succeeds"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        when:
        copyFilesJob.checkInitialSequenceFiles(seqTrack)

        then:
        noExceptionThrown()
    }


    void "test maybe submit when file has to be copied and not exists already"() {
        given:
        copyFilesJob.fileService.remoteShellHelper = getRemoteShellHelper(createSeqTrack())
        copyFilesJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_, _) >> { Realm realm, String command ->
                assert command ==~ """
cd .*
if \\[ -e ".*" \\]; then
    echo "File .* already exists."
    rm .*
fi
cp .* .*
md5sum .* > .*
chgrp -h .* .* .*
chmod 440 .* .*
"""
            }
        }

        expect:
        NextAction.WAIT_FOR_CLUSTER_JOBS == copyFilesJob.maybeSubmit()
    }


    void "test maybe submit when file has to be linked and not exists already"() {
        given:
        copyFilesJob.fileService.remoteShellHelper = getRemoteShellHelper(createSeqTrack(true))

        expect:
        NextAction.SUCCEED == copyFilesJob.maybeSubmit()
    }

    void "test maybe submit when file has to be linked and exists already"() {
        given:
        SeqTrack seqTrack = createSeqTrack(true)
        copyFilesJob.fileService.remoteShellHelper = getRemoteShellHelper(seqTrack)
        CollectionUtils.exactlyOneElement(seqTrack.dataFiles)

        expect:
        NextAction.SUCCEED == copyFilesJob.maybeSubmit()
    }

    private RemoteShellHelper getRemoteShellHelper(SeqTrack seqTrack) {
        Mock(RemoteShellHelper) {
            10 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                assert command ==~ "(chmod 2750|chgrp -h ${seqTrack.project.unixGroup}) .*/${seqTrack.project.dirName}(/.*)?"
                return new ProcessOutput(command, '', 0)
            }
        }
    }

    void "test validate when everything is valid"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.lsdfFilesService.getFileFinalPath(dataFile)))
        CreateFileHelper.createFile(new File(copyFilesJob.checksumFileService.pathToMd5File(dataFile)), "${dataFile.md5sum}  dataFileName")

        when:
        copyFilesJob.validate()

        then:
        noExceptionThrown()
    }

    void "test validate when md5Sum is not equal"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.lsdfFilesService.getFileFinalPath(dataFile)))
        CreateFileHelper.createFile(new File(copyFilesJob.checksumFileService.pathToMd5File(dataFile)), "${DomainFactory.DEFAULT_MD5_SUM}  dataFileName")

        when:
        copyFilesJob.validate()

        then:
        ProcessingException processingException = thrown()
        processingException.message.contains("Copying or linking of targetFile")
        processingException.message.contains("assert checksumFileService.compareMd5(dataFile)")
    }

    void "test validate when copying did not work"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.checksumFileService.pathToMd5File(dataFile)), "${dataFile.md5sum}  dataFileName")

        when:
        copyFilesJob.validate()

        then:
        ProcessingException processingException = thrown()
        processingException.message.contains("Copying or linking of targetFile")
        processingException.message.contains("on local filesystem is not accessible or does not exist.")
    }


    private SeqTrack createSeqTrack(boolean hasToBeLinked = false) {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                [linkedExternally: hasToBeLinked],
                [initialDirectory: temporaryFolder.newFolder().path],
        )

        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File("${dataFile.initialDirectory}/${dataFile.fileName}"))

        DomainFactory.createProcessParameter([
                process: step.process,
                value: seqTrack.id,
                className: SeqTrack.class.name,
        ])

        return seqTrack
    }
}
