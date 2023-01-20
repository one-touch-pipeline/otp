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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.PlotType
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.stream.Collectors

@CompileDynamic
@Transactional
class AceseqService extends AbstractBamFileAnalysisService<AceseqInstance> implements RoddyBamFileAnalysis, WithReferenceGenomeRestriction {

    private final static String ACESEQ_RESULTS_PATH_PART = 'cnv_results'

    @Override
    protected String getProcessingStateCheck() {
        return "sp.aceseqProcessingStatus = :needsProcessing AND " +
                "sp.sophiaProcessingStatus = 'NO_PROCESSING_NEEDED' AND " +
                "EXISTS (FROM SophiaInstance si " +
                "  WHERE si.samplePair = sp AND " +
                "  si.processingState = 'FINISHED' AND " +
                "  si.withdrawn = false " +
                ") AND " +
                "NOT EXISTS (FROM SophiaInstance si " +
                "  WHERE si.samplePair = sp AND " +
                "  si.processingState = 'IN_PROGRESS' AND " +
                "  si.withdrawn = false " +
                ") "
    }

    final Class<AceseqInstance> analysisClass = AceseqInstance

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.ACESEQ
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_ACESEQ
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME)
    }

    @Override
    protected String getResultsPathPart() {
        return ACESEQ_RESULTS_PATH_PART
    }

    /**
     * Example
     * ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/$whole_genome_sequencing/view-by-pid/$PID/cnv_results/paired/tumor_control/2014-08-25_15h32/plots
     */
    private Path getPlotPath(AceseqInstance instance) {
        return getWorkDirectory(instance).resolve("plots")
    }

    Path getQcJsonFile(AceseqInstance instance) {
        return getWorkDirectory(instance).resolve("cnv_${instance.individual.pid}_parameter.json")
    }

    Path getPlot(AceseqInstance instance, PlotType plot) {
        switch (plot) {
            case PlotType.ACESEQ_GC_CORRECTED: return getPlotPath(instance).resolve("${instance.individual.pid}_gc_corrected.png")
            case PlotType.ACESEQ_QC_GC_CORRECTED: return getPlotPath(instance).resolve("${instance.individual.pid}_qc_rep_corrected.png")
            case PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR: return getWorkDirectory(instance)
                    .resolve("${instance.individual.pid}_tcn_distances_combined_star.png")
            case PlotType.ACESEQ_WG_COVERAGE: return getPlotPath(instance).resolve("control_${instance.individual.pid}_wholeGenome_coverage.png")
            default: throw new IllegalArgumentException("Unknown AceSeq PlotType \"${plot}\"")
        }
    }

    /**
     * Search for Files that is equal to the pattern for plot Extra/ALL in the instance absolute Path
     * @return List with Files that matches with the Pattern
     */
    List<Path> getPlots(AceseqInstance instance, PlotType plot) {
        Path workDirectory = getWorkDirectory(instance)
        String pattern

        if (!Files.exists(workDirectory)) {
            return []
        }

        switch (plot) {
            case PlotType.ACESEQ_EXTRA:
                AceseqQc aceseqQc = CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumberAndAceseqInstance(1, instance))
                // If variables contain dots replace them, otherwise they will be used by Regex
                DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH)
                decimalFormat.applyPattern("0.##")
                pattern = "${instance.individual.pid}_plot_${aceseqQc.ploidyFactor}extra_${decimalFormat.format(aceseqQc.tcc)}_"
                        .replace('.', '\\.') + '.+\\.png'
                break
            case PlotType.ACESEQ_ALL:
                //If variables contain dots replace them if not they will be used by Regex
                pattern = "${instance.individual.pid}_plot_".replace('.', '\\.') + '.+_ALL\\.png'
                break
            default: throw new IllegalArgumentException("Unknown AceSeq PlotType \"${plot}\"")
        }

        return Files.list(workDirectory).collect(Collectors.toList()).findAll { it.fileName ==~ pattern }.sort()
    }

    List<Path> getAllFiles(AceseqInstance instance) {
        return [
                getPlot(instance, PlotType.ACESEQ_GC_CORRECTED),
                getPlot(instance, PlotType.ACESEQ_QC_GC_CORRECTED),
                getPlot(instance, PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR),
                getPlot(instance, PlotType.ACESEQ_WG_COVERAGE),
                getPlots(instance, PlotType.ACESEQ_ALL),
                getPlots(instance, PlotType.ACESEQ_EXTRA),
                getQcJsonFile(instance),
        ].flatten()
    }
}
