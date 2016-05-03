package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class Project implements Commentable, Entity {

    /**
     * This enum defines if SNV calling should be done for this project.
     * Default is unknown
     */
    enum Snv {
        YES,
        NO,
        UNKNOWN
    }

    Snv snv = Snv.UNKNOWN

    String name
    String dirName
    String realmName

    short processingPriority = ProcessingPriority.NORMAL_PRIORITY
    String alignmentDeciderBeanName

    /**
     * The name which is used in the {@link MetaDataColumn#PROJECT} column of metadata files.
     */
    String nameInMetadataFiles

    /**
     * this flag defines if the fastq files of this project have to be copied (instead of linked) regardless of whether
     * they will be processed or not
     */
    boolean hasToBeCopied = false

    Comment comment

    static belongsTo = [
            projectGroup: ProjectGroup
    ]

    static hasMany =  [
            contactPersons: ContactPerson
    ]

    static constraints = {
        name(blank: false, unique: true, validator: {val, obj ->
            Project project = atMostOneElement(Project.findAllByNameInMetadataFiles(val))
            if (project && project.id != obj.id) {
                return 'this name is already used in another project as nameInMetadataFiles entry'
            }
        })
        dirName(blank: false, unique: true, validator: { String val ->
            OtpPath.isValidRelativePath(val) &&
                    ['icgc', 'dkfzlsdf', 'lsdf', 'project'].every { !val.startsWith("${it}/") }
        })
        realmName(blank: false)
        projectGroup(nullable: true)
        processingPriority max: ProcessingPriority.MAXIMUM_PRIORITY
        alignmentDeciderBeanName(blank: false)  // If no alignment is desired, set to noAlignmentDecider instead of leaving blank
        nameInMetadataFiles(nullable: true, blank: false,  validator: {val, obj ->
            if (val) {
                Project projectByMetadata = atMostOneElement(Project.findAllByNameInMetadataFiles(val))
                Project projectByName = atMostOneElement(Project.findAllByName(val))
                if (projectByMetadata && projectByMetadata.id != obj.id) {
                    return 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry'
                }
                if (projectByName && projectByName.id != obj.id) {
                    return 'this nameInMetadataFiles is already used in another project as name entry'
                }
            }
        })
        comment(nullable: true)
    }

    String toString() {
        name
    }

    static mapping = {
        projectGroup index: "project_project_group_idx"
        processingPriority index: "project_processing_priority_idx"
    }
}
