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

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyAnalysisResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.PlotType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.Entity

import java.text.DecimalFormat
import java.text.NumberFormat

class AceseqInstance extends BamFilePairAnalysis implements RoddyAnalysisResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]

    /**
     * Example:
     * ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/$whole_genome_sequencing/view-by-pid/$PID/cnv_results/paired/tumor_control/2014-08-25_15h32
     */
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.aceseqSamplePairPath, instanceName)
    }

    /**
     * Example
     * ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/$whole_genome_sequencing/view-by-pid/$PID/cnv_results/paired/tumor_control/2014-08-25_15h32/plots
     */
    File getInstancePlotPath() {
        return new File(getWorkDirectory(), "plots")
    }

    @Override
    String toString() {
        return "AI ${id}${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return RoddyWorkflowConfig.get(super.config.id)
    }

    File getQcJsonFile() {
        return new File(getWorkDirectory(), "cnv_${individual.pid}_parameter.json")
    }

    File getPlot(PlotType plot) {
        switch (plot) {
            case PlotType.ACESEQ_GC_CORRECTED: return new File(getInstancePlotPath(), "${this.individual.pid}_gc_corrected.png")
            case PlotType.ACESEQ_QC_GC_CORRECTED: return new File(getInstancePlotPath(), "${this.individual.pid}_qc_rep_corrected.png")
            case PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR: return new File(getWorkDirectory(), "${this.individual.pid}_tcn_distances_combined_star.png")
            case PlotType.ACESEQ_WG_COVERAGE: return new File(getInstancePlotPath(), "control_${this.individual.pid}_wholeGenome_coverage.png")
            default: throw new Exception()
        }
    }


    /**
     * Search for Files that is equal to the pattern for plot Extra/ALL in the instance absolute Path
     * @return List with Files that matches with the Pattern
     */
    List<File> getPlots(PlotType plot) {
        String pattern
        switch (plot) {
            case PlotType.ACESEQ_EXTRA:
                AceseqQc aceseqQc = AceseqQc.findByNumberAndAceseqInstance(1, this)
                assert aceseqQc
                //If variables contain dots replace them if not they will be used by Regex
                DecimalFormat decimalFormat = (DecimalFormat)NumberFormat.getInstance(Locale.ENGLISH)
                decimalFormat.applyPattern("0.##")
                pattern = "${this.individual.pid}_plot_${aceseqQc.ploidyFactor}extra_${decimalFormat.format(aceseqQc.tcc)}_"
                        .replace('.', '\\.') + '.+\\.png'
                break
            case PlotType.ACESEQ_ALL:
                //If variables contain dots replace them if not they will be used by Regex
                pattern = "${this.individual.pid}_plot_".replace('.', '\\.') + '.+_ALL\\.png'
                break
            default: throw new Exception()
        }

        if (!getWorkDirectory().exists()) {
            return null
        }

        return new FileNameByRegexFinder().getFileNames(getWorkDirectory().toString(), pattern)
                .collect { new File(it) }.sort()
    }

    List<File> getAllFiles() {
        return [
                getPlot(PlotType.ACESEQ_GC_CORRECTED),
                getPlot(PlotType.ACESEQ_QC_GC_CORRECTED),
                getPlot(PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR),
                getPlot(PlotType.ACESEQ_WG_COVERAGE),
                getPlots(PlotType.ACESEQ_ALL),
                getPlots(PlotType.ACESEQ_EXTRA),
                getQcJsonFile(),
        ].flatten()
    }
}
