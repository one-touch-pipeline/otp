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

package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyAnalysisResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.Entity

class SophiaInstance extends BamFilePairAnalysis implements RoddyAnalysisResult {

    private final static String SOPHIA_OUTPUT_FILE_SUFFIX = "filtered_somatic_minEventScore3.tsv"
    private final static String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"
    private final static String COMBINED_PLOT_FILE_SUFFIX = "filtered.tsv_score_3_scaled_merged.pdf"

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]


    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.sophiaSamplePairPath, instanceName)
    }


    @Override
    String toString() {
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
        return SophiaInstance.findBySamplePairAndWithdrawnAndProcessingState(
                samplePair, false, AnalysisProcessingStates.FINISHED, [max:1, sort: "id", order: "desc"])
    }
}
