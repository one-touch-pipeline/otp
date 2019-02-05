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
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.Entity

class IndelCallingInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity, RoddyAnalysisResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/indel_results/paired/tumor_control/2014-08-25_15h32
     */
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.indelSamplePairPath, instanceName)
    }

    @Override
    String toString() {
        return "ICI ${id}${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return RoddyWorkflowConfig.get(super.config.id)
    }

    List<File> getResultFilePathsToValidate() {
        return ["indel_${this.individual.pid}.vcf.gz", "indel_${this.individual.pid}.vcf.raw.gz"].collect {
            new File(getWorkDirectory(), it)
        }
    }

    File getCombinedPlotPath() {
        return new File(getWorkDirectory(), "screenshots/indel_somatic_functional_combined.pdf")
    }

    File getCombinedPlotPathTiNDA() {
        return new File(getWorkDirectory(), "snvs_${this.individual.pid}.GTfiltered_gnomAD.Germline.Rare.Rescue.png")
    }

    File getIndelQcJsonFile() {
        return new File(getWorkDirectory(), "indel.json")
    }

    File getSampleSwapJsonFile() {
        return new File(getWorkDirectory(), "checkSampleSwap.json")
    }
}
