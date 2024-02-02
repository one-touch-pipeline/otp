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
package de.dkfz.tbi.otp.workflow.alignment.rna

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowShared
import de.dkfz.tbi.otp.workflow.alignment.RoddyAlignmentCreateNotificationJob

@Component
@Slf4j
class RnaAlignmentCreateNotificationJob extends RoddyAlignmentCreateNotificationJob implements AlignmentWorkflowShared {

    @Override
    protected String getProgramVersion(Map<String, String> config) {
        return config.get("STAR_VERSION") ? "STAR Version ${config.get("STAR_VERSION")}" : ""
    }

    @Override
    protected String getProgramParameter(Map<String, String> config) {
        return ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name ->
            config.get("STAR_PARAMS_${name}".toString())
        }.join(' ')
    }

    @Override
    protected String getMergingProgramVersion(Map<String, String> config) {
        return config.get('SAMBAMBA_VERSION') ? "Sambamba Version ${config.get('SAMBAMBA_VERSION')}" : ""
    }

    @Override
    protected String getMergingProgramParameter(Map<String, String> config) {
        return ""
    }
}
