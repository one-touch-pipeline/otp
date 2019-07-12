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

import grails.converters.JSON
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.DataTableCommand

import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class ProjectConfigController implements CheckAndCall {

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
        Map<SeqType, MergingCriteria> seqTypeMergingCriteria = SeqTypeService.allAlignableSeqTypes.collectEntries { SeqType seqType ->
            [(seqType): mergingCriteria.find { it.seqType == seqType }]
        }.sort { Map.Entry<SeqType, MergingCriteria> it -> it.key.displayNameWithLibraryLayout }


        List<Map> cellRangerOverview = SeqTypeService.cellRangerAlignableSeqTypes.sort {
            it.name
        }.collect { SeqType seqType ->
            CellRangerConfig config = projectService.getLatestCellRangerConfig(project, seqType)
            ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(project, seqType)
            return [
                    seqType: seqType,
                    config : config,
            ]
        }

        List<List> thresholdsTable = createThresholdTable(project)

        Pipeline snv = Pipeline.findByName(Pipeline.Name.RODDY_SNV)
        Pipeline indel = Pipeline.findByName(Pipeline.Name.RODDY_INDEL)
        Pipeline sophia = Pipeline.findByName(Pipeline.Name.RODDY_SOPHIA)
        Pipeline aceseq = Pipeline.findByName(Pipeline.Name.RODDY_ACESEQ)
        Pipeline runYapsa = Pipeline.findByName(Pipeline.Name.RUN_YAPSA)

        List snvConfigTable = createAnalysisConfigTable(project, snv)
        List indelConfigTable = createAnalysisConfigTable(project, indel)
        List sophiaConfigTable = createAnalysisConfigTable(project, sophia)
        List aceseqConfigTable = createAnalysisConfigTable(project, aceseq)
        List runYapsaConfigTable = createAnalysisConfigTable(project, runYapsa)

        Map<SeqType, String> checkSophiaReferenceGenome = sophia.getSeqTypes().collectEntries {
            [(it): projectService.checkReferenceGenomeForSophia(project, it).getError()]
        }
        Map<SeqType, String> checkAceseqReferenceGenome = aceseq.getSeqTypes().collectEntries {
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
                projects                       : projects,
                project                        : project,
                creationDate                   : dates.creationDate,
                lastReceivedDate               : dates.lastReceivedDate,
                comment                        : project?.comment,
                nameInMetadata                 : project?.nameInMetadataFiles ?: '',
                seqTypeMergingCriteria         : seqTypeMergingCriteria,
                roddySeqTypes                  : SeqTypeService.getRoddyAlignableSeqTypes().sort {
                    it.displayNameWithLibraryLayout
                },
                cellRangerSeqTypes             : SeqTypeService.getCellRangerAlignableSeqTypes().sort {
                    it.displayNameWithLibraryLayout
                },
                snvSeqTypes                    : snv.getSeqTypes(),
                indelSeqTypes                  : indel.getSeqTypes(),
                sophiaSeqTypes                 : sophia.getSeqTypes(),
                aceseqSeqTypes                 : aceseq.getSeqTypes(),
                runYapsaSeqTypes               : runYapsa.getSeqTypes(),
                snv                            : project?.snv,
                thresholdsTable                : thresholdsTable,
                snvConfigTable                 : snvConfigTable,
                indelConfigTable               : indelConfigTable,
                sophiaConfigTable              : sophiaConfigTable,
                aceseqConfigTable              : aceseqConfigTable,
                runYapsaConfigTable            : runYapsaConfigTable,
                snvDropDown                    : Project.Snv.values(),
                directory                      : projectDirectory ?: '',
                analysisDirectory              : project?.dirAnalysis ?: '',
                projectGroup                   : project?.projectGroup,
                sampleIdentifierParserBeanName : project?.sampleIdentifierParserBeanName,
                sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values()*.name(),
                forceCopyFiles                 : project?.forceCopyFiles,
                fingerPrinting                 : project?.fingerPrinting,
                description                    : project?.description,
                processingNotification         : project?.processingNotification,
                qcTrafficLightNotification     : project?.qcTrafficLightNotification,
                customFinalNotification        : project?.customFinalNotification,
                projectCategories              : ProjectCategory.listOrderByName()*.name,
                unixGroup                      : project?.unixGroup,
                costCenter                     : project?.costCenter,
                tumorEntities                  : TumorEntity.list().sort(),
                tumorEntity                    : project?.tumorEntity,
                processingPriority             : ProcessingPriority.getByPriorityNumber(project?.processingPriority),
                processingPriorities           : ProcessingPriority.displayPriorities,
                checkSophiaReferenceGenome     : checkSophiaReferenceGenome,
                checkAceseqReferenceGenome     : checkAceseqReferenceGenome,
                projectInfos                   : project?.projectInfos,
                cellRangerOverview             : cellRangerOverview,
                qcThresholdHandling            : project?.qcThresholdHandling,
                qcThresholdHandlingDropdown    : QcThresholdHandling.values(),
                speciesWithStrain              : project?.speciesWithStrain,
                allSpeciesWithStrain           : SpeciesWithStrain.list().sort { it.toString() } ?: [],
                addProjectInfos                : flash?.addProjectInfos,
                closed                         : project?.closed,
        ]
    }

    JSON getAlignmentInfo(Project project) {
        Map<String, AlignmentInfo> alignmentInfo = null
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

    JSON updateDescription(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project) })
    }

    JSON updateUnixGroup(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project) })
    }

    JSON updateCostCenter(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project) })
    }

    JSON updateSpeciesWithStrain(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, { projectService.updateProjectField(SpeciesWithStrain.get(cmd.value), cmd.fieldName, cmd.project) })
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
            projectService.updateProjectField(ProcessingPriority.valueOf(cmd.value).priority, cmd.fieldName, cmd.project)
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

    JSON updateSampleIdentifierParserBeanName(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(SampleIdentifierParserBeanName.valueOf(cmd.value), cmd.fieldName, cmd.project)
        })
    }

    JSON updateQcThresholdHandling(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(QcThresholdHandling.valueOf(cmd.value), cmd.fieldName, cmd.project)
        })
    }

    JSON updateCopyFiles(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, cmd.project)
        })
    }

    JSON updateClosed(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, cmd.project)
        })
    }


    def addProjectInfo(AddProjectInfoCommand cmd) {
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage(g.message(code: "projectOverview.projectInfos.errorMessage") as String, cmd.errors)
                flash.addProjectInfos = cmd.values()
            } else {
                try {
                    projectService.createProjectInfoAndUploadFile(cmd)
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    flash.message = new FlashMessage(g.message(code: "projectOverview.projectInfos.exceptionMessage") as String, e.toString())
                    flash.addProjectInfos = cmd.values()
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.invalid.session") as String, '')
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

    JSON updateProcessingNotification(Long id, String value) {
        assert id
        Project project = Project.get(id)
        projectService.updateProcessingNotification(project, value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    JSON updateQcTrafficLightNotification(Long id, String value) {
        assert id
        Project project = Project.get(id)
        projectService.updateQcTrafficLightNotification(project, value.toBoolean())
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

    Map<String, String> getDates(Project project) {
        Timestamp[] timestamps = Sequence.createCriteria().get {
            eq("projectId", project?.id)
            projections {
                min("dateCreated")
                max("dateCreated")
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        return [
                creationDate    : timestamps[0] ? simpleDateFormat.format(timestamps[0]) : null,
                lastReceivedDate: timestamps[0] ? simpleDateFormat.format(timestamps[1]) : null,
        ]
    }

    private
    static List<List<String>> createAnalysisConfigTable(Project project, Pipeline pipeline) {
        List<List<String>> table = []
        table.add(["", "Config created", "Version"])
        pipeline.getSeqTypes().each { SeqType seqType ->
            List<String> row = []
            row.add(seqType.displayNameWithLibraryLayout)
            SnvConfig snvConfig = atMostOneElement(SnvConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType))
            RunYapsaConfig runYapsaConfig = atMostOneElement(RunYapsaConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType))
            RoddyWorkflowConfig roddyWorkflowConfig = atMostOneElement(
                    RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateIsNull(project, seqType, pipeline))
            if (pipeline.type == Pipeline.Type.SNV && snvConfig) {
                row.add("Yes")
                row.add(snvConfig.externalScriptVersion)
            } else if (pipeline.name == Pipeline.Name.RUN_YAPSA && runYapsaConfig) {
                row.add("Yes")
                row.add(runYapsaConfig.programVersion)
            } else if (pipeline.usesRoddy() && roddyWorkflowConfig) {
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
        List<SeqType> seqTypes = SeqTypeService.getAllAnalysableSeqTypes()

        List row = []
        row.add(message(code: "projectOverview.analysis.sampleType"))
        row.add(message(code: "projectOverview.analysis.category"))
        seqTypes.each {
            row.add(message(code: "projectOverview.analysis.minLanes", args: [it.displayNameWithLibraryLayout]))
            row.add(message(code: "projectOverview.analysis.coverage", args: [it.displayNameWithLibraryLayout]))
        }
        thresholdsTable.add(row)

        sampleTypeService.findUsedSampleTypesForProject(project).each { SampleType sampleType ->
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

    private static List removeEmptyColumns(List orgList) {
        List list = orgList.transpose()
        list.removeAll {
            it.findAll { it == null }.size() == (it.size() - 1)
        }
        return list.transpose()
    }

    JSON dataTableSourceReferenceGenome(DataTableCommand cmd) {
        Project project = projectService.getProject(params.project as Long)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.listReferenceGenome(project).collect { ReferenceGenomeProjectSeqType it ->
            String adapterTrimming = ""
            if (!it.sampleType) {
                adapterTrimming = it.seqType.isWgbs() ?:
                            RoddyWorkflowConfig.getLatestForProject(
                                    project,
                                    it.seqType,
                                    Pipeline.findByName(Pipeline.Name.PANCAN_ALIGNMENT)
                            )?.adapterTrimmingNeeded
            }
            return [
                    it.seqType.displayNameWithLibraryLayout,
                    it.sampleType?.name,
                    it.referenceGenome.name,
                    it.statSizeFileName ?: "",
                    adapterTrimming,
            ]
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
            flash.message = new FlashMessage("No file '${cmd.projectInfo.fileName}' found.")
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
    String recipient
    String commissioningUser
    String transferDate
    String validityDate
    String transferMode
    String legalBasis

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
        commissioningUser(validator: { val ->
            if (val && !User.findByUsername(val)) {
                return "Could not find user with username: ${val}"
            }
        })
        transferDate(validator: { val ->
            if (val && !isDateParsable(val)) {
                return "The value '${val}' is not in the expected date format 'yyyy-mm-dd'"
            }
        })
        validityDate(validator: { val ->
            if (val && !isDateParsable(val)) {
                return "The value '${val}' is not in the expected date format 'yyyy-mm-dd'"
            }
        })
        recipient(validator: { val, obj, Errors errors ->
                List<String> elements = [
                        obj.recipient,
                        obj.commissioningUser,
                        obj.transferDate,
                        obj.validityDate,
                        obj.transferMode,
                        obj.legalBasis,
                                        ]
            int givenValues = elements.findAll().size()
            if (givenValues != 0 && givenValues != elements.size()) {
                errors.reject(null, "Either all or none of the additional values have to be given: " +
                        "project, recipient, commissioningUser, transferDate, validityDate, transferMode, legalBasis")
            }
            return true
        })
    }

    static private boolean isDateParsable(String date) {
        try {
            new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH).parse(date)
            return true
        } catch (ParseException e) {
            return false
        }
    }

    Map<String, String> values() {
        return [
                recipient        : recipient,
                commissioningUser: commissioningUser,
                transferDate     : transferDate,
                validityDate     : validityDate,
                transferMode     : transferMode,
                legalBasis       : legalBasis,
        ]
    }
}

class UpdateCategoryCommand implements Serializable {
    List<String> value = [].withLazyDefault { new String() }
    Project project
}

class DownloadProjectInfoCommand implements Serializable {
    ProjectInfo projectInfo
}
