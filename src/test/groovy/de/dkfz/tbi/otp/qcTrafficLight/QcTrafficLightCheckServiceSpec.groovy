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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class QcTrafficLightCheckServiceSpec extends Specification implements IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                Comment,
                DataFile,
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    @Unroll
    void "handleQcCheck, if status is #status, then #text"() {
        given:
        int notifyCount = callNotify ? 1 : 0

        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    notifyCount * informResultsAreBlocked(_)
                },
        ])

        boolean called = false
        Closure closure = {
            called = true
        }

        when:
        service.handleQcCheck(bamFile, closure)

        then:
        called == callCallback

        where:
        status                                                   || callCallback | callNotify
        AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED     || true         | false
        AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED       || false        | true
        AbstractMergedBamFile.QcTrafficLightStatus.AUTO_ACCEPTED || true         | true
        AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED     || true         | false

        text = "${callCallback ? '' : 'do not '}call the callback and ${callNotify ? '' : 'do not '} call the notify"
    }

    @Unroll
    void "handleQcCheck, if status is #status, then do not call the callback nor call the notification, but throw an exception"() {
        given:
        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    0 * informResultsAreBlocked(_)
                },
        ])

        Closure closure = {
            throw new AssertionError('should not be called')
        }

        when:
        service.handleQcCheck(bamFile, closure)

        then:
        RuntimeException e = thrown()
        e.message.contains(status.toString())

        where:
        status << AbstractMergedBamFile.QcTrafficLightStatus.values().findAll {
            it.jobLinkCase == AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR ||
                    it.jobNotifyCase == AbstractMergedBamFile.QcTrafficLightStatus.JobNotifyCase.SHOULD_NOT_OCCUR
        }
    }
}
