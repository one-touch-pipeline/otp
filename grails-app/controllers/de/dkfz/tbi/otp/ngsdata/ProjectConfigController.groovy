package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import org.springframework.validation.*
import org.springframework.web.multipart.*

import java.sql.*
import java.text.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectConfigController {

    ProjectService projectService
    ProjectOverviewService projectOverviewService
    SeqTrackService seqTrackService
    ProcessingThresholdsService processingThresholdsService
    CommentService commentService
    ProjectSelectionService projectSelectionService
    SampleTypeService sampleTypeService
    ConfigService configService

    Map index() {
        List<Project> projects = projectService.getAllProjects()
        if (!projects) {
            return [
                    projects: projects,
            ]
        }
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        project = atMostOneElement(Project.findAllByName(project?.name, [fetch: [projectCategories: 'join', projectGroup: 'join']]))
        Map<String, String> dates = getDates(project)

        List<MergingCriteria> mergingCriteria = MergingCriteria.findAllByProject(project)
        Map<SeqType, MergingCriteria> seqTypeMergingCriteria = SeqType.roddyAlignableSeqTypes.collectEntries { SeqType seqType ->
            [(seqType): mergingCriteria.find { it.seqType == seqType }]
        }.sort { Map.Entry<SeqType, MergingCriteria> it -> it.key.displayNameWithLibraryLayout }

        List<List> thresholdsTable = createThresholdTable(project)

        List snvConfigTable = createAnalysisConfigTable(project, SeqType.getSnvPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_SNV))
        List indelConfigTable = createAnalysisConfigTable(project, SeqType.getIndelPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_INDEL))
        List sophiaConfigTable = createAnalysisConfigTable(project, SeqType.getSophiaPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_SOPHIA))
        List aceseqConfigTable = createAnalysisConfigTable(project, SeqType.getAceseqPipelineSeqTypes(), Pipeline.findByName(Pipeline.Name.RODDY_ACESEQ))

        Map<SeqType, String> checkSophiaReferenceGenome = SeqType.sophiaPipelineSeqTypes.collectEntries {
            [(it): projectService.checkReferenceGenomeForSophia(project, it).getError()]
        }
        Map<SeqType, String> checkAceseqReferenceGenome = SeqType.aceseqPipelineSeqTypes.collectEntries {
            [(it): projectService.checkReferenceGenomeForAceseq(project, it).getError()]
        }

        File projectDirectory

        if (project) {
            projectDirectory = LsdfFilesService.getPath(
                    configService.getRootPath().path,
                    project.dirName,
            )
        }

        return [
                projects                  : projects,
                project                   : project,
                creationDate              : dates.creationDate,
                lastReceivedDate          : dates.lastReceivedDate,
                comment                   : project?.comment,
                nameInMetadata            : project?.nameInMetadataFiles ?: '',
                seqTypeMergingCriteria    : seqTypeMergingCriteria,
                seqTypes                  : SeqType.roddyAlignableSeqTypes.sort { it.displayNameWithLibraryLayout },
                snvSeqTypes               : SeqType.snvPipelineSeqTypes,
                indelSeqTypes             : SeqType.indelPipelineSeqTypes,
                sophiaSeqType             : SeqType.sophiaPipelineSeqTypes,
                aceseqSeqType             : SeqType.aceseqPipelineSeqTypes,
                snv                       : project?.snv,
                thresholdsTable           : thresholdsTable,
                snvConfigTable            : snvConfigTable,
                indelConfigTable          : indelConfigTable,
                sophiaConfigTable         : sophiaConfigTable,
                aceseqConfigTable         : aceseqConfigTable,
                snvDropDown               : Project.Snv.values(),
                directory                 : projectDirectory ?: '',
                analysisDirectory         : project?.dirAnalysis ?: '',
                projectGroup              : project?.projectGroup,
                copyFiles                 : project?.hasToBeCopied,
                fingerPrinting            : project?.fingerPrinting,
                mailingListName           : project?.mailingListName,
                description               : project?.description,
                customFinalNotification   : project?.customFinalNotification,
                projectCategories         : ProjectCategory.listOrderByName()*.name,
                unixGroup                 : project?.unixGroup,
                costCenter                : project?.costCenter,
                tumorEntities             : TumorEntity.list().sort(),
                tumorEntity               : project?.tumorEntity,
                processingPriorities      : ProjectService.processingPriorities,
                checkSophiaReferenceGenome: checkSophiaReferenceGenome,
                checkAceseqReferenceGenome: checkAceseqReferenceGenome,
                projectInfos              : project?.projectInfos,
                hasErrors                 : params.hasErrors,
                message                   : params.message,
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

    JSON updateMailingListName(UpdateMailingListCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project) })
    }

    JSON updateDescription(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project) })
    }

    JSON updateCostCenter(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project) })
    }

    JSON updatePhabricatorAlias(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updatePhabricatorAlias(cmd.value, cmd.project) })
    }

    JSON updateAnalysisDirectory(UpdateAnalysisDirectoryCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(cmd.analysisDirectory, "dirAnalysis", cmd.project)
        })
    }

    JSON updateNameInMetadataFiles(UpdateNameInMetadataCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(cmd.newNameInMetadata, "nameInMetadataFiles", cmd.project)
        })
    }

    JSON updateCategory(UpdateCategoryCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateCategory(cmd.value, cmd.project) })
    }

    JSON updateProcessingPriority(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(ProcessingPriority."${cmd.value}_PRIORITY", cmd.fieldName, cmd.project)
        })
    }

    JSON updateTumorEntity(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(TumorEntity.findByName(cmd.value), cmd.fieldName, cmd.project)
        })
    }

    JSON updateSnv(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(Project.Snv.valueOf(cmd.value), cmd.fieldName, cmd.project)
        })
    }

    def addProjectInfo(AddProjectInfoCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = g.message(code: "projectOverview.projectInfos.errorMessage")
                flash.errors = cmd.errors
            } else {
                projectService.createProjectInfoAndUploadFile(cmd.project, cmd.projectInfoFile)
            }

        }.invalidToken {
            flash.message = g.message(code: "projectOverview.projectInfos.errorMessage")
        }
        redirect(action: "index")
    }

    JSON snvDropDown() {
        render Project.Snv.values() as JSON
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

    JSON updateCustomFinalNotification(Long id, String value) {
        assert id
        Project project = Project.get(id)
        projectService.updateCustomFinalNotification(project, value.toBoolean())
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
            eq("projectId", project?.id)
            projections {
                min("dateCreated")
                max("dateCreated")
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return [creationDate: timestamps[0] ? simpleDateFormat.format(timestamps[0]) : null, lastReceivedDate: timestamps[0] ? simpleDateFormat.format(timestamps[1]) : null]
    }

    private
    static List<List<String>> createAnalysisConfigTable(Project project, List<SeqType> seqTypes, Pipeline pipeline) {
        List<List<String>> table = []
        table.add(["", "Config created", "Version"])
        seqTypes.each { SeqType seqType ->
            List<String> row = []
            row.add(seqType.displayNameWithLibraryLayout)
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
            row.add(message(code: "projectOverview.analysis.minLanes", args: [it.displayNameWithLibraryLayout]))
            row.add(message(code: "projectOverview.analysis.coverage", args: [it.displayNameWithLibraryLayout]))
        }
        thresholdsTable.add(row)

        sampleTypeService.findUsedSampleTypesForProject(project).each() { SampleType sampleType ->
            row = []
            row.add(sampleType.name)
            row.add(sampleType.getCategory(project) ?: SampleType.Category.UNDEFINED)
            seqTypes.each {
                ProcessingThresholds processingThresholds = processingThresholdsService.findByProjectAndSampleTypeAndSeqType(project, sampleType, it)
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
            it.findAll { it == null }.size() == (it.size() - 1)
        }
        return list.transpose()
    }

    JSON dataTableSourceReferenceGenome(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.listReferenceGenome(project).collect { ReferenceGenomeProjectSeqType it ->
            String adapterTrimming = it.sampleType ? "" :
                    it.seqType.isWgbs() || it.seqType.isWgbs() ?:
                            RoddyWorkflowConfig.getLatestForProject(project, it.seqType, Pipeline.findByName(Pipeline.Name.PANCAN_ALIGNMENT))?.adapterTrimmingNeeded
            [it.seqType.displayNameWithLibraryLayout, it.sampleType?.name, it.referenceGenome.name, it.statSizeFileName ?: "", adapterTrimming]
        }
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    def download(DownloadProjectInfoCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }

        byte[] outputFile = projectService.getProjectInfoContent(cmd.projectInfo)

        if (outputFile) {
            render(file: outputFile, contentType: "application/octet-stream", fileName: cmd.projectInfo.fileName)
        } else {
            flash.message = "No file '${cmd.projectInfo.fileName}' found."
            redirect(action: "index")
        }

    }
}

class UpdateProjectCommand implements Serializable {
    Project project
    String value
    String fieldName

    static constraints = {
        fieldName(nullable: true)
    }
}

class UpdateMailingListCommand extends UpdateProjectCommand {
    static constraints = {
        value(nullable: true, validator: { val, obj ->
            if (val) {
                if (!(val.startsWith("tr_") && val.contains('@'))) {
                    return "Has to Start with 'tr_' and contain a '@'."
                }
            }
        })
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

class AddProjectInfoCommand implements Serializable {
    MultipartFile projectInfoFile
    Project project
    static constraints = {
        projectInfoFile(validator: { val, obj ->
            if (val.isEmpty()) {
                return "File is empty"
            }
            if (!OtpPath.isValidPathComponent(val.originalFilename)) {
                return "Invalid fileName"
            }
            if (ProjectInfo.findAllByProjectAndFileName(obj.project, val.originalFilename).size() != 0) {
                return "A ProjectInfo with this fileName already exists"
            }
            if (val.getSize() > ProjectService.PROJECT_INFO_MAX_SIZE) {
                return "The File exceeds the 20mb file size limit "
            }
        })
    }
}

class UpdateCategoryCommand implements Serializable {
    List<String> value = [].withLazyDefault { new String() }
    Project project
}

class DownloadProjectInfoCommand implements Serializable {
    ProjectInfo projectInfo
}
