package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import org.springframework.validation.*

import java.sql.*
import java.text.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectConfigController {

    ProjectService projectService
    ProjectOverviewService projectOverviewService
    SeqTrackService seqTrackService
    ContactPersonService contactPersonService
    SampleTypePerProjectService sampleTypePerProjectService
    ProcessingThresholdsService processingThresholdsService
    CommentService commentService
    ProjectSelectionService projectSelectionService

    Map index() {
        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        project = exactlyOneElement(Project.findAllByName(project.name, [fetch: [projectCategories: 'join', projectGroup: 'join']]))

        Map<String, String> dates = getDates(project)

        List<ProjectContactPerson> projectContactPersons = ProjectContactPerson.findAllByProject(project)
        List<String> contactPersonRoles = [''] + ContactPersonRole.findAll()*.name

        List<List> thresholdsTable = createThresholdTable(project)

        List snvConfigTable = createAnalysisConfigTable(project,  SeqType.getSnvPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_SNV))
        List indelConfigTable = createAnalysisConfigTable(project, SeqType.getIndelPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_INDEL))
        List sophiaConfigTable = createAnalysisConfigTable(project, SeqType.getSophiaPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_SOPHIA))
        List aceseqConfigTable = createAnalysisConfigTable(project, SeqType.getAceseqPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_ACESEQ))

        String checkSophiaReferenceGenome = projectService.checkReferenceGenomeForSophia(project).getError()
        String checkAceseqReferenceGenome = projectService.checkReferenceGenomeForAceseq(project).getError()

        Realm realm = ConfigService.getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                project.dirName,
        )

        List accessPersons = projectOverviewService.getAccessPersons(project)

        return [
                projects: projects,
                project: project,
                creationDate: dates.creationDate,
                lastReceivedDate: dates.lastReceivedDate,
                comment: project.comment,
                nameInMetadata: project.nameInMetadataFiles?: '',
                seqTypes: SeqType.roddyAlignableSeqTypes,
                snvSeqTypes: SeqType.snvPipelineSeqTypes,
                indelSeqTypes: SeqType.indelPipelineSeqTypes,
                sophiaSeqType: SeqType.sophiaPipelineSeqTypes,
                aceseqSeqType: SeqType.aceseqPipelineSeqTypes,
                snv: project.snv,
                projectContactPersons: projectContactPersons,
                roleDropDown: contactPersonRoles,
                thresholdsTable: thresholdsTable,
                snvConfigTable: snvConfigTable,
                indelConfigTable: indelConfigTable,
                sophiaConfigTable: sophiaConfigTable,
                aceseqConfigTable: aceseqConfigTable,
                snvDropDown: Project.Snv.values(),
                directory: projectDirectory,
                analysisDirectory: project.dirAnalysis?: '',
                projectGroup: project.projectGroup,
                copyFiles: project.hasToBeCopied,
                fingerPrinting: project.fingerPrinting,
                mailingListName: project.mailingListName,
                description: project.description,
                projectCategories: ProjectCategory.listOrderByName(),
                accessPersons: accessPersons,
                unixGroup: project.unixGroup,
                costCenter: project.costCenter,
                processingPriorities: ProjectService.processingPriorities,
                checkSophiaReferenceGenome: checkSophiaReferenceGenome,
                checkAceseqReferenceGenome: checkAceseqReferenceGenome,
        ]
    }

    JSON getAlignmentInfo() {
        Project project = exactlyOneElement(Project.findAllByName(params.project, [fetch: [projectCategories: 'join', projectGroup: 'join']]))
        Map<String, ProjectOverviewService.AlignmentInfo> alignmentInfo = null
        String alignmentError = null
        try {
            alignmentInfo = projectOverviewService.getAlignmentInformation(project)
        } catch (Throwable e) {
            alignmentError = e.message
            log.error(e.message, e)
        }

        Map map = [alignmentInfo: alignmentInfo, alignmentError: alignmentError]
        render map as JSON
    }

    JSON createContactPersonOrAddProject(UpdateContactPersonCommand cmd){
        if (cmd.hasErrors()) {
            Map data = getErrorData(cmd.errors.getFieldError())
            render data as JSON
            return
        }
        if (ContactPerson.findByFullName(cmd.name)) {
            checkErrorAndCallMethod(cmd, { contactPersonService.addContactPersonToProject(cmd.name, cmd.project, cmd.contactPersonRole) })
        } else {
            checkErrorAndCallMethod(cmd, { contactPersonService.createContactPerson(cmd.name, cmd.email, cmd.aspera, cmd.project, cmd.contactPersonRole) })
        }
    }

    static ContactPersonRole getContactPersonRoleByName(String roleName) {
        return roleName.isEmpty() ? null : exactlyOneElement(ContactPersonRole.findAllByName(roleName))
    }

    JSON deleteContactPersonOrRemoveProject(UpdateDeleteContactPersonCommand cmd){
        checkErrorAndCallMethod(cmd, { contactPersonService.removeContactPersonFromProject(cmd.projectContactPerson) })
    }

    JSON updateName(UpdateContactPersonNameCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateName(cmd.contactPerson, cmd.newName) })
    }

    JSON updateEmail(UpdateContactPersonEmailCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateEmail(cmd.contactPerson, cmd.newEmail) })
    }

    JSON updateAspera(UpdateContactPersonAsperaCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateAspera(cmd.contactPerson, cmd.newAspera) })
    }

    JSON updateRole(UpdateContactPersonRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateRole(cmd.projectContactPerson, getContactPersonRoleByName(cmd.newRole)) })
    }

    JSON updateAnalysisDirectory(UpdateAnalysisDirectoryCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateAnalysisDirectory(cmd.analysisDirectory, cmd.project) })
    }

    JSON updateNameInMetadataFiles(UpdateNameInMetadataCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateNameInMetadata(cmd.newNameInMetadata, cmd.project) })
    }

    JSON updateCategory(UpdateCategoryCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateCategory(cmd.value, cmd.project) })
    }

    JSON updateMailingListName(UpdateMailingListNameCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateMailingListName(cmd.mailingListName, cmd.project) })
    }

    JSON updateCostCenter(UpdateCostCenterCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateCostCenterName(cmd.costCenter, cmd.project) })
    }

    JSON updateDescription(UpdateDescriptionCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateDescription(cmd.description, cmd.project) })
    }

    JSON updateProcessingPriority(UpdateProcessingPriorityCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProcessingPriority(cmd.processingPriority, cmd.project) })
    }

    JSON updateSnv(UpdateSnvCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.setSnv(cmd.project, Project.Snv.valueOf(cmd.value))})
    }

    JSON snvDropDown() {
        render Project.Snv.values() as JSON
    }

    JSON contactPersons() {
        List<String> contactPersons = contactPersonService.getAllContactPersons()*.fullName
        render contactPersons as JSON
    }

    JSON saveProjectComment(CommentCommand cmd) {
        Project project = projectService.getProject(cmd.id)
        commentService.saveComment(project, cmd.comment)
        def dataToRender = [date: project.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: project.comment.author]
        render dataToRender as JSON
    }

    JSON updateFingerPrinting(Long id, String value) {
        assert id
        Project project = Project.get(id)
        projectService.updateFingerPrinting(project, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }


    private void checkErrorAndCallMethod(Serializable cmd, Closure method) {
        Map data
        if (cmd.hasErrors()) {
            data = getErrorData(cmd.errors.getFieldError())
        } else {
            method()
            data = [success: true]
        }
        render data as JSON
    }

    private Map getErrorData(FieldError errors) {
        return [success: false, error: "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"]
    }

    Map<String, String> getDates(Project project) {
        Timestamp[] timestamps = Sequence.createCriteria().get {
            eq("projectId", project.id)
            projections {
                min("dateCreated")
                max("dateCreated")
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return [creationDate: timestamps[0] ? simpleDateFormat.format(timestamps[0]) : null, lastReceivedDate: timestamps[0] ? simpleDateFormat.format(timestamps[1]) : null]
    }

    private static List<List<String>> createAnalysisConfigTable(Project project, List<SeqType> seqTypes, Pipeline pipeline) {
        List<List<String>> table = []
        table.add(["", "Config created", "Version"])
        seqTypes.each { SeqType seqType ->
            List<String> row = []
            row.add(seqType.displayName)
            SnvConfig snvConfig
            RoddyWorkflowConfig roddyWorkflowConfig
            if (pipeline.type == Pipeline.Type.SNV && (snvConfig = atMostOneElement(SnvConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType)))) {
                row.add("Yes")
                row.add(snvConfig.externalScriptVersion)
            } else if ((roddyWorkflowConfig = atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateIsNull(project, seqType, pipeline)))) {
                row.add("Yes")
                row.add(roddyWorkflowConfig.pluginVersion)
            } else {
                row.add("No")
                row.add("-")
            }
            table.add(row)
        }
        return table.transpose()
    }

    private List<List<String>> createThresholdTable(Project project) {
        List<List<String>> thresholdsTable = []
        List<SeqType> seqTypes = SeqType.getAllAnalysableSeqTypes()

        List row = []
        row.add(message(code: "projectOverview.analysis.sampleType"))
        row.add(message(code: "projectOverview.analysis.category"))
        seqTypes.each {
            row.add(message(code: "projectOverview.analysis.minLanes", args:[it.displayName, it.libraryLayout]))
            row.add(message(code: "projectOverview.analysis.coverage", args:[it.displayName, it.libraryLayout]))
        }
        thresholdsTable.add(row)

        sampleTypePerProjectService.findByProject(project).each() { sampleTypePerProject ->
            row = []
            row.add(sampleTypePerProject.sampleType.name)
            row.add(sampleTypePerProject.category)
            seqTypes.each {
                ProcessingThresholds processingThresholds = processingThresholdsService.findByProjectAndSampleTypeAndSeqType(project, sampleTypePerProject.sampleType, it)
                row.add(processingThresholds?.numberOfLanes)
                row.add(processingThresholds?.coverage)
            }
            thresholdsTable.add(row)
        }
        if (thresholdsTable.size() == 1) {
            return []
        }
        thresholdsTable = removeEmptyColumns(thresholdsTable)
        return thresholdsTable
    }

    private static List removeEmptyColumns(List list) {
        list = list.transpose()
        list.removeAll {
            it.findAll {it == null}.size() == (it.size() - 1)
        }
        return list.transpose()
    }

    JSON dataTableSourceReferenceGenome(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.listReferenceGenome(project).collect { ReferenceGenomeProjectSeqType it->
            String adapterTrimming = it.sampleType ? "" :
                    it.seqType.isWgbs() || it.seqType.isWgbs() ?:
                            RoddyWorkflowConfig.getLatestForProject(project, it.seqType, Pipeline.findByName(Pipeline.Name.PANCAN_ALIGNMENT))?.adapterTrimmingNeeded
            [it.seqType.name, it.sampleType?.name, it.referenceGenome.name, it.statSizeFileName ?: "", adapterTrimming]
        }
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }
}

