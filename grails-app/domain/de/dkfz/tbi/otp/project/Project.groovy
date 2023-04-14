/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.project

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldValue
import de.dkfz.tbi.otp.project.dta.DataTransferAgreement
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersionSelector

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
class Project implements CommentableWithProject, ProjectPropertiesGivenWithRequest, Entity {

    enum ProjectType {
        SEQUENCING,
        USER_MANAGEMENT,
    }

    /** This attribute is used externally. Please discuss a change in the team */
    String individualPrefix
    String internalNotes
    String dirName
    Realm realm
    String dirAnalysis

    /**
     * Historic flag to signal whether the individualPrefix field has to be unqiue or not.
     *
     * Currently we still have projects sharing an individual prefix, so need this flag as a workaround
     * but new projects should not rely on this and provide a unique individualPrefix.
     */
    boolean uniqueIndividualPrefix = true

    ProcessingPriority processingPriority

    /**
     * The name which is used in the {@link MetaDataColumn#PROJECT} column of metadata files.
     */
    String nameInMetadataFiles

    boolean fingerPrinting = true

    /**
     * flag to send  processing notification to end users.
     *
     * The mail is send nevertheless to the ticketing system.
     */
    boolean processingNotification = true

    /**
     * flag to send qcTrafficLight notification to end users.
     *
     * The mail is send nevertheless to the ticketing system.
     */
    boolean qcTrafficLightNotification = true

    /**
     * flag to send final notification.
     */
    boolean customFinalNotification = false

    boolean closed = false

    Set<ProjectInfo> projectInfos

    Set<DataTransferAgreement> dataTransferAgreements

    ProjectGroup projectGroup

    String unixGroup

    boolean publiclyAvailable

    boolean projectRequestAvailable = false

    /**
     * flag for archived project.
     *
     * Data in archived project cannot be accessed.
     */
    boolean archived = false

    /**
     * @deprecated field is part of the old workflow system, use {@link WorkflowVersionSelector} instead
     */
    @Deprecated
    AlignmentDeciderBeanName alignmentDeciderBeanName = AlignmentDeciderBeanName.NO_ALIGNMENT

    /** This attribute is used externally. Please discuss a change in the team */
    SampleIdentifierParserBeanName sampleIdentifierParserBeanName = SampleIdentifierParserBeanName.NO_PARSER

    QcThresholdHandling qcThresholdHandling

    static hasMany = [
            speciesWithStrains    : SpeciesWithStrain,
            projectInfos          : ProjectInfo,
            dataTransferAgreements: DataTransferAgreement,
            keywords              : Keyword,
            projectFields         : AbstractFieldValue,
    ]

    static belongsTo = [
            projectGroup: ProjectGroup,
            realm       : Realm,
    ]

    static mappedBy = [
            projectInfos: "project",
            dataTransferAgreements: "project",
    ]

    static constraints = {
        name(blank: false, unique: true, validator: { val, obj ->
            Project project = atMostOneElement(Project.findAllByNameInMetadataFiles(val))
            if (project && project.id != obj.id) {
                return 'duplicate.metadataFilesName'
            }
        })

        individualPrefix(nullable: true, blank: false, validator: { val, obj ->
            if (obj.uniqueIndividualPrefix) {
                List<Project> list = Project.findAllByIndividualPrefixAndIdNotEqual(val, obj.id)
                if (list && ((list.size() == 1 && !list*.id.contains(obj.id)) || (list.size() > 1))) { // true if list contains other projects than itself
                    return 'duplicate'
                }
                if (!val) {
                    return 'default.blank.message'
                }
            }
        })

        dirName(blank: false, unique: true, shared: "relativePath")

        dirAnalysis(nullable: true, shared: "absolutePath")

        realm(nullable: false)

        projectGroup(nullable: true)

        nameInMetadataFiles(nullable: true, blank: false, validator: { val, obj ->
            if (val) {
                Project projectByMetadata = atMostOneElement(Project.findAllByNameInMetadataFiles(val))
                Project projectByName = atMostOneElement(Project.findAllByName(val))
                if (projectByMetadata && projectByMetadata.id != obj.id) {
                    return 'duplicate'
                }
                if (projectByName && projectByName.id != obj.id) {
                    return 'duplicate.name'
                }
            }
        })

        comment nullable: true
        description nullable: true
        tumorEntity nullable: true
        endDate nullable: true
        relatedProjects nullable: true
        internalNotes nullable: true
        storageUntil nullable: true
        archived nullable: false
    }

    @Override
    String toString() {
        return name
    }

    static Closure mapping = {
        projectGroup index: "project_project_group_idx"
        processingPriority index: "project_processing_priority_idx"
        name index: "project_name_idx"
        nameInMetadataFiles index: "project_name_in_metadata_files_idx"
        realm index: "project_realm_idx"
        unixGroup index: "project_unix_group_idx"
        dirAnalysis type: "text"
        description type: "text"
        internalNotes type: "text"
        comment cascade: "all-delete-orphan"
        projectFields cascade: "all-delete-orphan"
    }

    String getDisplayName() {
        return "${name}${closed ? " (closed)" : ""}${archived ? " (archived)" : ""}"
    }

    /**
     * @deprecated use {@link ProjectService#getProjectDirectory(Project)}
     */
    @Deprecated
    File getProjectDirectory() {
        return new OtpPath(this, dirName).absoluteDataManagementPath
    }

    @Override
    Project getProject() {
        return this
    }

    String listAllKeywords() {
        return keywords*.name.sort().join(",")
    }
}
