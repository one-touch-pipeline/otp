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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance

import java.nio.file.Path

@Transactional
class SophiaService extends AbstractBamFileAnalysisService<SophiaInstance> implements RoddyBamFileAnalysis, WithReferenceGenomeRestriction {

    private final static String SOPHIA_RESULTS_PATH_PART = 'sv_results'
    private final static String SOPHIA_OUTPUT_FILE_SUFFIX = "filtered_somatic_minEventScore3.tsv"
    private final static String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"
    private final static String COMBINED_PLOT_FILE_SUFFIX = "filtered.tsv_score_3_scaled_merged.pdf"

    @Override
    protected String getProcessingStateCheck() {
        return "sp.sophiaProcessingStatus = :needsProcessing "
    }

    final Class<SophiaInstance> analysisClass = SophiaInstance

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SOPHIA
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_SOPHIA
    }

    @Override
    protected String pipelineSpecificBamFileChecks(String number) {
        return """
        AND (
            ambf${number}.class = de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
            OR (
                ambf${number}.class = de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
                AND ambf${number}.insertSizeFile IS NOT NULL
                AND ambf${number}.maximumReadLength IS NOT NULL
                AND EXISTS (
                    FROM ExternallyProcessedBamFileQualityAssessment epmbfqa
                    WHERE epmbfqa.abstractBamFile = ambf${number}
                )
            )
        )
        """.replaceAll('\n', ' ')
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME)
    }

    @Override
    protected String getResultsPathPart() {
        return SOPHIA_RESULTS_PATH_PART
    }

    Path getFinalAceseqInputFile(SophiaInstance instance) {
        return getWorkDirectory(instance).resolve("svs_${instance.individual.pid}_${SOPHIA_OUTPUT_FILE_SUFFIX}")
    }

    Path getCombinedPlotPath(SophiaInstance instance) {
        return getWorkDirectory(instance).resolve("${getFileNamePrefix(instance)}_${COMBINED_PLOT_FILE_SUFFIX}")
    }

    Path getQcJsonFile(SophiaInstance instance) {
        return getWorkDirectory(instance).resolve(QUALITY_CONTROL_JSON_FILE_NAME)
    }

    private String getFileNamePrefix(SophiaInstance instance) {
        return "svs_${instance.individual.pid}_${instance.samplePair.sampleType1.name.toLowerCase()}-${instance.samplePair.sampleType2.name.toLowerCase()}"
    }
}