class UpdateContactPersonCommand implements Serializable {
    String name
    String email
    String aspera
    ContactPersonRole contactPersonRole
    Project project
    static constraints = {
        name(blank: false, validator: {val, obj ->
            ContactPerson contactPerson = ContactPerson.findByFullName(val)
            if (ProjectContactPerson.findByContactPersonAndProject(contactPerson, obj.project)) {
                return 'Duplicate'
            } else if (contactPerson && obj.email != "" && obj.email != contactPerson.email) {
                return 'There is already a Person with \'' + contactPerson.fullName + '\' as Name and \'' + contactPerson.email + '\' as Email in the Database'
            }})
        email(email:true, validator: {val, obj ->
            if (!ContactPerson.findByFullName(obj.name) && val == "") {
                return 'Empty'
            }})
        aspera(blank: true)
        project(nullable: false)
        contactPersonRole(nullable: true)
    }
    void setName(String name) {
        this.name = name?.trim()?.replaceAll(" +", " ")
    }
    void setEmail(String email) {
        this.email = email?.trim()?.replaceAll(" +", " ")
    }
    void setAspera(String aspera) {
        this.aspera = aspera?.trim()?.replaceAll(" +", " ")
    }
    void setRole(String role) {
        this.contactPersonRole = ProjectOverviewController.getContactPersonRoleByName(role)
    }
}

