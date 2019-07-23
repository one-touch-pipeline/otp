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

package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.searchability.Keyword
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
        UNKNOWN,
    }

    enum ProjectType {
        SEQUENCING,
        DATA_MANAGEMENT,
    }

    SpeciesWithStrain speciesWithStrain

    Snv snv = Snv.UNKNOWN

    String name
    String projectPrefix
    String subsequentApplication
    String connectedProjects
    String internalNotes
    String organisationUnit
    String dirName
    Realm realm
    String dirAnalysis

    short processingPriority = ProcessingPriority.NORMAL.priority

    /**
     * The name which is used in the {@link MetaDataColumn#PROJECT} column of metadata files.
     */
    String nameInMetadataFiles

    /**
     * this flag defines if the fastq files of this project have to be copied (instead of linked) regardless of whether
     * they will be processed or not
     */
    boolean forceCopyFiles = false

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

    Comment comment

    String description

    ProjectGroup projectGroup

    String unixGroup

    String costCenter

    TumorEntity tumorEntity

    AlignmentDeciderBeanName alignmentDeciderBeanName = AlignmentDeciderBeanName.NO_ALIGNMENT

    SampleIdentifierParserBeanName sampleIdentifierParserBeanName = SampleIdentifierParserBeanName.NO_PARSER

    QcThresholdHandling qcThresholdHandling

    Date endDate

    Date storageUntil

    ProjectType projectType

    static hasMany = [
            projectInfos: ProjectInfo,
            keywords: Keyword
    ]

    static belongsTo = [
            projectGroup: ProjectGroup,
            realm: Realm,
    ]

    static mappedBy = [
            projectInfos: "project",
    ]

    static constraints = {
        name(blank: false, unique: true, validator: { val, obj ->
            Project project = atMostOneElement(Project.findAllByNameInMetadataFiles(val))
            if (project && project.id != obj.id) {
                return 'this name is already used in another project as nameInMetadataFiles entry'
            }
        })

        projectPrefix(unique: true, blank: false)

        dirName(blank: false, unique: true, validator: { String val ->
            OtpPath.isValidRelativePath(val)
        })

        dirAnalysis(nullable: true, validator: { String val ->
            !val || OtpPath.isValidAbsolutePath(val)
        })

        realm(nullable: false)

        projectGroup(nullable: true)

        processingPriority max: ProcessingPriority.MAXIMUM.priority

        nameInMetadataFiles(nullable: true, blank: false,  validator: { val, obj ->
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

        description(nullable: true)

        costCenter(nullable: true)

        tumorEntity(nullable: true)

        speciesWithStrain(nullable : true)

        endDate(nullable: true)

        organisationUnit(nullable: true)

        connectedProjects(nullable: true)

        subsequentApplication(nullable: true)

        internalNotes(nullable: true)
    }

    @Override
    String toString() {
        name
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
        new File(getProjectDirectory(), 'sequencing')
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
