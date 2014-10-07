package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class AbstractBamFileService {

    /**
     * Same criteria as in {@link #hasBeenQualityAssessedAndMerged(AbstractBamFile, Date)}.
     */
    public static final String QUALITY_ASSESSED_AND_MERGED_QUERY =
        "qualityAssessmentStatus = :qaStatus AND status = :status AND EXISTS (" +
            "FROM MergingSetAssignment msa " +
            "WHERE msa.mergingSet.status = :mergingSetStatus " +
            "AND msa.bamFile = bf1 AND EXISTS (" +
                "FROM ProcessedMergedBamFile bf3 " +
                // Make sure that the PMBF (bf3) which this BAM file (bf1) has been merged into has already been
                // quality assessed and is old enough.
                "WHERE bf3.qualityAssessmentStatus = :qaStatus " +
                "AND bf3.dateCreated < :createdBefore " +
                // If this BAM file (bf1) is withdrawn, don't care whether bf3 is withdrawn. If bf1 is *not* withdrawn,
                // require bf3 not to be withdrawn.
                "AND (bf1.withdrawn = true OR bf3.withdrawn = false) " +
                "AND bf3.mergingPass.mergingSet = msa.mergingSet))"

    /**
     * @param bamFile, which was assigned to a {@link MergingSet}
     */
    void assignedToMergingSet(AbstractBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method assignedToMergingSet is null")
        bamFile.status = State.PROCESSED
        assertSave(bamFile)
    }

    public List<AbstractBamFile> findByMergingSet(MergingSet mergingSet) {
        notNull(mergingSet, "The parameter merging set is not allowed to be null")
        return MergingSetAssignment.findAllByMergingSet(mergingSet)*.bamFile
    }

    public List<AbstractBamFile> findByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile is not allowed to be null")
        return findByMergingSet(processedMergedBamFile.mergingPass.mergingSet)
    }

    /**
     * returns a list of all single lane bam files, which are merged in several step to the final processedMergedBamFile.
     * It is assumed that only new lanes are merged with the old mergedBamFile to a new one.
     */
    public List<ProcessedBamFile> findAllByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
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
    public boolean hasBeenQualityAssessedAndMerged(final AbstractBamFile bamFile, final Date before) {
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

    Double calculateCoverageWithoutN(AbstractBamFile bamFile) {
        assert bamFile : 'Parameter bamFile must not be null'

        if ([SeqTypeNames.WHOLE_GENOME.seqTypeName, SeqTypeNames.EXOME.seqTypeName].contains(bamFile.seqType.name)) {
            calculateCoverage(bamFile, 'lengthWithoutN')
        } else {
            throw new RuntimeException("The 'without N' coverage calculation for seq Type ${bamFile.seqType.name} is not possible yet.")
        }
    }


    Double calculateCoverageWithN(AbstractBamFile bamFile) {
        assert bamFile : 'Parameter bamFile must not be null'

        if (bamFile.seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName) {
            calculateCoverage(bamFile, 'length')
        } else if (bamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            //In case of Exome sequencing this value stays 'null' since there is no differentiation between 'with N' and 'without N'
            return null
        } else {
            throw new RuntimeException("The 'with N' coverage calculation for seq Type ${bamFile.seqType.name} is not possible yet.")
        }
    }


    private Double calculateCoverage(AbstractBamFile bamFile, String property) {
        assert bamFile : 'Parameter bamFile must not be null'
        assert bamFile.isQualityAssessed() : "Cannot calculate coverage! The BAM file ${bamFile} has not passed the QC yet"

        Long length
        Long basesMapped

        if (bamFile.seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName) {
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

            basesMapped = bamFile.overallQualityAssessment.onTargetMappedBases
        } else {
            throw new RuntimeException("The coverage calculation for seq Type ${bamFile.seqType.name} is not possible yet.")
        }
        return basesMapped / length
    }


    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
