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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

import static org.springframework.util.Assert.notNull

@Transactional
class AbstractBamFileService {

    private static final String QUALITY_ASSESSED_AND_MERGED_QUERY_PART =
        "qualityAssessmentStatus = :qaStatus AND status = :status AND EXISTS (" +
            "FROM MergingSetAssignment msa " +
            "WHERE msa.mergingSet.status = :mergingSetStatus " +
            "AND msa.bamFile = bf1 AND EXISTS (" +
                "FROM ProcessedMergedBamFile bf3 " +
                // Make sure that the PMBF (bf3) which this BAM file (bf1) has been merged into is old enough.
                "WHERE bf3.dateCreated < :createdBefore " +
                // If this BAM file (bf1) is withdrawn, don't care whether bf3 is withdrawn. If bf1 is *not* withdrawn,
                // require bf3 not to be withdrawn.
                "AND (bf1.withdrawn = true OR bf3.withdrawn = false) " +
                "AND bf3.mergingPass.mergingSet = msa.mergingSet "

    /**
     * Same criteria as in {@link #hasBeenQualityAssessedAndMerged(AbstractBamFile, Date)}.
     */
    static final String QUALITY_ASSESSED_AND_MERGED_QUERY =
            QUALITY_ASSESSED_AND_MERGED_QUERY_PART +
                // Make sure that the PMBF (bf3) which this BAM file (bf1) has been merged into has already been
                // quality assessed.
                "AND bf3.qualityAssessmentStatus = :qaStatus)) "


    static final String QUALITY_ASSESSED_AND_MERGED_QUERY_WITHOUT_QA_CHECK =
            QUALITY_ASSESSED_AND_MERGED_QUERY_PART + ")) "


    /**
     * @param bamFile, which was assigned to a {@link MergingSet}
     */
    void assignedToMergingSet(AbstractBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method assignedToMergingSet is null")
        bamFile.status = State.PROCESSED
        bamFile.save()
    }

    /**
     * @return bam files connected directly with this processedMergedBamFile.mergingSet
     */
    List<AbstractBamFile> findByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile is not allowed to be null")
        return processedMergedBamFile.mergingPass.mergingSet.bamFiles
    }

    /**
     * returns a list of all single lane bam files, which are merged in several step to the final processedMergedBamFile.
     * It is assumed that only new lanes are merged with the old mergedBamFile to a new one.
     */
    List<ProcessedBamFile> findAllByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile is not allowed to be null")
        List<AbstractBamFile> results = [processedMergedBamFile]
        while (results.find { it instanceof ProcessedMergedBamFile }) {
            ProcessedMergedBamFile tempFile = results.find { it instanceof ProcessedMergedBamFile }
            results = results - tempFile
            results.addAll(findByProcessedMergedBamFile(tempFile))
        }
        return results
    }

    /**
     * Same criteria as in {@link #QUALITY_ASSESSED_AND_MERGED_QUERY}.
     */
    boolean hasBeenQualityAssessedAndMerged(final AbstractBamFile bamFile, final Date before) {
        notNull bamFile
        if (bamFile.qualityAssessmentStatus != AbstractBamFile.QaProcessingStatus.FINISHED ||
                bamFile.status != AbstractBamFile.State.PROCESSED) {
            return false
        }
        final Collection<MergingSet> processedMergingSets =
                MergingSetAssignment.findAllByBamFile(bamFile)*.mergingSet.findAll { it.status == MergingSet.State.PROCESSED }
        if (processedMergingSets.empty) {
            return false
        }
        return ProcessedMergedBamFile.createCriteria().get {
            eq("qualityAssessmentStatus", AbstractBamFile.QaProcessingStatus.FINISHED)
            lt("dateCreated", before)
            if (!bamFile.withdrawn) {
                eq("withdrawn", false)
            }
            mergingPass {
                'in'("mergingSet", processedMergingSets)
            }
            maxResults(1)
        } != null
    }

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
        return bamFile.save()
    }
}
