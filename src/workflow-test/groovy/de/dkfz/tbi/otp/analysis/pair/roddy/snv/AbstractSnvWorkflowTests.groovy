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
package de.dkfz.tbi.otp.analysis.pair.roddy.snv

import de.dkfz.tbi.otp.analysis.pair.roddy.AbstractRoddyBamFilePairAnalysisWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.project.RoddyConfiguration

import java.nio.file.Path

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

abstract class AbstractSnvWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<RoddySnvCallingInstance> {

    LsdfFilesService lsdfFilesService
    ProjectService projectService
    SnvCallingService snvCallingService

    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createSnvSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        createDirectories([new File(projectService.getSequencingDirectory(project).toString())])

        config = doWithAuth(OPERATOR) {
            projectService.configureSnvPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME),
                            programVersion   : processingOptionService.findOptionAsString(PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        return createAndSetup_Bwa06_1K_ReferenceGenome()
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddySnvWorkflow.groovy",
        ]
    }

    @Override
    File getWorkflowData() {
        new File(inputRootDirectory, 'snv')
    }

    @Override
    List<Path> filesToCheck(RoddySnvCallingInstance instance) {
        return [
                snvCallingService.getSnvCallingResult(instance),
                snvCallingService.getSnvDeepAnnotationResult(instance),
        ]
    }
}
