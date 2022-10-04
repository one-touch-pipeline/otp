/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.analysis.pair.roddy.sophia

import de.dkfz.tbi.otp.analysis.pair.roddy.AbstractRoddyBamFilePairAnalysisWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.SophiaService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.project.RoddyConfiguration
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.nio.file.Path
import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

abstract class AbstractSophiaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<SophiaInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService
    SophiaService sophiaService

    @Override
    void setupData() {
        SessionUtils.withTransaction {
            linkQualityControlFiles()
            super.setupData()
        }
    }

    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createSophiaPipelineLazy()
        DomainFactory.createSophiaSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        createDirectories([new File(projectService.getSequencingDirectory(project).toString())])

        config = doWithAuth(OPERATOR) {
            projectService.configureSophiaPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME),
                            programVersion   : processingOptionService.findOptionAsString(PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        doWithAuth(OPERATOR) {
            processingOptionService.createOrUpdate(
                    PIPELINE_SOPHIA_REFERENCE_GENOME,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }

    void linkQualityControlFiles() {
        File tumorInsertSizeFile = new File(workflowData, "tumor_HCC1187-div128_insertsize_plot.png_qcValues.txt")
        File controlInsertSizeFile = new File(workflowData, "blood_HCC1187-div128_insertsize_plot.png_qcValues.txt")

        File finalTumorInsertSizeFile = bamFileTumor.finalInsertSizeFile
        File finalControlInsertSizeFile = bamFileControl.finalInsertSizeFile

        LogThreadLocal.withThreadLog(System.out) {
            linkFileUtils.createAndValidateLinks([
                    (tumorInsertSizeFile)  : finalTumorInsertSizeFile,
                    (controlInsertSizeFile): finalControlInsertSizeFile,
            ], realm)
        }
    }

    @Override
    void setupExternalBamFile() {
        super.setupExternalBamFile()
        DomainFactory.createExternalProcessedMergedBamFileQualityAssessment(QC_VALUES, bamFileControl)
        DomainFactory.createExternalProcessedMergedBamFileQualityAssessment(QC_VALUES, bamFileTumor)
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddySophiaWorkflow.groovy",
        ]
    }

    @Override
    List<Path> filesToCheck(SophiaInstance sophiaInstance) {
        return [
                sophiaService.getFinalAceseqInputFile(sophiaInstance),
                sophiaService.getQcJsonFile(sophiaInstance),
                sophiaService.getCombinedPlotPath(sophiaInstance),
        ]
    }

    @Override
    void checkAnalysisSpecific(SophiaInstance sophiaInstance) {
        CollectionUtils.exactlyOneElement(SophiaQc.findAllBySophiaInstance(sophiaInstance))
    }

    @Override
    File getWorkflowData() {
        new File(inputRootDirectory, 'sophia')
    }

    @Override
    Duration getTimeout() {
        Duration.ofHours(5)
    }
}
