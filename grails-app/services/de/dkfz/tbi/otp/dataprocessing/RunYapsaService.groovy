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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance

@Transactional
class RunYapsaService extends AbstractBamFileAnalysisService<RunYapsaInstance> implements WithReferenceGenomeRestriction {

    private final static String RUN_YAPSA_RESULTS_PATH_PART = 'mutational_signatures_results'

    @Override
    protected String getProcessingStateCheck() {
        return "sp.runYapsaProcessingStatus = :needsProcessing AND " +
                "sp.snvProcessingStatus = 'NO_PROCESSING_NEEDED' AND " +
                "EXISTS (FROM RoddySnvCallingInstance sci " +
                "  WHERE sci.samplePair = sp AND " +
                "  sci.processingState = 'FINISHED' AND " +
                "  sci.withdrawn = false " +
                ") AND " +
                "NOT EXISTS (FROM RoddySnvCallingInstance sci " +
                "  WHERE sci.samplePair = sp AND " +
                "  sci.processingState = 'IN_PROGRESS' AND " +
                "  sci.withdrawn = false " +
                ") "
    }

    @Override
    Class<RunYapsaInstance> getAnalysisClass() {
        return RunYapsaInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.MUTATIONAL_SIGNATURE
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RUN_YAPSA
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME)
    }

    @Override
    String getConfigName() {
        RunYapsaConfig.name
    }

    @Override
    protected String getResultsPathPart() {
        return RUN_YAPSA_RESULTS_PATH_PART
    }
}
