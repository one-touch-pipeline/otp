package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyAnalysisResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.Entity

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
        return new File(instancePath.absoluteDataManagementPath, "plots")
    }

    @Override
    public String toString() {
        return "AI ${id}: ${instanceName} ${samplePair.toStringWithoutId()}"
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
        return new File(instancePath.absoluteDataManagementPath, "${individual.pid}_cnv_parameter.json")
    }

    File getGcCorrected() {
        return new File(getInstancePlotPath(), "${this.individual.pid}_gc_corrected.png")
    }

    File getQcGcCorrected() {
        return new File(getInstancePlotPath(), "${this.individual.pid}_qc_rep_corrected.png")
    }

    File getWgCoverage() {
        return new File(getInstancePlotPath(), "${this.individual.pid}_wholeGenome_coverage.png")
    }

    File getTcnDistancesCombinedStar() {
        return new File(getInstancePath().absoluteDataManagementPath, "${this.individual.pid}_tcn_distances_combined_star.png")
    }

    /**
     * Search for Files that is equal to the pattern for plot Extra in the instance absolute Path
     * @return List with Files that matches with the Pattern
     */
    List<File> getPlotExtra() {
        AceseqQc aceseqQc = AceseqQc.findByNumberAndAceseqInstance(1, this)
        assert aceseqQc
        List<File> resultList = new FileNameByRegexFinder().getFileNames(getInstancePath().absoluteDataManagementPath.toString(),
                //If variables contain dots replace them if not they will be used by Regex
                ("${this.individual.pid}_plot_${aceseqQc.ploidyFactor}extra_${aceseqQc.purity}_").replace('.', '\\.')
                        + '.+\\.png').collect{new File(it)}
        return resultList
    }

    /**
     * Search for Files that is equal to the pattern for plot All in the instance absolute Path
     * @return List with Files that matches with the Pattern
     */
    List<File> getPlotAll() {
        //If variables contain dots replace them if not they will be used by Regex
        List<File> resultList = new FileNameByRegexFinder().getFileNames(getInstancePath().absoluteDataManagementPath.toString(),
                ("${this.individual.pid}_plot_").replace('.', '\\.')+ '.+_ALL\\.png').collect{new File(it)}
        return resultList
    }
}
