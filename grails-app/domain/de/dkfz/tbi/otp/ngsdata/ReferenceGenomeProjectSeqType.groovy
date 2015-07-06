package de.dkfz.tbi.otp.ngsdata

/**
 * Represents connection between {@link Project}, {@link SeqType}
 * and {@link ReferenceGenome}
 *
 *
 */
class ReferenceGenomeProjectSeqType {

    static final String TAB_FILE_PATTERN = /[0-9a-zA-Z-_\.]+\.tab/

    /**
     * Date when the object has been created.
     * To be filled in automatically by GORM.
     */
    Date dateCreated
    /**
     * Is used to track information about
     * reference genomes used in a project for a seqType.
     * If it is null, this reference genome is used for these
     * project and seqType.
     * If it is not null, it is date when usage
     * of this reference genome for these project and seqType
     * has been deprecated.
     */
    Date deprecatedDate = null

    /**
     * File name of file holding the chromosome stat size.
     * The file ends with '.tab' and is located in the stat subdirectory of the reference genome.
     */
    String statSizeFileName


    static belongsTo = [
        project: Project,
        seqType: SeqType,
        sampleType: SampleType,
        referenceGenome: ReferenceGenome
    ]

    static constraints = {
        // there must be no 2 current (not deprecated) reference genomes
        // defined for the same combination of project and seqType and sampleType
        referenceGenome validator: { val, obj ->
            if (!obj.deprecatedDate) {
                List<ReferenceGenomeProjectSeqType> existingObjects = ReferenceGenomeProjectSeqType
                            .findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(obj.project, obj.seqType, obj.sampleType)
                if (existingObjects.isEmpty()) {
                    return true
                } else if ((existingObjects.size() == 1) && (existingObjects.contains(obj))) {
                    return true
                } else {
                    return false
                }
            }
        }
        sampleType(nullable: true)
        deprecatedDate(nullable: true)
        statSizeFileName nullable: true, blank: false, matches: TAB_FILE_PATTERN
    }

    String toString() {
        return "deprecatedDate: ${deprecatedDate}, project: ${project}, seqType: ${seqType}, sampleType: ${sampleType}, referenceGenome: ${referenceGenome}"
    }

    static mapping = {
        project index: "reference_genome_project_seq_type_project_idx"
        seqType index: "reference_genome_project_seq_type_seq_type_idx"
        sampleType index: "reference_genome_project_seq_type_sample_type_idx"
        referenceGenome index: "reference_genome_project_seq_type_reference_genome_idx"
    }
}
