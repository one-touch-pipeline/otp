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

package workflows.analysis.pair.runyapsa

import grails.plugin.springsecurity.SpringSecurityUtils
import workflows.analysis.pair.AbstractRoddyBamFilePairAnalysisWorkflowTests

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*

import java.time.Duration

abstract class AbstractRunYapsaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<RunYapsaInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE,
                type   : Pipeline.Type.MUTATIONAL_SIGNATURE.toString(),
                project: null,
                value  : "20",
        ])
        DomainFactory.createRunYapsaPipelineLazy()
        DomainFactory.createRunYapsaSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(project.projectSequencingDirectory, realm)

        config = DomainFactory.createRunYapsaConfig([
                programVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_DEFAULT_VERSION),
                project       : samplePair.project,
                seqType       : samplePair.seqType,
                pipeline      : Pipeline.Name.RUN_YAPSA.pipeline,
        ])


    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processingOptionService.createOrUpdate(
                    OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }


    void createRunYapsaInput() {
        File sourceSnvCallingInputFile

        if (seqType == SeqTypeService.wholeGenomePairedSeqType) {
            sourceSnvCallingInputFile = new File(workflowData, "snvs_stds_somatic_snvs_conf_8_to_10-wgs.vcf")
        } else if (seqType == SeqTypeService.exomePairedSeqType) {
            sourceSnvCallingInputFile = new File(workflowData, "snvs_stds_somatic_snvs_conf_8_to_10-wes.vcf")
        } else {
            throw new Exception("The SeqType '${seqType}' is not supported by runYapsa workflow")
        }

        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(samplePair)
        File runYapsaInputFile = snvCallingInstance.getResultRequiredForRunYapsa()
        samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save(flush: true)

        linkFileUtils.createAndValidateLinks([
                (sourceSnvCallingInputFile): runYapsaInputFile,
        ], realm)
    }


    void executeTest() {
        createRunYapsaInput()

        super.executeTest()
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RunYapsaWorkflow.groovy",
        ]
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<File> filesToCheck(RunYapsaInstance runYapsaInstance) {
        return []
    }


    @Override
    File getWorkflowData() {
        new File(getInputRootDirectory(), 'runYapsa')
    }

    @Override
    Duration getTimeout() {
        Duration.ofHours(24)
    }
}
