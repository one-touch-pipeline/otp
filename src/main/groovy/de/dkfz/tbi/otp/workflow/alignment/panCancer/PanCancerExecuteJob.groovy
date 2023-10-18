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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.alignment.RoddyAlignmentExecuteJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
class PanCancerExecuteJob extends RoddyAlignmentExecuteJob implements PanCancerShared {

    @Autowired
    BedFileService bedFileService

    @Override
    protected final String getAnalysisConfiguration(SeqType seqType) {
        return seqType.needsBedFile ? 'exomeAnalysis' : 'qcAnalysis'
    }

    @Override
    protected final boolean getFilenameSectionKillSwitch() {
        return true
    }

    @Override
    protected final Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        Map<String, String> conf = super.getConfigurationValues(workflowStep, combinedConfig)

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        conf.putAll(roddyConfigValueService.getFilesToMerge(roddyBamFile))

        if (roddyBamFile.seqType.needsBedFile) {
            BedFile bedFile = roddyBamFile.bedFile
            File bedFilePath = bedFileService.filePath(bedFile) as File
            conf.put("TARGET_REGIONS_FILE", bedFilePath.toString())
            conf.put("TARGETSIZE", bedFile.targetSize.toString())
        }

        return conf
    }
}
