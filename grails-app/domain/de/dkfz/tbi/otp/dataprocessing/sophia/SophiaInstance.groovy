package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class SophiaInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity, RoddyAnalysisResult {

    private final static String SOPHIA_OUTPUT_FILE_SUFFIX = "filtered_somatic_minEventScore3.tsv"
    private final static String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"
    private final static String COMBINED_PLOT_FILE_SUFFIX = "filtered.tsv_score_3_scaled_merged.pdf"

    static hasMany = [
            roddyExecutionDirectoryNames: String
    ]


    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.sophiaSamplePairPath, instanceName)
    }


    @Override
    public String toString() {
        return "SI ${id}${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return RoddyWorkflowConfig.get(super.config.id)
    }

    File getFinalAceseqInputFile() {
        return new File(getWorkDirectory(), "svs_${individual.pid}_${SOPHIA_OUTPUT_FILE_SUFFIX}")
    }

    File getCombinedPlotPath() {
        return new File(getWorkDirectory(), "${fileNamePrefix}_${COMBINED_PLOT_FILE_SUFFIX}")
    }

    File getQcJsonFile() {
        return new File(getWorkDirectory(), QUALITY_CONTROL_JSON_FILE_NAME)
    }

    private String getFileNamePrefix() {
        "svs_${individual.pid}_${samplePair.sampleType1.name.toLowerCase()}-${samplePair.sampleType2.name.toLowerCase()}"
    }

    static SophiaInstance getLatestValidSophiaInstanceForSamplePair(SamplePair samplePair) {
        return SophiaInstance.findBySamplePairAndWithdrawnAndProcessingState(samplePair, false, AnalysisProcessingStates.FINISHED, [max:1, sort: "id", order: "desc"])
    }
}
