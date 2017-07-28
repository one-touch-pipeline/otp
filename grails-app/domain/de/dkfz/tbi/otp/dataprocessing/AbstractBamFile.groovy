package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractBamFile implements Entity {

    /**
     * This ENUM declares the different states a {@link AbstractBamFile} can have while it is assigned to a {@link MergingSet}
     */
    enum State {
        /**
         * default value -> state of the {@link AbstractBamFile} when it is created (declared)
         * no processing has been started on the bam file
         */
        DECLARED,
        /**
         * {@link AbstractBamFile} should be assigned to a {@link MergingSet}
         */
        NEEDS_PROCESSING,
        /**
         * {@link AbstractBamFile} is currently in progress to be assigned to a {@link MergingSet}
         */
        INPROGRESS,
        /**
         * {@link AbstractBamFile} was assigned to a {@link MergingSet}
         */
        PROCESSED
    }

    enum BamType {
        SORTED,
        MDUP,
        RMDUP
    }

    enum QaProcessingStatus {
        UNKNOWN,
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED
    }

    BamType type = null
    boolean hasIndexFile = false
    boolean hasCoveragePlot = false
    boolean hasInsertSizePlot = false
    boolean hasMetricsFile = false
    boolean withdrawn = false

    /**
     * Coverage without N of the BamFile
     */
    // Has to be from Type Double so that it can be nullable
    Double coverage

    /**
     * Coverage with N of the BAM file.
     * In case of Exome sequencing this value stays 'null' since there is no differentiation between 'with N' and 'without N'.
     */
    Double coverageWithN

    /** Time stamp of deletion */
    Date deletionDate

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN

    /**
     * Whether this has been assigned to a merging set.
     */
    State status = State.DECLARED


    public abstract AbstractMergingWorkPackage getMergingWorkPackage()
    public abstract Set<SeqTrack> getContainedSeqTracks()
    public abstract AbstractQualityAssessment getOverallQualityAssessment()

    static constraints = {
        // Type is not nullable for BamFiles except RoddyBamFile,
        // Grails does not create the SQL schema correctly when using simple nullable constraints,
        // therefore this workaround with validator constraints is used
        type nullable: true, validator: { it != null }
        hasMetricsFile validator: { val, obj ->
            if (obj.type == BamType.SORTED) {
                return !val
            } else {
                return true
            }
        }
        status validator: { val, obj ->
            if (val == State.NEEDS_PROCESSING) {
                if (obj.withdrawn == true || obj.type == BamType.RMDUP) {
                    return false
                }
            }
            return true
        }
        deletionDate(nullable: true)
        coverage(nullable: true)
        coverageWithN(nullable: true)
    }



    static mapping = {
        'class' index: "abstract_bam_file_class_idx"
        withdrawn index: "abstract_bam_file_withdrawn_idx"
        qualityAssessmentStatus index: "abstract_bam_file_quality_assessment_status_idx"
    }



    boolean isQualityAssessed() {
        qualityAssessmentStatus == QaProcessingStatus.FINISHED
    }

    public BedFile getBedFile() {
        assert seqType.name == SeqTypeNames.EXOME.seqTypeName : "A BedFile is only available when the sequencing type is exome."
        List<SeqTrack> seqTracks = containedSeqTracks as List

        assert seqTracks.size() > 0
        return exactlyOneElement(BedFile.findAllWhere(
                referenceGenome: referenceGenome,
                libraryPreparationKit: exactlyOneElement(seqTracks*.libraryPreparationKit.unique(), "Wrong libraryPreparationKit count"),
        ), "Wrong BedFile count")
    }

    Project getProject() {
        return individual.project
    }

    short getProcessingPriority() {
        return project.processingPriority
    }

    Individual getIndividual() {
        return sample.individual
    }

    Sample getSample() {
        return mergingWorkPackage.sample
    }

    SampleType getSampleType() {
        return  sample.sampleType
    }

    SeqType getSeqType() {
        return mergingWorkPackage.seqType
    }

    Pipeline getPipeline() {
        return mergingWorkPackage.pipeline
    }

    /**
     * @return The reference genome which was used to produce this BAM file.
     */
    ReferenceGenome getReferenceGenome() {
        return mergingWorkPackage.referenceGenome
    }

    /**
     * The maximum value of {@link DataFile#dateCreated} of all {@link DataFile}s that have been merged into one of
     * the specified BAM files, or <code>null</code> if no such {@link DataFile} exists.
     */
    static Date getLatestSequenceDataFileCreationDate(final AbstractBamFile... bamFiles) {
        if (!bamFiles) {
            throw new IllegalArgumentException('No BAM files specified.')
        }
        if (bamFiles.contains(null)) {
            throw new IllegalArgumentException('At least one of the specified BAM files is null.')
        }
        return DataFile.createCriteria().get {
            seqTrack {
                'in'('id', bamFiles.sum{it.containedSeqTracks}*.id)
            }
            fileType {
                eq('type', FileType.Type.SEQUENCE)
            }
            projections {
                max('dateCreated')
            }
        }
    }


    private withdrawDownstreamBamFiles() {
        List<MergingSetAssignment> assignments = MergingSetAssignment.findAllByBamFile(this)*.mergingSet*.id

        if (assignments) {
            ProcessedMergedBamFile.createCriteria().list {
                mergingPass {
                    mergingSet {
                        'in'('id', assignments)
                    }
                }
            }.each {
                it.withdraw()
            }
        }
    }
}
