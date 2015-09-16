package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.AlignmentDecider
import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectOverviewController {

    public final String SAMPLE_IDENTIFIER = "sample-identifier"

    ProjectService projectService

    ProjectOverviewService projectOverviewService

    SeqTrackService seqTrackService

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
        List<SampleType> sampleTypes  = projectOverviewService.sampleTypeByProject(project)
        String sampleTypeName = (params.sampleType && sampleTypes.contains(params.sampleType)) ? params.sampleType : sampleTypes[0]

        return [
            projects: projects,
            hideSampleIdentifier: hideSampleIdentifier(project),
            project: projectName,
            seqTypes: seqTypes,
            sampleTypes: sampleTypes,
            sampleType: sampleTypeName
        ]
    }

    Map mmmlIdentifierMapping() {
        return [:]
    }

    /**
     * determine, if the column sample identifier should be hide in the view
     */
    private boolean hideSampleIdentifier(Project project) {
        return project.name == "MMML"
    }


    /**
     * A helper class to collect for a sample the {@link SampleIdentifier}, the lane count for the different {@link SeqType} and the calculated coverage.
     * It is used for transforming the data received from services to the form needed by dataTable.
     *
     *
     */
    class InformationOfSamples {
        String sampleIdentifier
        Map<SeqType, String> laneCount = [:]
        Map<SeqType, String> coverage = [:]
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

        Project project = projectService.getProjectByName(params.project)

        List<SeqType> seqTypes = projectOverviewService.seqTypeByProject(project)
        boolean hideSampleIdentifier = hideSampleIdentifier(project)
        /*Map<mockPid, Map<sampleTypeName, InformationOfSample>>*/
        Map dataLastMap = [:]

        /**
         * returns the InformationOfSamples for the given mock pid and sample type name.
         * The InformationOfSamples are stored in a map of map structure in the variable dataLastMap.
         * If no one exist yet, it is created.
         */
        def getDataForMockPidAndSampleTypeName = { String mockPid, String sampleTypeName ->
            Map<String, InformationOfSamples> informationOfSampleMap = dataLastMap[mockPid]
            if (!informationOfSampleMap) {
                informationOfSampleMap = [:]
                dataLastMap.put(mockPid, informationOfSampleMap)
            }
            InformationOfSamples informationOfSamples = informationOfSampleMap[sampleTypeName]
            if (!informationOfSamples) {
                informationOfSamples = new InformationOfSamples()
                informationOfSampleMap.put(sampleTypeName, informationOfSamples)
            }
            return informationOfSamples
        }

        List lanes = projectOverviewService.laneCountForSeqtypesPerPatientAndSampleType(project)
        lanes.each {
            InformationOfSamples informationOfSamples = getDataForMockPidAndSampleTypeName(it.mockPid, it.sampleTypeName)
            informationOfSamples.laneCount.put(it.seqType, it.laneCount)
        }

        List coverage = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(project)
        coverage.each {
            InformationOfSamples informationOfSamples = getDataForMockPidAndSampleTypeName(it.mockPid, it.sampleTypeName)
            informationOfSamples.coverage.put(it.seqType, it.coverage)
            informationOfSamples.laneCount.put(it.seqType, it.numberOfMergedLanes)
        }

        if (!hideSampleIdentifier) {
            List<SampleIdentifier> sampleIdentifier = projectOverviewService.overviewSampleIdentifier(project)
            sampleIdentifier.each {mockPidSampleTypeMinSampleId ->
                InformationOfSamples informationOfSample = getDataForMockPidAndSampleTypeName(mockPidSampleTypeMinSampleId[0], mockPidSampleTypeMinSampleId[1])
                informationOfSample.sampleIdentifier = mockPidSampleTypeMinSampleId[2]
            }
        }

        List data = []
        dataLastMap.each {String individual, Map<String, InformationOfSamples> dataMap ->
            dataMap.each { String sampleType, InformationOfSamples informationOfSamples ->
                List<String> line = [individual, sampleType]
                if (!hideSampleIdentifier) {
                    line << informationOfSamples.sampleIdentifier
                }
                seqTypes.each { SeqType seqType ->
                    if (informationOfSamples.laneCount[seqType]) {
                        String value = informationOfSamples.laneCount[seqType]
                        if (informationOfSamples.coverage[seqType]) {
                            value += " (coverage = ${String.format(Locale.ENGLISH, '%.2f', informationOfSamples.coverage[seqType])})"
                        }
                        line << value
                    } else {
                        line << ""
                    }
                }
                data << line
            }
        }

        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
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

    @PreAuthorize("hasRole('ROLE_MMML_MAPPING')")
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
}
