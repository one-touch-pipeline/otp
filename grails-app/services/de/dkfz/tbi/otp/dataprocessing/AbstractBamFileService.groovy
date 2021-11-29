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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.*

@Transactional
class AbstractBamFileService {

    Double calculateCoverageWithN(AbstractBamFile bamFile) {
        assert bamFile : 'Parameter bamFile must not be null'

        if (bamFile.seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName || bamFile.seqType.isWgbs() || bamFile.seqType.isChipSeq()) {
            calculateCoverage(bamFile, 'length')
        } else if (bamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            //In case of Exome sequencing this value stays 'null' since there is no differentiation between 'with N' and 'without N'
            return null
        } else {
            throw new NotSupportedException("The 'with N' coverage calculation for seq Type ${bamFile.seqType.name} is not possible yet.")
        }
    }

    private Double calculateCoverage(AbstractBamFile bamFile, String property) {
        assert bamFile : 'Parameter bamFile must not be null'

        Long length
        Long basesMapped

        if (bamFile.seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName || bamFile.seqType.isWgbs() || bamFile.seqType.isChipSeq()) {
            ReferenceGenome referenceGenome = bamFile.referenceGenome
            assert referenceGenome : "Unable to find a reference genome for the BAM file ${bamFile}"

            length = referenceGenome."${property}"
            assert length > 0 : "The property '${property}' of the reference genome '${referenceGenome}' is 0 or negative."

            basesMapped = bamFile.overallQualityAssessment.qcBasesMapped
        } else if (bamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            BedFile bedFile = bamFile.bedFile
            assert bedFile : "Unable to find a bed file for the BAM file ${bamFile}"

            length = bedFile.mergedTargetSize
            assert length > 0 : "The length of the targets in the bed file ${bedFile} is 0 or negative."

            /*
             * In the beginning of the exome alignments we calculated the QA the same way as for whole genome.
             * Therefore for old data we do not have the field onTargetMappedBases filled in.
             * To prevent displaying wrong values nothing is shown in the GUI (null is returned).
             */
            if (bamFile.overallQualityAssessment.onTargetMappedBases) {
                basesMapped = bamFile.overallQualityAssessment.onTargetMappedBases
            } else {
                return null
            }
        } else {
            throw new NotSupportedException("The coverage calculation for seq Type ${bamFile.seqType.name} is not possible yet.")
        }
        return basesMapped / length
    }

    /**
     * @Deprecated methods accessing the database shouldn't be static, since then the transaction proxies does not work.
     */
    @Deprecated
    static AbstractBamFile saveBamFile(AbstractBamFile bamFile) {
        return bamFile.save(flush: true)
    }
}
