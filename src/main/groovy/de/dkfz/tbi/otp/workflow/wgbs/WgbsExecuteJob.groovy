/*
 * Copyright 2011-2021 The OTP authors
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.ngsdata.BedFileService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerShared
import de.dkfz.tbi.otp.workflow.shared.JobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
@CompileStatic
class WgbsExecuteJob extends AbstractExecuteRoddyPipelineJob implements PanCancerShared {

    @Autowired
    BedFileService bedFileService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    RoddyBamFileService roddyBamFileService

    @Override
    protected final RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getRoddyBamFile(workflowStep)
    }

    @Override
    protected final String getRoddyWorkflowName() {
        return "AlignmentAndQCWorkflows"
    }

    @Override
    protected final String getAnalysisConfiguration(SeqType seqType) {
        return "bisulfiteCoreAnalysis"
    }

    @Override
    protected final boolean getFilenameSectionKillSwitch() {
        return true
    }

    @Override
    protected final Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        Map<String, String> conf = [:]

        conf.putAll(roddyConfigValueService.getAlignmentValues(roddyBamFile, combinedConfig))
        conf.putAll(roddyConfigValueService.getChromosomeIndexParameterWithMitochondrion(roddyBamFile.referenceGenome))

        if (roddyBamFile.referenceGenome.cytosinePositionsIndex) {
            conf.put("CYTOSINE_POSITIONS_INDEX", referenceGenomeService.cytosinePositionIndexFilePath(roddyBamFile.referenceGenome).absolutePath)
        } else {
            throw new JobFailedException("Cytosine position index for reference genome ${roddyBamFile.referenceGenome} is not defined.")
        }

        return conf
    }

    @Override
    protected final List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        return ["--usemetadatatable=${roddyBamFileService.getWorkMetadataTableFile(roddyBamFile)}" as String]
    }
}
