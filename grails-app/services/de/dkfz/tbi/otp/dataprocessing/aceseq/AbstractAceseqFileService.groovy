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
package de.dkfz.tbi.otp.dataprocessing.aceseq

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.ArtefactFileService
import de.dkfz.tbi.otp.dataprocessing.RoddyResultServiceTrait
import de.dkfz.tbi.otp.ngsdata.PlotType
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.stream.Collectors

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract trait AbstractAceseqFileService implements ArtefactFileService<AceseqInstance>, RoddyResultServiceTrait<AceseqInstance> {

    /**
     * Example
     * ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/$whole_genome_sequencing/view-by-pid/$PID/cnv_results/paired/tumor_control/2014-08-25_15h32/plots
     */
    private Path getPlotPath(AceseqInstance instance) {
        return getDirectoryPath(instance).resolve("plots")
    }

    Path getQcJsonFile(AceseqInstance instance) {
        return getDirectoryPath(instance).resolve("cnv_${instance.individual.pid}_parameter.json")
    }

    Path getPlot(AceseqInstance instance, PlotType plot) {
        switch (plot) {
            case PlotType.ACESEQ_GC_CORRECTED: return getPlotPath(instance).resolve("${instance.individual.pid}_gc_corrected.png")
            case PlotType.ACESEQ_QC_GC_CORRECTED: return getPlotPath(instance).resolve("${instance.individual.pid}_qc_rep_corrected.png")
            case PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR: return getDirectoryPath(instance)
                    .resolve("${instance.individual.pid}_tcn_distances_combined_star.png")
            case PlotType.ACESEQ_WG_COVERAGE: return getPlotPath(instance).resolve("control_${instance.individual.pid}_wholeGenome_coverage.png")
            default: throw new IllegalArgumentException("Unknown AceSeq PlotType \"${plot}\"")
        }
    }

    /**
     * Search for Files that is equal to the pattern for plot Extra/ALL in the instance absolute Path
     * @return List with Files that matches with the Pattern
     */
    @CompileDynamic
    List<Path> getPlots(AceseqInstance instance, PlotType plot) {
        Path workDirectory = getDirectoryPath(instance)
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
                // If variables contain dots replace them if not they will be used by Regex
                pattern = "${instance.individual.pid}_plot_".replace('.', '\\.') + '.+_ALL\\.png'
                break
            default: throw new IllegalArgumentException("Unknown AceSeq PlotType \"${plot}\"")
        }

        return Files.list(workDirectory).collect(Collectors.toList()).findAll { it.fileName ==~ pattern }.sort()
    }

    List<Path> getAllFiles(AceseqInstance instance) {
        return getPlots(instance, PlotType.ACESEQ_ALL) + getPlots(instance, PlotType.ACESEQ_EXTRA) + [
                getPlot(instance, PlotType.ACESEQ_GC_CORRECTED),
                getPlot(instance, PlotType.ACESEQ_QC_GC_CORRECTED),
                getPlot(instance, PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR),
                getPlot(instance, PlotType.ACESEQ_WG_COVERAGE),
                getQcJsonFile(instance),
        ]
    }
}
