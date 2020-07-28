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
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LinkFileUtils

class CreateViewByPidJobSpec extends Specification implements DataTest {

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

    CreateViewByPidJob createViewByPidJob
    ProcessingStep step

    def setup() {
        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)
        createViewByPidJob = new CreateViewByPidJob()
        createViewByPidJob.processingStep = step

        createViewByPidJob.lsdfFilesService = new LsdfFilesService()
        createViewByPidJob.configService = new TestConfigService()
    }


    void "test execute when everything is fine"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([:],
                [
                        fileExists    : true,
                        dateFileSystem: new Date(),
                        fileSize      : DomainFactory.counter++,
                ]
        )

        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)

        File source = new File(createViewByPidJob.lsdfFilesService.getFileFinalPath(dataFile))
        File target = new File(createViewByPidJob.lsdfFilesService.getFileViewByPidPath(dataFile))

        DomainFactory.createProcessParameter([
                process  : step.process,
                value    : seqTrack.id,
                className: SeqTrack.class.name,
        ])

        createViewByPidJob.linkFileUtils = Mock(LinkFileUtils) {
            1 * createAndValidateLinks(_, _, _) >> { Map<File, File> sourceLinkMap, Realm realm, String unixGroup ->
                assert sourceLinkMap.size() == 1
                assert sourceLinkMap.containsKey(source)
                assert sourceLinkMap.containsValue(target)
            }
        }

        when:
        createViewByPidJob.execute()

        then:
        dataFile.fileLinked
        dataFile.dateLastChecked
        seqTrack.dataInstallationState == SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState == SeqTrack.DataProcessingState.NOT_STARTED
    }
}
