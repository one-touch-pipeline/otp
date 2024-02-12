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
package de.dkfz.tbi.otp.dataprocessing.indelcalling

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.*

import java.nio.file.Path

@Transactional
class IndelCallingService extends AbstractBamFileAnalysisService<IndelCallingInstance> implements RoddyBamFileAnalysis {

    private final static String INDEL_RESULTS_PATH_PART = 'indel_results'
    private final static String INDEL_RESULTS_PREFIX = 'indel_'

    @Override
    protected String getProcessingStateCheck() {
        return "sp.indelProcessingStatus = :needsProcessing "
    }

    final Class<IndelCallingInstance> analysisClass = IndelCallingInstance

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.INDEL
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_INDEL
    }

    @Override
    protected String pipelineSpecificBamFileChecks(String number) {
        return "AND ambf${number}.class in (de.dkfz.tbi.otp.dataprocessing.RoddyBamFile, de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile)"
    }

    @Override
    protected String getResultsPathPart() {
        return INDEL_RESULTS_PATH_PART
    }

    List<Path> getResultFilePathsToValidate(IndelCallingInstance instance) {
        return ["${INDEL_RESULTS_PREFIX}${instance.individual.pid}.vcf.gz", "${INDEL_RESULTS_PREFIX}${instance.individual.pid}.vcf.raw.gz"].collect {
            getWorkDirectory(instance).resolve(it)
        }
    }

    Path getCombinedPlotPath(IndelCallingInstance instance) {
        return getWorkDirectory(instance).resolve("screenshots/${INDEL_RESULTS_PREFIX}somatic_functional_combined.pdf")
    }

    Path getCombinedPlotPathTiNDA(IndelCallingInstance instance) {
        return getWorkDirectory(instance).resolve("snvs_${instance.individual.pid}.GTfiltered_gnomAD.Germline.Rare.Rescue.png")
    }

    Path getIndelQcJsonFile(IndelCallingInstance instance) {
        return getWorkDirectory(instance).resolve("indel.json")
    }

    Path getSampleSwapJsonFile(IndelCallingInstance instance) {
        return getWorkDirectory(instance).resolve("checkSampleSwap.json")
    }
}
