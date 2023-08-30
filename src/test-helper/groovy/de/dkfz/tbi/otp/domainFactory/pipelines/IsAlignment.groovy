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
package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.ngsdata.DomainFactory

trait IsAlignment implements IsPipeline {

    Map<String, ?> getDefaultValuesForAbstractQualityAssessment() {
        return [
                qcBasesMapped                  : 0,
                totalReadCounter               : 0,
                qcFailedReads                  : 0,
                duplicates                     : 0,
                totalMappedReadCounter         : 0,
                pairedInSequencing             : 0,
                pairedRead1                    : 0,
                pairedRead2                    : 0,
                properlyPaired                 : 0,
                withItselfAndMateMapped        : 0,
                withMateMappedToDifferentChr   : 0,
                withMateMappedToDifferentChrMaq: 0,
                singletons                     : 0,
                insertSizeMedian               : 0,
                insertSizeSD                   : 0,
                referenceLength                : 1,
        ].asImmutable()
    }

    abstract Map getQaValuesProperties()

    abstract Class getQaClass()

    AbstractQualityAssessment createQa(Map properties = [:]) {
        return createQa(createBamFile(), properties)
    }

    AbstractQualityAssessment createQa(AbstractBamFile abstractBamFile, Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(qaClass, defaultValuesForAbstractQualityAssessment + [
                qualityAssessmentMergedPass: DomainFactory.createQualityAssessmentMergedPass(
                        abstractBamFile: abstractBamFile
                ),
        ] + qaValuesProperties, properties, saveAndValidate)
    }
}
