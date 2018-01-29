package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectOverviewController {

    ProjectService projectService
    ProjectOverviewService projectOverviewService
    SeqTrackService seqTrackService
    CommentService commentService
    ProjectSelectionService projectSelectionService


    Map index() {
        String projectName = params.project
        if (projectName) {
            Project project
            if ((project = projectService.getProjectByName(projectName))) {
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: controllerName, action: actionName)
                return
            }
        }
        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        return [
                projects: projects,
                project : project,
        ]
    }

    /**
     * the basic data for the page projectOverview/laneOverview. The table content are retrieved asynchrony from {@link #dataTableSourceLaneOverview} via JavaScript.
     */
    Map laneOverview() {
        String projectName = params.project
        if (projectName) {
            Project project
            if ((project =  projectService.getProjectByName(projectName))) {
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: controllerName, action: actionName)
                return
            }
        }

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        List<SeqType> seqTypes = projectOverviewService.seqTypeByProject(project)
        List<String> sampleTypes = projectOverviewService.sampleTypeByProject(project)
        String sampleTypeName = (params.sampleType && sampleTypes.contains(params.sampleType)) ? params.sampleType : sampleTypes[0]

        return [
                projects            : projects,
                hideSampleIdentifier: ProjectOverviewService.hideSampleIdentifier(project),
                project             : project,
                seqTypes            : seqTypes,
                sampleTypes         : sampleTypes,
                sampleType          : sampleTypeName,
                pipelines           : findPipelines(),
        ]
    }

    Map mmmlIdentifierMapping() {
        return [:]
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
                            String subCell
                            if (pipeline.name == Pipeline.Name.RODDY_RNA_ALIGNMENT) {
                                subCell = "${it.numberOfMergedLanes} | ${informationOfSample.laneCountRegistered[seqType.id]}"
                            } else {
                                subCell = "${it.numberOfMergedLanes} | ${it.coverage ? String.format(Locale.ENGLISH, '%.2f', it.coverage) : "unknown"}"
                            }
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

    JSON dataTableMMMLMapping(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.tableForMMMLMapping()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }
}
