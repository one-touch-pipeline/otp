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
package de.dkfz.tbi.otp.workflowTest.analysis.roddy.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.Decider
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.SnvDecider
import de.dkfz.tbi.otp.workflowTest.analysis.roddy.AbstractRoddyAnalysisWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg37

import java.nio.file.Path

abstract class AbstractSnvCallingWorkflowSpec extends AbstractRoddyAnalysisWorkflowSpec<RoddySnvCallingInstance> implements ReferenceGenomeHg37 {

    SnvDecider snvDecider
    SnvLinkFileService snvLinkFileService
    SnvWorkFileService snvWorkFileService

    @Override
    void setupData() {
        super.setupData()
        log.debug("Load SNV virtualenvs")
        createFragmentAndSelector("virtualenvs", """
            {
                "RODDY": {
                    "cvalues": {
                        "tbiLsfVirtualEnvDir": {
                            "value": "${configService.workflowTestRoddyVirtualEnvsBaseDir}/python_2.7.9_SNVCalling_1.2.166-1",
                            "type": "path"
                        }
                    }
                }
            }
        """, [
                workflows: [workflowAnalysis],
        ])
    }

    @Override
    List<Path> filesToCheck(RoddySnvCallingInstance instance) {
        return [
                snvLinkFileService.getSnvCallingResult(instance),
                snvWorkFileService.getSnvCallingResult(instance),
                snvLinkFileService.getSnvDeepAnnotationResult(instance),
                snvWorkFileService.getSnvDeepAnnotationResult(instance),
                snvLinkFileService.getCombinedPlotPath(instance),
                snvWorkFileService.getCombinedPlotPath(instance),
        ]
    }

    @Override
    String getWorkflowName() {
        return SnvWorkflow.WORKFLOW
    }

    @Override
    protected Decider getDecider() {
        return snvDecider
    }

    @SuppressWarnings("EmptyMethodInAbstractClass") // Makes no sense to implement this in each subclass
    @Override
    void checkQc(RoddySnvCallingInstance instance) { }

    Class<SnvWorkflow> workflowComponentClass = SnvWorkflow
}
