package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class AceseqInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity, RoddyAnalysisResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String
    ]

    /**
     * Example: $OTP_ROOT_PATH/${project}/sequencing/$whole_genome_sequencing/view-by-pid/$PID/cnv_results/paired/tumor_control/2014-08-25_15h32
     */
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.aceseqSamplePairPath, instanceName)
    }

    /**
     * Example:  $OTP_ROOT_PATH/${project}/sequencing/$whole_genome_sequencing/view-by-pid/$PID/cnv_results/paired/tumor_control/2014-08-25_15h32/plots
     */
    File getInstancePlotPath() {
        return new File(getWorkDirectory(), "plots")
    }

    @Override
    public String toString() {
        return "AI ${id}${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return super.config
    }

    File getQcJsonFile() {
        return new File(getWorkDirectory(), "cnv_${individual.pid}_parameter.json")
    }

    enum AceseqPlot {
        GC_CORRECTED,
        QC_GC_CORRECTED,
        WG_COVERAGE,
        TCN_DISTANCE_COMBINED_STAR,
    }

    File getPlot(AceseqPlot plot) {
        switch (plot) {
            case AceseqPlot.GC_CORRECTED: return new File(getInstancePlotPath(), "${this.individual.pid}_gc_corrected.png")
            case AceseqPlot.QC_GC_CORRECTED: return new File(getInstancePlotPath(), "${this.individual.pid}_qc_rep_corrected.png")
            case AceseqPlot.TCN_DISTANCE_COMBINED_STAR: return new File(getWorkDirectory(), "${this.individual.pid}_tcn_distances_combined_star.png")
            case AceseqPlot.WG_COVERAGE: return new File(getInstancePlotPath(), "control_${this.individual.pid}_wholeGenome_coverage.png")
            default: throw new Exception()
        }
    }

    enum AceseqPlots {
        EXTRA,
        ALL,
    }

    /**
     * Search for Files that is equal to the pattern for plot Extra/ALL in the instance absolute Path
     * @return List with Files that matches with the Pattern
     */
    List<File> getPlots(AceseqPlots plot) {
        String pattern
        switch (plot) {
            case AceseqPlots.EXTRA:
                AceseqQc aceseqQc = AceseqQc.findByNumberAndAceseqInstance(1, this)
                assert aceseqQc
                //If variables contain dots replace them if not they will be used by Regex
                pattern = "${this.individual.pid}_plot_${aceseqQc.ploidyFactor}extra_${aceseqQc.tcc}_"
                        .replace('.', '\\.') + '.+\\.png'
                break
            case AceseqPlots.ALL:
                //If variables contain dots replace them if not they will be used by Regex
                pattern = "${this.individual.pid}_plot_".replace('.', '\\.') + '.+_ALL\\.png'
                break
            default: throw new Exception()
        }
        return new FileNameByRegexFinder().getFileNames(getWorkDirectory().toString(), pattern)
                .collect { new File(it) }.sort()
    }

    List<File> getAllFiles() {
        return [
                AceseqPlot.values().collect { getPlot(it) },
                AceseqPlots.values().collect { getPlots(it) },
                getQcJsonFile(),
        ].flatten()
    }
}
