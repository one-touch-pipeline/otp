/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.jobs.AbstractCreateNotificationJob
import de.dkfz.tbi.otp.workflowExecution.*

abstract class AbstractCreateNotificationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    protected WorkflowRun run

    protected WorkflowStep workflowStep

    protected AbstractBamFile abstractBamFile

    protected AbstractCreateNotificationJob job

    protected TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RoddyBamFile,
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                WorkflowRun,
        ]
    }

    final static String NOTIFICATION_PROCESSING = " Notification for processing "
    final static String NOTIFICATION_RODDY = " Notification for Roddy "

    final static String COMBINED_CONFIG = """
{
  "RODDY": {
    "cvalues": {
      "BWA_ALIGNMENT_OPTIONS": {
        "value": "\\"-q 20\\""
      },
      "SAMBAMBA_MARKDUP_VERSION": {
        "type": "string",
        "value": "0.6.5"
      },
      "SAMBAMBA_MARKDUP_OPTS": {
        "type": "string",
        "value": "\\" -SOME_OPTS \\""
      },
      "SAMBAMBA_VERSION": {
        "type": "string",
        "value": "2.0"
      },
      "BWA_VERSION": {
        "type": "string",
        "value": "0.7.15"
      },
      "BWA_MEM_OPTIONS": {
        "type": "string",
        "value": "\\" -T 0 \\""
      },
      "SAMTOOLS_VERSION": {
        "type": "string",
        "value": "1.0"
      }
    }
  }
}
"""

    abstract protected String workflowName()

    abstract protected AbstractCreateNotificationJob createJob()

    abstract protected AbstractBamFile createRoddyBamFile()

    void setup() {
        run = createWorkflowRun([
                workflow: createWorkflow([
                        name: workflowName(),
                ]),
                combinedConfig: COMBINED_CONFIG,
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
        abstractBamFile = createRoddyBamFile()

        job = createJob()
        job.alignmentInfoService = new AlignmentInfoService()
        job.messageSourceService = Mock(MessageSourceService) {
            createMessage("notification.template.alignment.processing", _) >> NOTIFICATION_PROCESSING
            createMessage("notification.template.alignment.processing.roddy", _) >> NOTIFICATION_RODDY
        }
    }

    void "execute(workflowStep) should save the notification in workflow run"() {
        given:
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _) >> abstractBamFile
        }

        job.logService = Mock(LogService)

        final String notificationText = NOTIFICATION_PROCESSING + NOTIFICATION_RODDY

        when:
        job.execute(workflowStep)

        then:
        run.notificationText == notificationText

        then:
        1 * job.logService.addSimpleLogEntry(workflowStep, "Notification has been generated")
        1 * job.logService.addSimpleLogEntry(workflowStep, notificationText)
    }
}
