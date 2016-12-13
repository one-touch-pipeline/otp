package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import org.springframework.validation.FieldError

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.getOrPut

class ProjectOverviewController {

    public final String SAMPLE_IDENTIFIER = "sample-identifier"

    ProjectService projectService

    ProjectOverviewService projectOverviewService

    SeqTrackService seqTrackService

    ContactPersonService contactPersonService

    SampleTypePerProjectService sampleTypePerProjectService

    ProcessingThresholdsService processingThresholdsService

    SeqTypeService seqTypeService

    CommentService commentService

    Map index() {
        String projectName = params.projectName
        return [projects: projectService.getAllProjects()*.name, project: projectName]
    }

    /**
     * the basic data for the page projectOverview/laneOverview. The table content are retrieved asynchrony from {@link #dataTableSourceLaneOverview} via JavaScript.
     */
    Map laneOverview() {
        List<String> projects = projectService.getAllProjects()*.name
        String projectName = params.project ?: projects[0]

        Project project = projectService.getProjectByName(projectName)
        List<SeqType> seqTypes  = projectOverviewService.seqTypeByProject(project)
        List<String> sampleTypes  = projectOverviewService.sampleTypeByProject(project)
        String sampleTypeName = (params.sampleType && sampleTypes.contains(params.sampleType)) ? params.sampleType : sampleTypes[0]

        return [
            projects: projects,
            hideSampleIdentifier: ProjectOverviewService.hideSampleIdentifier(project),
            project: projectName,
            seqTypes: seqTypes,
            sampleTypes: sampleTypes,
            sampleType: sampleTypeName,
            pipelines: findPipelines(),
        ]
    }

    Map mmmlIdentifierMapping() {
        return [:]
    }

    Map specificOverview() {
        List<Project> projects = projectService.getAllProjects()
        Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(params.project ?: projects.first().name, [fetch: [projectCategories: 'join', projectGroup: 'join']]))

        Map<String, ProjectOverviewService.AlignmentInfo> alignmentInfo = null
        String alignmentError = null
        try {
            alignmentInfo = projectOverviewService.getAlignmentInformation(project)
        } catch (Exception e) {
            alignmentError = e.message
            log.error(e.message, e)
        }
        List configTable

        List<ProjectContactPerson> projectContactPersons = ProjectContactPerson.findAllByProject(project)
        List<String> contactPersonRoles = [''] + ContactPersonRole.findAll()*.name

        List<List> thresholdsTable = createThresholdTable(project)

        configTable = buildTableForConfig(project)
        List configTableHeadline = getHeadline(configTable)
        configTable.remove(configTableHeadline)

