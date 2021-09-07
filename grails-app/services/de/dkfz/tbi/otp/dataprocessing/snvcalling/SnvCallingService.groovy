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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService

import java.nio.file.Path

class SnvCallingService extends AbstractBamFileAnalysisService<AbstractSnvCallingInstance> implements RoddyBamFileAnalysis {

    private final static String SNV_RESULTS_PATH_PART = 'snv_results'
    private final static String SNV_RESULTS_PREFIX = 'snvs_'

    FileService fileService

    @Override
    protected String getProcessingStateCheck() {
        return "sp.snvProcessingStatus = :needsProcessing "
    }

    @Override
    Class<RoddySnvCallingInstance> getAnalysisClass() {
        return RoddySnvCallingInstance
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SNV
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_SNV
    }

    Path getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty(BamFilePairAnalysis bamFilePairAnalysis) {
        final Path WORK_DIRECTORY = getWorkDirectory(bamFilePairAnalysis.samplePair.findLatestSnvCallingInstance())
        final String MIN_CONF_SCORE = /[0-9]/
        final String MATCHER_FOR_FILE_REQUIRED_FOR_RUN_YAPSA =
                ".*${SNV_RESULTS_PREFIX}${bamFilePairAnalysis.individual.pid}_somatic_snvs_conf_${MIN_CONF_SCORE}_to_10.vcf"
        return fileService.getFoundFileInPathEnsureIsReadableAndNotEmpty(WORK_DIRECTORY, MATCHER_FOR_FILE_REQUIRED_FOR_RUN_YAPSA)
    }

    @Override
    protected String getResultsPathPart() {
        return SNV_RESULTS_PATH_PART
    }

    Path getSnvCallingResult(AbstractSnvCallingInstance instance) {
        return getWorkDirectory(instance).resolve("${SNV_RESULTS_PREFIX}${instance.individual.pid}_raw.vcf.gz")
    }

    Path getSnvDeepAnnotationResult(AbstractSnvCallingInstance instance) {
        return getWorkDirectory(instance).resolve("${SNV_RESULTS_PREFIX}${instance.individual.pid}.vcf.gz")
    }

    Path getCombinedPlotPath(AbstractSnvCallingInstance instance) {
        return getWorkDirectory(instance).resolve("${SNV_RESULTS_PREFIX}${instance.individual.pid}_allSNVdiagnosticsPlots.pdf")
    }
}
