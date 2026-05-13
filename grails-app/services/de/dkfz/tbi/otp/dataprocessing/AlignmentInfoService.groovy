/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerAlignmentInfoService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.workflowExecution.*

@Transactional
class AlignmentInfoService {

    OtpWorkflowService otpWorkflowService
    RoddyAlignmentInfoService roddyAlignmentInfoService
    CellRangerAlignmentInfoService cellRangerAlignmentInfoService

    AlignmentInfo getAlignmentInformationForRun(WorkflowRun run) {
        assert run: "No run provided"
        OtpWorkflow workflowBean = otpWorkflowService.lookupOtpWorkflowBean(run.workflow)
        return alignmentInfoServices.find { it.supports(workflowBean) }?.getAlignmentInfo(run)
    }

    /**
     * @deprecated use {@link RoddyAlignmentInfoService#extractCValuesMapFromJsonConfigString(String)} directly
     */
    @Deprecated
    Map<String, String> extractCValuesMapFromJsonConfigString(String config) {
        return roddyAlignmentInfoService.extractCValuesMapFromJsonConfigString(config)
    }

    /**
     * @deprecated method is part of the old workflow system
     */
    @Deprecated
    protected RoddyAlignmentInfo getRoddyAlignmentInformation(RoddyWorkflowConfig workflowConfig) {
        return roddyAlignmentInfoService.getRoddyAlignmentInformation(workflowConfig)
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link #getAlignmentInformationForRun(WorkflowRun)} instead
     */
    @Deprecated
    AlignmentInfo getAlignmentInformationFromConfig(AlignmentConfig config) {
        assert config
        if (config.class == RoddyWorkflowConfig) {
            return getRoddyAlignmentInformation((RoddyWorkflowConfig) config)
        }
        return config.alignmentInformation
    }

    private List<AbstractAlignmentInfoService> getAlignmentInfoServices() {
        return [roddyAlignmentInfoService, cellRangerAlignmentInfoService]
    }
}
