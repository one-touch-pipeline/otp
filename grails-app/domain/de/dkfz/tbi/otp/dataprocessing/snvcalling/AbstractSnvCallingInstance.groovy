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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.OtpPath

/**
 * For each tumor-control pair the snv pipeline will be called.
 * The AbstractSnvCallingInstance symbolizes one call of the pipeline.
 */
abstract class AbstractSnvCallingInstance extends BamFilePairAnalysis {

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32
     */
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.snvSamplePairPath, instanceName)
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32/config.txt
     */
    OtpPath getConfigFilePath() {
        return new OtpPath(instancePath, "config.txt")
    }

    File getSnvCallingResult() {
        new File(getWorkDirectory(), "snvs_${individual.pid}_raw.vcf.gz")
    }

    File getSnvDeepAnnotationResult() {
        new File(getWorkDirectory(), "snvs_${individual.pid}.vcf.gz")
    }

    File getResultRequiredForRunYapsa() {
        new File(getWorkDirectory(), "snvs_${individual.pid}_somatic_snvs_conf_8_to_10.vcf")
    }

    File getCombinedPlotPath() {
        return new File(getWorkDirectory(), "snvs_${getIndividual().pid}_allSNVdiagnosticsPlots.pdf")
    }

    @Override
    String toString() {
        return "SCI ${id} ${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }
}
