package de.dkfz.tbi.otp.ngsdata

/**
 * Represents connection between {@link Project}, {@link SeqType}
 * and {@link ReferenceGenome}
 *
 *
 */
class ReferenceGenomeProjectSeqType {

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


    static belongsTo = [
        project: Project,
        seqType: SeqType,
        referenceGenome: ReferenceGenome
    ]

    static constraints = {
        // there must be no 2 current (not deprecated) reference genomes
        // defined for the same combination of project and seqType
        referenceGenome validator: { val, obj ->
            if (!obj.deprecatedDate) {
                List<ReferenceGenomeProjectSeqType> existingObjects = ReferenceGenomeProjectSeqType
                                .findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(obj.project, obj.seqType)
                if (existingObjects.isEmpty()) {
                    return true
                } else if ((existingObjects.size() == 1) && (existingObjects.contains(obj))) {
                    return true
                } else {
                    return false
                }
            }
        }
        deprecatedDate(nullable: true)
    }

    String toString() {
        return "deprecatedDate: ${deprecatedDate}, project: ${project}, seqType: ${seqType}, referenceGenome: ${referenceGenome}"
    }
}
