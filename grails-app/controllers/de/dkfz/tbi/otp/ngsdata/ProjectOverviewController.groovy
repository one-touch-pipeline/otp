package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.utils.DataTableCommand

class ProjectOverviewController {

    public final String SAMPLE_IDENTIFIER = "sample-identifier"

    ProjectService projectService

    ProjectOverviewService projectOverviewService

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

        return [
            projects: projects,
            hideSampleIdentifier: hideSampleIdentifier(project),
            project: projectName,
            seqTypes: seqTypes
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
     * Retreives the data shown in the table of projectOverview/laneOverview.
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
        List< String> patients = projectOverviewService.mockPidByProject(project)
        List<List<String>> sampleTypes = projectOverviewService.overviewMockPidSampleType(project)
        List<SeqType> seqTypes = projectOverviewService.seqTypeByProject(project)

        boolean hideSampleIdentifier = hideSampleIdentifier(project)


        /*Map<mockPid, Map<sampleType, Map<SeqType / sample identifier constant, LaneCount / SampleIdentifier value>>>*/
        Map dataLastMap = [:]

        patients.each { String mockPid ->
            dataLastMap.put(mockPid, [:])
        }

        sampleTypes.each { mockPidSampleType ->
            dataLastMap[mockPidSampleType[0]].put(mockPidSampleType[1], [:])
        }

        if (!hideSampleIdentifier) {
            List<SampleIdentifier> sampleIdentifier = projectOverviewService.overviewSampleIdentifier(project)
            sampleIdentifier.each {mockPidSampleTypeMinSampleId ->
                dataLastMap.get(mockPidSampleTypeMinSampleId[0])?.get(mockPidSampleTypeMinSampleId[1])?.put(SAMPLE_IDENTIFIER, mockPidSampleTypeMinSampleId[2])
            }
        }

        seqTypes.each { SeqType seqType ->
            List lanes = projectOverviewService.laneCountPerPatientAndSampleType(project, seqType)
            lanes.each {mockPidSampleTypeLaneCount ->
                dataLastMap[mockPidSampleTypeLaneCount[0]][mockPidSampleTypeLaneCount[1]].put(seqType, mockPidSampleTypeLaneCount[2])
            }
        }

        List data = []
        dataLastMap.each {String individual, Map value1 ->
            value1.each { String sampleType, Map value2 ->
                List<String> line = [individual, sampleType]
                if (!hideSampleIdentifier) {
                    line << value2[SAMPLE_IDENTIFIER]
                }
                seqTypes.each { SeqType seqType ->
                    if (value2.containsKey(seqType)) {
                        line << value2[seqType]
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

    @PreAuthorize("hasRole('ROLE_MMML_MAPPING')")
    JSON dataTableMMMLMapping(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.tableForMMMLMapping()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

}
