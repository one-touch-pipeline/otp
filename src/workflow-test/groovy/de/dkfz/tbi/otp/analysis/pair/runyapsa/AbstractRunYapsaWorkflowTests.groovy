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
package de.dkfz.tbi.otp.analysis.pair.runyapsa

import de.dkfz.tbi.otp.analysis.pair.roddy.AbstractRoddyBamFilePairAnalysisWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.Path
import java.time.Duration

abstract class AbstractRunYapsaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<RunYapsaInstance> {

    IndividualService individualService

    @Override
    void setupData() {
        SessionUtils.withTransaction {
            createRunYapsaInput()
            super.setupData()
        }
    }

    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createProcessingOptionLazy([
                name   : OptionName.PIPELINE_MIN_COVERAGE,
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
        createDirectories([new File(projectService.getSequencingDirectory(project).toString())])

        return DomainFactory.createRunYapsaConfig([
                programVersion: processingOptionService.findOptionAsString(OptionName.PIPELINE_RUNYAPSA_DEFAULT_VERSION),
                project       : samplePair.project,
                seqType       : samplePair.seqType,
                pipeline      : Pipeline.Name.RUN_YAPSA.pipeline,
        ])
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        doWithAuth(OPERATOR) {
            processingOptionService.createOrUpdate(
                    OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }

    void createRunYapsaInput() {
        int minConfidenceScore = 8
        File sourceSnvCallingInputFile

        if (seqType == SeqTypeService.wholeGenomePairedSeqType) {
            sourceSnvCallingInputFile = new File(workflowData, "snvs_stds_somatic_snvs_conf_${minConfidenceScore}_to_10-wgs.vcf")
        } else if (seqType == SeqTypeService.exomePairedSeqType) {
            sourceSnvCallingInputFile = new File(workflowData, "snvs_stds_somatic_snvs_conf_${minConfidenceScore}_to_10-wes.vcf")
        } else {
            throw new UnsupportedOperationException("The SeqType '${seqType}' is not supported by runYapsa workflow")
        }

        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(samplePair)
        Path runYapsaInputFile = CreateRoddyFileHelper.getSnvResultRequiredForRunYapsa(snvCallingInstance, minConfidenceScore, individualService)
        SamplePair sp = SamplePair.get(samplePair.id)
        sp.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        assert sp.save(flush: true)

        linkFileUtils.createAndValidateLinks([
                (sourceSnvCallingInputFile): new File(runYapsaInputFile.toString()),
        ], realm)
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RunYapsaWorkflow.groovy",
        ]
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> filesToCheck(RunYapsaInstance runYapsaInstance) {
        return []
    }

    @Override
    File getWorkflowData() {
        return new File(inputRootDirectory, 'runYapsa')
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(24)
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    @Override
    String getJobName() {
        return 'ExecuteRunYapsaJob'
    }
}