class UpdateDeleteContactPersonCommand implements Serializable {
    ProjectContactPerson projectContactPerson
}

class UpdateContactPersonNameCommand implements Serializable {
    ContactPerson contactPerson
    String newName
    static constraints = {
        newName(blank: false, validator: {val, obj ->
            if (val == obj.contactPerson?.fullName) {
                return 'No Change'
            } else if (ContactPerson.findByFullName(val)) {
                return 'Duplicate'
            }
        })
    }
    void setValue(String value) {
        this.newName = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateContactPersonEmailCommand implements Serializable {
    ContactPerson contactPerson
    String newEmail
    static constraints = {
        newEmail(email:true, blank: false ,validator: {val, obj ->
            if (val == obj.contactPerson?.email) {
                return 'No Change'
            } else if (ContactPerson.findByEmail(val)) {
                return 'Duplicate'
            }
        })
    }
    void setValue(String value) {
        this.newEmail = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateContactPersonAsperaCommand implements Serializable {
    ContactPerson contactPerson
    String newAspera
    static constraints = {
        newAspera(blank: true, validator: {val, obj ->
            if (val == obj.contactPerson?.aspera) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newAspera = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateContactPersonRoleCommand implements Serializable {
    ProjectContactPerson projectContactPerson
    String newRole
    void setValue(String value) {
        this.newRole = value
    }
}

class UpdateNameInMetadataCommand implements Serializable {
    String newNameInMetadata
    Project project
    static constraints = {
        newNameInMetadata(nullable: true, validator: { val, obj ->
            Project projectByNameInMetadata = Project.findByNameInMetadataFiles(val)
            if (val && projectByNameInMetadata && projectByNameInMetadata != obj.project) {
                return '\'' + val + '\' exists already in another project as nameInMetadataFiles entry'
            }
            if (val != obj.project.name && Project.findByName(val)) {
                return '\'' + val + '\' is used in another project as project name'
            }
        })
    }
    void setValue(String value) {
        this.newNameInMetadata = value?.trim()?.replaceAll(" +", " ")
        if (this.newNameInMetadata == "") {
            this.newNameInMetadata = null
        }
    }
}

class UpdateAnalysisDirectoryCommand implements Serializable {
    String analysisDirectory
    Project project
    static constraints = {
        analysisDirectory(nullable: true, validator: { String val ->
            if (!OtpPath.isValidAbsolutePath(val)) {
                return 'Is not an absolute Path'
            }
        })
    }
    void setValue(String value) {
        this.analysisDirectory = value
    }
}

class UpdateSnvCommand implements Serializable {
    Project project
    String value
}

class UpdateCategoryCommand implements Serializable {
    List<String> value = [].withLazyDefault {new String()}
    Project project
}

class UpdateMailingListNameCommand implements Serializable {
    String mailingListName
    Project project
    void setValue(String mailingListName) {
        this.mailingListName = mailingListName
    }
}

class UpdateCostCenterCommand implements Serializable {
    String costCenter
    Project project
    void setValue(String costCenter) {
        this.costCenter = costCenter
    }
}

class UpdateDescriptionCommand implements Serializable {
    String description
    Project project
    void setValue(String description) {
        this.description = description
    }
}

class UpdateProcessingPriorityCommand implements Serializable {
    Project project
    short processingPriority
    void setValue(String value) {
        switch (value) {
            case "NORMAL":
                this.processingPriority = ProcessingPriority.NORMAL_PRIORITY
                break
            case "FAST_TRACK":
                this.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
                break
        }
    }
}
