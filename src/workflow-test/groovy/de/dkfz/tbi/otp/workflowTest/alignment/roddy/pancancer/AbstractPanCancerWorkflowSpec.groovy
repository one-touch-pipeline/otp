/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.pancancer

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.AbstractWorkflowDecider
import de.dkfz.tbi.otp.workflowExecution.decider.PanCancerDecider
import de.dkfz.tbi.otp.workflowTest.alignment.roddy.AbstractRoddyAlignmentWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.roddy.RoddyFileAssertHelper

/**
 * base class for all PanCancer workflow tests
 */
abstract class AbstractPanCancerWorkflowSpec extends AbstractRoddyAlignmentWorkflowSpec {

    PanCancerDecider panCancerDecider

    Class<? extends OtpWorkflow> workflowComponentClass = PanCancerWorkflow

    @Override
    protected AbstractWorkflowDecider getDecider() {
        return panCancerDecider
    }

    @Override
    String getWorkflowName() {
        return PanCancerWorkflow.WORKFLOW
    }

    @Override
    protected boolean isFastQcRequired() {
        return true
    }

    void "test align lanes only, no base bam file exists, one lane, all fine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            decide(3, 1)
        }

        when:
        execute(1, 2)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    @Override
    protected void assertWorkflowFileSystemState(RoddyBamFile bamFile) {
        RoddyFileAssertHelper.assertFileSystemState(bamFile, roddyBamFileService)
    }

    @Override
    protected void assertWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile) {
        RoddyFileAssertHelper.assertWorkDirectoryFileSystemState(bamFile, isBaseBamFile, roddyBamFileService, roddyConfigService)
    }
}
