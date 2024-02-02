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
package de.dkfz.tbi.otp.dataprocessing.sophia

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.CollectionUtils

@ManagedEntity
class SophiaInstance extends BamFilePairAnalysis implements RoddyAnalysisResult, RoddyResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]

    /**
     * @deprecated use {@link SophiaService#getWorkDirectory()}
     */
    @Override
    @Deprecated
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.sophiaSamplePairPath, instanceName)
    }

    @Override
    String toString() {
        return "SI ${id}${withdrawn ? ' (withdrawn)' : ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return sampleType2BamFile.referenceGenome
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return RoddyWorkflowConfig.get(super.config.id)
    }

    static SophiaInstance getLatestValidSophiaInstanceForSamplePair(SamplePair samplePair) {
        return CollectionUtils.atMostOneElement(SophiaInstance.findAllBySamplePairAndWithdrawnAndProcessingState(
                samplePair, false, AnalysisProcessingStates.FINISHED, [max : 1, sort : "id", order : "desc", ]))
    }
}
