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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.administration.ProjectInfo
import de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.ProjectPropertiesGivenWithRequest
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/** This table is used externally. Please discuss a change in the team */
class Project implements ProjectPropertiesGivenWithRequest, CommentableWithProject, Entity {

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

    boolean uniqueIndividualPrefix = true

    /**
     * this flag defines if the fastq files of this project have to be copied (instead of linked) regardless of whether
     * they will be processed or not
     */
    boolean forceCopyFiles = false

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

    ProjectGroup projectGroup

    String unixGroup

    boolean publiclyAvailable

    AlignmentDeciderBeanName alignmentDeciderBeanName = AlignmentDeciderBeanName.NO_ALIGNMENT

    /** This attribute is used externally. Please discuss a change in the team */
    SampleIdentifierParserBeanName sampleIdentifierParserBeanName = SampleIdentifierParserBeanName.NO_PARSER

    QcThresholdHandling qcThresholdHandling

    static hasMany = [
            projectInfos: ProjectInfo,
            keywords    : Keyword,
    ]

    static belongsTo = [
            projectGroup: ProjectGroup,
            realm       : Realm,
    ]

    static mappedBy = [
            projectInfos: "project",
    ]

    static constraints = {
        name(blank: false, unique: true, validator: { val, obj ->
            Project project = atMostOneElement(Project.findAllByNameInMetadataFiles(val))
            if (project && project.id != obj.id) {
                return 'duplicate.metadataFilesName'
            }
        })

        individualPrefix(nullable: true, blank: false, validator: { val, obj ->
            Project project = Project.findByIndividualPrefixAndIdNotEqual(val, obj.id)
            if (obj.uniqueIndividualPrefix && project && project.id != obj.id) {
                return 'duplicate'
            } else if (obj.uniqueIndividualPrefix && !val) {
                return 'default.blank.message'
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
        costCenter nullable: true
        tumorEntity nullable: true
        speciesWithStrain nullable: true
        endDate nullable: true
        organizationalUnit nullable: true
        relatedProjects nullable: true
        internalNotes nullable: true
        fundingBody nullable: true
        grantId nullable: true
        storageUntil nullable: true
    }

    @Override
    String toString() {
        return name
    }

    static mapping = {
        projectGroup index: "project_project_group_idx"
        processingPriority index: "project_processing_priority_idx"
        name index: "project_name_idx"
        dirAnalysis type: "text"
        description type: "text"
        internalNotes type: "text"
    }

    String getDisplayName() {
        "${name}${closed ? " (closed)" : ""}"
    }

    File getProjectDirectory() {
        new OtpPath(this, dirName).absoluteDataManagementPath
    }

    File getProjectSequencingDirectory() {
        new File(projectDirectory, 'sequencing')
    }

    @Override
    Project getProject() {
        return this
    }

    static Project getByNameOrNameInMetadataFiles(String name) {
        if (name) {
            return findByNameOrNameInMetadataFiles(name, name)
        }
        return null
    }

    String listAllKeywords() {
        return keywords*.name.sort().join(",")
    }
}
