/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.analysis.roddy.indelcalling

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelLinkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelQualityControl
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelSampleSwapDetection
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.Decider
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.IndelDecider
import de.dkfz.tbi.otp.workflowTest.analysis.roddy.AbstractRoddyAnalysisWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37

import java.nio.file.Path

abstract class AbstractIndelCallingWorkflowSpec extends AbstractRoddyAnalysisWorkflowSpec<IndelCallingInstance> implements ReferenceGenomeHg37 {

    IndelDecider indelDecider
    IndelLinkFileService indelLinkFileService
    IndelWorkFileService indelWorkFileService

    @Override
    List<Path> filesToCheck(IndelCallingInstance instance) {
        return [
                indelLinkFileService.getCombinedPlotPath(instance),
                indelWorkFileService.getCombinedPlotPath(instance),
                indelLinkFileService.getCombinedPlotPathTiNDA(instance),
                indelWorkFileService.getCombinedPlotPathTiNDA(instance),
                indelLinkFileService.getIndelQcJsonFile(instance),
                indelWorkFileService.getIndelQcJsonFile(instance),
                indelLinkFileService.getSampleSwapJsonFile(instance),
                indelWorkFileService.getSampleSwapJsonFile(instance),
        ] + indelLinkFileService.getResultFilePathsToValidate(instance) +
                indelWorkFileService.getResultFilePathsToValidate(instance)
    }

    @Override
    String getWorkflowName() {
        return IndelWorkflow.WORKFLOW
    }

    @Override
    protected Decider getDecider() {
        return indelDecider
    }

    @Override
    void checkQc(IndelCallingInstance instance) {
        CollectionUtils.exactlyOneElement(IndelQualityControl.findAllByIndelCallingInstance(instance))
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.findAllByIndelCallingInstance(instance))
    }

    Class<IndelWorkflow> workflowComponentClass = IndelWorkflow
}
