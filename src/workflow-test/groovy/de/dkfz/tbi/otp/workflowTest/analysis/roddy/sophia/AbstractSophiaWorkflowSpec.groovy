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
package de.dkfz.tbi.otp.workflowTest.analysis.roddy.sophia

import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.Decider
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.SophiaDecider
import de.dkfz.tbi.otp.workflowTest.analysis.roddy.AbstractRoddyAnalysisWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37

import java.nio.file.Path

abstract class AbstractSophiaWorkflowSpec extends AbstractRoddyAnalysisWorkflowSpec<SophiaInstance> implements ReferenceGenomeHg37 {

    SophiaDecider sophiaDecider
    SophiaLinkFileService sophiaLinkFileService
    SophiaWorkFileService sophiaWorkFileService

    @Override
    List<Path> filesToCheck(SophiaInstance instance) {
        return [
                sophiaLinkFileService.getFinalAceseqInputFile(instance),
                sophiaWorkFileService.getFinalAceseqInputFile(instance),
                sophiaLinkFileService.getQcJsonFile(instance),
                sophiaWorkFileService.getQcJsonFile(instance),
                sophiaLinkFileService.getCombinedPlotPath(instance),
                sophiaWorkFileService.getCombinedPlotPath(instance),
        ]
    }

    @Override
    String getWorkflowName() {
        return SophiaWorkflow.WORKFLOW
    }

    @Override
    protected Decider getDecider() {
        return sophiaDecider
    }

    @Override
    void checkQc(SophiaInstance instance) {
        CollectionUtils.exactlyOneElement(SophiaQc.findAllBySophiaInstance(instance))
    }

    @Override
    void setupExternalBamFile() {
        super.setupExternalBamFile()
        DomainFactory.createExternallyProcessedBamFileQualityAssessment(QC_VALUES, bamFileControl)
        DomainFactory.createExternallyProcessedBamFileQualityAssessment(QC_VALUES, bamFileTumor)
    }

    Class<SophiaWorkflow> workflowComponentClass = SophiaWorkflow
}
