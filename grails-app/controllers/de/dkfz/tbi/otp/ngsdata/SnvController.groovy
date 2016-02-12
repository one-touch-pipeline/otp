package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import de.dkfz.tbi.otp.ProcessingThresholds.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.SampleType.Category
import de.dkfz.tbi.otp.utils.DataTableCommand

class SnvController {

    ProjectService projectService
    SampleTypePerProjectService sampleTypePerProjectService
    SampleTypeService sampleTypeService
    SeqTypeService seqTypeService
    ProcessingThresholdsService processingThresholdsService
    IndividualService individualService

    Map index() {
        String projectName = params.projectName
        List<String> projects = projectService.getAllProjects()*.name
        if (!projectName) {
            projectName = projects.first()
        }
        Project project = projectService.getProjectByName(projectName)

        if (params.submit) {
            handleSubmit(params, project)
        }

        Map map = [
            project: project.name,
            projects: projects,
        ] + fetchData(project)
        return map
    }

    Map results() {
        return [:]
    }

    private handleSubmit(Map params, Project project) {
        Project.withTransaction {
            Map map = fetchData(project)
            map['sampleTypes'].each {SampleType sampleType ->
                String categoryString = params["${project.name}!${sampleType.name}"]

                Category category = categoryString as Category
                assert category
                sampleTypePerProjectService.createOrUpdate(project, sampleType, category)
                map['alignableSeqType'].each {SeqType seqType ->
                    String numberOfLanesString = params["${project.name}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!numberOfLanes"]
                    String coverageString = params["${project.name}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!coverage"]
                    Long numberOfLanes = numberOfLanesString ? numberOfLanesString as Long : null
                    Double coverage = coverageString ? coverageString as Double : null
                    if (numberOfLanes != null || coverage!= null) {
                        processingThresholdsService.createOrUpdate(project, sampleType, seqType, numberOfLanes, coverage)
                    }
                }
            }
        }
    }

    private Map fetchData(Project project) {
        List<SampleTypePerProject> sampleTypePerProjects = sampleTypePerProjectService.findByProject(project)
        List<ProcessingThresholds> processingThresholds = processingThresholdsService.findByProject(project)
        List<SampleType> sampleTypes = [
            sampleTypeService.findUsedSampleTypesForProject(project),
            sampleTypePerProjects*.sampleType,
            processingThresholds*.sampleType,
        ].flatten().unique{it.id}.sort {it.name}
        List<SeqType> alignableSeqType = seqTypeService.alignableSeqTypes()
        Map groupedDiseaseTypes = sampleTypePerProjects.groupBy{it.sampleType.id}
        Map groupedThresholds = processingThresholds.groupBy([{it.sampleType.id}, {it.seqType.id}])
        return [
            categories: SampleType.Category.values(),
            sampleTypes: sampleTypes,
            alignableSeqType: alignableSeqType,
            groupedDiseaseTypes: groupedDiseaseTypes,
            groupedThresholds: groupedThresholds
        ]
    }

    JSON dataTableSourceForIndividuals(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Project project = projectService.getProjectByName(params.project)
        List data = individualService.findAllMockPidsByProject(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data.collect{[it]}
        render dataToRender as JSON
    }

}

