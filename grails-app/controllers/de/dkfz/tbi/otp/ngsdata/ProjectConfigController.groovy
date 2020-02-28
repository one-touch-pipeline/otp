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
import grails.validation.Validateable

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.DataTableCommand

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class ProjectConfigController implements CheckAndCall {

    ProjectService projectService
    ProjectOverviewService projectOverviewService
    ProcessingThresholdsService processingThresholdsService
    CommentService commentService
    ProjectSelectionService projectSelectionService
    SampleTypeService sampleTypeService
    ConfigService configService

    Map index() {
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects()

        Map<String, String> dates = getDates(project)

        File projectDirectory

        if (project) {
            projectDirectory = LsdfFilesService.getPath(
                    configService.rootPath.path,
                    project.dirName,
            )
        }

        return [
                creationDate                   : dates.creationDate,
                lastReceivedDate               : dates.lastReceivedDate,
                directory                      : projectDirectory ?: '',
                sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values()*.name(),
                tumorEntities                  : TumorEntity.list().sort(),
                projectTypes                   : Project.ProjectType.values(),
                processingPriority             : ProcessingPriority.getByPriorityNumber(project?.processingPriority),
                processingPriorities           : ProcessingPriority.displayPriorities,
                qcThresholdHandlingDropdown    : QcThresholdHandling.values(),
                allSpeciesWithStrain           : SpeciesWithStrain.list().sort { it.toString() } ?: [],
                allProjectGroups               : ProjectGroup.list(),
                closed                         : project?.closed,
        ]
    }

    Map alignment() {
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects()

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

        return [
                seqTypeMergingCriteria         : seqTypeMergingCriteria,
                roddySeqTypes                  : SeqTypeService.roddyAlignableSeqTypes.sort {
                    it.displayNameWithLibraryLayout
                },
                cellRangerSeqTypes             : SeqTypeService.cellRangerAlignableSeqTypes.sort {
                    it.displayNameWithLibraryLayout
                },
                cellRangerOverview             : cellRangerOverview,

        ]
    }

    Map analysis() {
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects()

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

        Map<SeqType, String> checkSophiaReferenceGenome = sophia.seqTypes.collectEntries {
            [(it): projectService.checkReferenceGenomeForSophia(project, it).error]
        }
        Map<SeqType, String> checkAceseqReferenceGenome = aceseq.seqTypes.collectEntries {
            [(it): projectService.checkReferenceGenomeForAceseq(project, it).error]
        }

        return [
                snvSeqTypes                    : snv.seqTypes,
                indelSeqTypes                  : indel.seqTypes,
                sophiaSeqTypes                 : sophia.seqTypes,
                aceseqSeqTypes                 : aceseq.seqTypes,
                runYapsaSeqTypes               : runYapsa.seqTypes,
                thresholdsTable                : thresholdsTable,
                snvConfigTable                 : snvConfigTable,
                indelConfigTable               : indelConfigTable,
                sophiaConfigTable              : sophiaConfigTable,
                aceseqConfigTable              : aceseqConfigTable,
                runYapsaConfigTable            : runYapsaConfigTable,
                checkSophiaReferenceGenome     : checkSophiaReferenceGenome,
                checkAceseqReferenceGenome     : checkAceseqReferenceGenome,
        ]
    }

    @SuppressWarnings('CatchThrowable')
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

    JSON updateProjectField(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(cmd.value, cmd.fieldName, cmd.project)
        }
    }

    JSON updateProjectFieldDate(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectFieldDate(cmd.value, cmd.fieldName, cmd.project)
        }
    }

    JSON updateProcessingPriority(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(ProcessingPriority.valueOf(cmd.value).priority, cmd.fieldName, cmd.project)
        }
    }

    JSON updateSpeciesWithStrain(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SpeciesWithStrain.get(cmd.value), cmd.fieldName, cmd.project)
        }
    }

    JSON updateTumorEntity(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(TumorEntity.findByName(cmd.value), cmd.fieldName, cmd.project)
        }
    }

    JSON updateProjectGroup(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(CollectionUtils.atMostOneElement(ProjectGroup.findAllByName(cmd.value)), cmd.fieldName, cmd.project)
        }
    }

    JSON updateSampleIdentifierParserBeanName(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(SampleIdentifierParserBeanName.valueOf(cmd.value), cmd.fieldName, cmd.project)
        }
    }

    JSON updateQcThresholdHandling(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(QcThresholdHandling.valueOf(cmd.value), cmd.fieldName, cmd.project)
        }
    }

    JSON updateCopyFiles(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, cmd.project)
        }
    }

    JSON updateClosed(UpdateProjectCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            projectService.updateProjectField(Boolean.valueOf(cmd.value), cmd.fieldName, cmd.project)
        }
    }

    JSON saveProjectComment(CommentCommand cmd) {
        Project project = projectService.getProject(cmd.id)
        commentService.saveComment(project, cmd.comment)
        Map dataToRender = [date: project.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: project.comment.author]
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
        pipeline.seqTypes.each { SeqType seqType ->
            List<String> row = []
            row.add(seqType.displayNameWithLibraryLayout)
            SnvConfig snvConfig = atMostOneElement(SnvConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType))
            RunYapsaConfig runYapsaConfig = atMostOneElement(RunYapsaConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType))
            RoddyWorkflowConfig roddyWorkflowConfig = atMostOneElement(
                    RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateIsNull(project, seqType, pipeline))
            if (pipeline.type == Pipeline.Type.SNV && snvConfig) {
                row.add("Yes")
                row.add(snvConfig.programVersion)
            } else if (pipeline.name == Pipeline.Name.RUN_YAPSA && runYapsaConfig) {
                row.add("Yes")
                row.add(runYapsaConfig.programVersion)
            } else if (pipeline.usesRoddy() && roddyWorkflowConfig) {
                row.add("Yes")
                row.add(roddyWorkflowConfig.programVersion)
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
        List<SeqType> seqTypes = SeqTypeService.allAlignableSeqTypes

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
                adapterTrimming = it.seqType.wgbs ?:
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
}

class UpdateProjectCommand implements Validateable {
    Project project
    String value
    String fieldName

    static constraints = {
        fieldName(nullable: true)
    }
}
