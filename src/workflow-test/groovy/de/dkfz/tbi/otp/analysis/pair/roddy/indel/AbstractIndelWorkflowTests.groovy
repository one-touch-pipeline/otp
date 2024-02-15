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
package de.dkfz.tbi.otp.analysis.pair.roddy.indel

import de.dkfz.tbi.otp.analysis.pair.roddy.AbstractRoddyBamFilePairAnalysisWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.project.RoddyConfiguration
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path
import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

abstract class AbstractIndelWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<IndelCallingInstance> {

    IndelCallingService indelCallingService

    ProjectService projectService

    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createIndelSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        createDirectories([new File(projectService.getSequencingDirectory(project).toString())])

        return doWithAuth(OPERATOR) {
            projectService.configureIndelPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_NAME),
                            programVersion   : processingOptionService.findOptionAsString(PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(PIPELINE_RODDY_INDEL_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddyIndelWorkflow.groovy",
        ]
    }

    List<Path> filesToCheck(IndelCallingInstance indelCallingInstance) {
        return [
                indelCallingService.getResultFilePathsToValidate(indelCallingInstance),
                indelCallingService.getCombinedPlotPath(indelCallingInstance),
                indelCallingService.getIndelQcJsonFile(indelCallingInstance),
                indelCallingService.getSampleSwapJsonFile(indelCallingInstance),
        ].flatten()
    }

    @Override
    File getWorkflowData() {
        return new File(inputRootDirectory, 'indel')
    }

    @Override
    void checkAnalysisSpecific(IndelCallingInstance indelCallingInstance) {
        CollectionUtils.exactlyOneElement(IndelQualityControl.findAllByIndelCallingInstance(indelCallingInstance))
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.findAllByIndelCallingInstance(indelCallingInstance))
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(5)
    }
}
