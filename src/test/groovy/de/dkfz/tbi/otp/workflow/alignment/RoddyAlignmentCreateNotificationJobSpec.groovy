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
package de.dkfz.tbi.otp.workflow.alignment

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.alignment.AbstractCreateNotificationJobSpec
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow

class RoddyAlignmentCreateNotificationJobSpec extends AbstractCreateNotificationJobSpec {

    @Override
    protected String workflowName() {
        return PanCancerWorkflow.WORKFLOW
    }

    @Override
    protected RoddyAlignmentCreateNotificationJob createJob() {
        RoddyAlignmentCreateNotificationJob job = new RoddyAlignmentCreateNotificationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> abstractBamFile
            0 * _
        }
        return job
    }

    @Override
    protected AbstractBamFile createRoddyBamFile() {
        return createRoddyBamFile(RoddyBamFile)
    }

    void "createNotificationText should return correct notification"() {
        when:
        job.createNotificationText(workflowStep)

        then:
        1 * job.messageSourceService.createMessage("notification.template.alignment.processing", _) >> { arguments ->
            assert arguments[0] == "notification.template.alignment.processing"
            assert arguments[1]["seqType"] == abstractBamFile.seqType.displayNameWithLibraryLayout
            assert arguments[1]["referenceGenome"] == abstractBamFile.referenceGenome
            assert arguments[1]["alignmentProgram"] == "BWA Version 0.7.15"
            assert arguments[1]["alignmentParameter"] == "\" -T 0 \""
        }
        1 * job.messageSourceService.createMessage("notification.template.alignment.processing.roddy", _) >> { arguments ->
            assert arguments[0] == "notification.template.alignment.processing.roddy"
            assert arguments[1]["mergingProgram"] == "Sambamba Version 0.6.5"
            assert arguments[1]["mergingParameter"] == "\" -SOME_OPTS \""
            assert arguments[1]["samtoolsProgram"] == "Version 1.0"
        }
    }
}