        Realm realm = ConfigService.getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                project.dirName,
        )

        List accessPersons = projectOverviewService.getAccessPersons(project)

        return [
                projects: projects*.name,
                project: project,
                comment: project.comment,
                nameInMetadata: project.nameInMetadataFiles?: '',
                seqTypes: SeqType.panCanAlignableSeqTypes,
                snvSeqTypes: SeqType.snvPipelineSeqTypes,
                indelSeqTypes: SeqType.indelPipelineSeqTypes,
                alignmentInfo: alignmentInfo,
                alignmentError: alignmentError,
                snv: project.snv,
                projectContactPersons: projectContactPersons,
                roleDropDown: contactPersonRoles,
                thresholdsTable: thresholdsTable,
                configTableHeadline: configTableHeadline,
                configTable: configTable,
                snvDropDown: Project.Snv.values(),
                directory: projectDirectory,
                analysisDirectory: project.dirAnalysis?: '',
                projectGroup: project.projectGroup,
                copyFiles: project.hasToBeCopied,
                mailingListName: project.mailingListName,
                description: project.description,
                projectCategories: ProjectCategory.listOrderByName(),
                accessPersons: accessPersons,
        ]
    }

    class InfoAboutOneSample {
        String sampleIdentifier
        // Map<SeqType.id, value>>
        Map<Long, String> laneCountRegistered = [:]
        // Map<SeqType.id, Map<Pipeline.id, Collection<bamFileInProjectFolder>>>
        Map<Long, Map<Long, Collection<AbstractMergedBamFile>>> bamFilesInProjectFolder = [:]
    }

    /**
     * Retrieves the data shown in the table of projectOverview/laneOverview.
     * The data structure is:
     * <pre>
     * [[mockPid1, sampleTypeName1, one sample identifier for mockPid1.sampleType1, lane count for seqType1, lane count for seqType2, ...],
     * [mockPid1, sampleTypeName2, one sample identifier for mockPid1.sampleType2, lane count for seqType1, lane count for seqType2, ...],
     * [mockPid2, sampleTypeName2, one sample identifier for mockPid1.sampleType2, lane count for seqType1, lane count for seqType2, ...],
     * ...]
     * </pre>
     * The available seqTypes are depend on the selected Project.
     */
    JSON dataTableSourceLaneOverview(DataTableCommand cmd) {

        boolean anythingWithdrawn = false
        Project project = projectService.getProjectByName(params.project)

        List<SeqType> seqTypes = projectOverviewService.seqTypeByProject(project)
        boolean hideSampleIdentifier = ProjectOverviewService.hideSampleIdentifier(project)
        /*Map<mockPid, Map<sampleTypeName, InformationOfSample>>*/
        Map dataLastMap = [:]

        /**
         * returns the InfoAboutOneSample for the given mock pid and sample type name.
         * The InfoAboutOneSample are stored in a map of map structure in the variable dataLastMap.
         * If no one exist yet, it is created.
         */
        def getDataForMockPidAndSampleTypeName = { String mockPid, String sampleTypeName ->
            Map<String, InfoAboutOneSample> informationOfSampleMap = dataLastMap[mockPid]
            if (!informationOfSampleMap) {
                informationOfSampleMap = [:]
                dataLastMap.put(mockPid, informationOfSampleMap)
            }
            InfoAboutOneSample informationOfSample = informationOfSampleMap[sampleTypeName]
            if (!informationOfSample) {
                informationOfSample = new InfoAboutOneSample()
                informationOfSampleMap.put(sampleTypeName, informationOfSample)
            }
            return informationOfSample
        }

        List lanes = projectOverviewService.laneCountForSeqtypesPerPatientAndSampleType(project)
        lanes.each {
            InfoAboutOneSample informationOfSample = getDataForMockPidAndSampleTypeName(it.mockPid, it.sampleTypeName)
            informationOfSample.laneCountRegistered.put(it.seqType.id, it.laneCount)
        }

        projectOverviewService.abstractMergedBamFilesInProjectFolder(project).each {
            assert it.numberOfMergedLanes != null
            InfoAboutOneSample informationOfSample = getDataForMockPidAndSampleTypeName(it.individual.mockPid, it.sampleType.name)
            getOrPut(getOrPut(informationOfSample.bamFilesInProjectFolder, it.seqType.id, [:]), it.pipeline.id, []).add(it)
        }

        if (!hideSampleIdentifier) {
            List<SampleIdentifier> sampleIdentifier = projectOverviewService.overviewSampleIdentifier(project)
            sampleIdentifier.each { mockPidSampleTypeMinSampleId ->
                InfoAboutOneSample informationOfSample = getDataForMockPidAndSampleTypeName(mockPidSampleTypeMinSampleId[0], mockPidSampleTypeMinSampleId[1])
                informationOfSample.sampleIdentifier = mockPidSampleTypeMinSampleId[2]
            }
        }

        List data = []
        dataLastMap.each { String individual, Map<String, InfoAboutOneSample> dataMap ->
            dataMap.each { String sampleType, InfoAboutOneSample informationOfSample ->
                List<String> line = [individual, sampleType]
                if (!hideSampleIdentifier) {
                    line << informationOfSample.sampleIdentifier
                }
                seqTypes.each { SeqType seqType ->
                    line << informationOfSample.laneCountRegistered[seqType.id]

                    Map<Long, Collection<AbstractMergedBamFile>> bamFilesPerWorkflow = informationOfSample.bamFilesInProjectFolder.get(seqType.id)

                    findPipelines().each { Pipeline pipeline ->
                        String cell = ""
                        bamFilesPerWorkflow?.get(pipeline.id).each {
                            String subCell = "${it.numberOfMergedLanes} | ${it.coverage ? String.format(Locale.ENGLISH, '%.2f', it.coverage) : "unknown"}"
                            if (it.withdrawn) {
                                anythingWithdrawn = true
                                subCell = "<span class='withdrawn'>" + subCell + "</span>"
                            }
                            cell += "${subCell}<br>"
                        }
                        line << cell
                    }
                }
                data << line
            }
        }

        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        dataToRender.anythingWithdrawn = anythingWithdrawn

        render dataToRender as JSON
    }

    private List<Pipeline> findPipelines() {
        Pipeline.findAllByType(Pipeline.Type.ALIGNMENT, [sort: "id"])
    }

    JSON individualCountByProject(String projectName) {
        Project project = projectService.getProjectByName(projectName)
        Map dataToRender = [individualCount: projectOverviewService.individualCountByProject(project)]
        render dataToRender as JSON
    }

    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.overviewProjectQuery(params.project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourcePatientsAndSamplesGBCountPerProject(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.patientsAndSamplesGBCountPerProject(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceSampleTypeNameCountBySample(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.sampleTypeNameCountBySample(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceCenterNameRunId(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.centerNameRunId(project)
        List dataLast = projectOverviewService.centerNameRunIdLastMonth(project)

        Map dataLastMap = [:]
        dataLast.each {
            dataLastMap.put(it[0], it[1])
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = []
        data.each {
            List line = []
            line << it[0]
            line << it[1]
            if (dataLastMap.containsKey(it[0])) {
                line << dataLastMap.get(it[0])
            } else {
                line << "0"
            }
            dataToRender.aaData << line
        }
        render dataToRender as JSON
    }

    JSON dataTableSourceReferenceGenome(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.listReferenceGenome(project).collect { ReferenceGenomeProjectSeqType it->
            [it.seqType.name, it.sampleType?.name, it.referenceGenome.name, it.statSizeFileName ?: ""]
        }
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableMMMLMapping(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.tableForMMMLMapping()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON checkForAlignment(String projectName) {
        Project project = projectService.getProjectByName(projectName)
        AlignmentDecider decider = seqTrackService.getAlignmentDecider(project)
        Map dataToRender = [alignmentMessage: "Project ${project.name} ${decider.alignmentMessage()}."]
        render dataToRender as JSON
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
        return roleName.isEmpty() ? null : CollectionUtils.exactlyOneElement(ContactPersonRole.findAllByName(roleName))
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
        checkErrorAndCallMethod(cmd, { projectService.updateNameInMetadata(cmd.newNameInMetadata, projectService.getProjectByName(cmd.projectName)) })
    }

    JSON updateCategory(UpdateCategoryCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateCategory(cmd.value, cmd.project) })
    }

    JSON updateMailingListName(UpdateMailingListNameCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateMailingListName(cmd.mailingListName, projectService.getProjectByName(cmd.projectName)) })
    }

    JSON updateDescription(UpdateDescriptionCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateDescription(cmd.description, cmd.project) })
    }

    JSON updateDates(String projectName) {
        Timestamp[] timestamps = Sequence.createCriteria().get {
            eq("projectName", projectName)
            projections {
                min("dateCreated")
                max("dateCreated")
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        Map dataToRender = [creationDate: timestamps[0] ? simpleDateFormat.format(timestamps[0]) : null, lastReceivedDate: timestamps[0] ? simpleDateFormat.format(timestamps[1]) : null]
        render dataToRender as JSON
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

    private List buildTableForConfig(Project project) {
        List table = []
        table.add(["", "Config uploaded", "Version"])
        SeqType.getSnvPipelineSeqTypes().each {
            List row = []
            row.add(it.displayName)
            SnvConfig snvConfig = CollectionUtils.atMostOneElement(SnvConfig.findAllByProjectAndSeqTypeAndObsoleteDate(project, it, null))
            if (snvConfig) {
                row.add("Yes")
                row.add(snvConfig.externalScriptVersion)
            } else {
                RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndObsoleteDateAndPipeline(project, it, null, Pipeline.findByName(Pipeline.Name.RODDY_SNV)))

                if (roddyWorkflowConfig) {
                    row.add("Yes")
                    row.add(roddyWorkflowConfig.pluginVersion)
                } else {
                    row.add("No")
                    row.add("")
                }
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

    private List getHeadline(List list) {
        if (list) {
            return list.first()
        }
        return []
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
    String projectName
    static constraints = {
        newNameInMetadata(nullable: true, validator: { val, obj ->
            Project projectByNameInMetadata = Project.findByNameInMetadataFiles(val)
            if (val && projectByNameInMetadata && projectByNameInMetadata.name != obj.projectName) {
                return '\'' + val + '\' exists already in another project as nameInMetadataFiles entry'
            }
            if (val != obj.projectName && Project.findByName(val)) {
                return '\'' + val + '\' is used in another project as project name'
            }
        })
        projectName(blank: false)
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
    String projectName
    void setValue(String mailingListName) {
        this.mailingListName = mailingListName
    }
}

class UpdateDescriptionCommand implements Serializable {
    String description
    Project project
    void setValue(String description) {
        this.description = description
    }
}
