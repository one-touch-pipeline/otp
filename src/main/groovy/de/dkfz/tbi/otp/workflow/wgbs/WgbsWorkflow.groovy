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
package de.dkfz.tbi.otp.workflow.wgbs

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflow.jobs.SetCorrectPermissionJob
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerCheckQcJob
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerCleanUpJob
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerConditionalFailJob
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerFinishJob
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow

@Component
@Slf4j
@CompileStatic
class WgbsWorkflow extends PanCancerWorkflow {
    static final String WORKFLOW = "WGBS alignment"
    static final String OUTPUT_BAM = "BAM"

    @Override
    List<String> getJobBeanNames() {
        return [
                PanCancerConditionalFailJob.simpleName.uncapitalize(),
                WgbsPrepareJob.simpleName.uncapitalize(),
                WgbsExecuteJob.simpleName.uncapitalize(),
                WgbsValidationJob.simpleName.uncapitalize(),
                WgbsParseJob.simpleName.uncapitalize(),
                PanCancerCheckQcJob.simpleName.uncapitalize(),
                PanCancerCleanUpJob.simpleName.uncapitalize(),
                WgbsLinkJob.simpleName.uncapitalize(),
                SetCorrectPermissionJob.simpleName.uncapitalize(),
                PanCancerFinishJob.simpleName.uncapitalize(),
        ]
    }
}
