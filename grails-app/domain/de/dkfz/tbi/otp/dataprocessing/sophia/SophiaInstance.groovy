package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class SophiaInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity, RoddyAnalysisResult {

    final static String SOPHIA_OUTPUT_FILE_SUFFIX = "filtered_somatic_minEventScore3.tsv"
    static final String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"

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
        return super.config
    }

    File getFinalAceseqInputFile() {
        return new File(getInstancePath().absoluteDataManagementPath, "svs_${individual.pid}_${SOPHIA_OUTPUT_FILE_SUFFIX}")
    }

    File getQcJsonFile() {
       return new File(getInstancePath().absoluteDataManagementPath, QUALITY_CONTROL_JSON_FILE_NAME)
    }
}
