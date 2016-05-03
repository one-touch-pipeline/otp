package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ProcessingThresholds.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.SampleType.Category
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

class SnvController {

    ProjectService projectService
    SampleTypePerProjectService sampleTypePerProjectService
    SampleTypeService sampleTypeService
    SeqTypeService seqTypeService
    ProcessingThresholdsService processingThresholdsService
    IndividualService individualService
    SnvService snvService

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
        String projectName = params.projectName
        List<String> projects = projectService.getAllProjects()*.name
        if (!projectName) {
            projectName = projects.first()
        }
        Project project = projectService.getProjectByName(projectName)

        return [
            projects: projects,
            project: project.name,
        ]
    }

    Map plots(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        SnvCallingInstance snvCallingInstance = snvService.getSnvCallingInstance(cmd.id as long)
        if (!snvCallingInstance) {
            return [
                    error: "Sorry, invalid snv calling instance",
                    pid: "Individual not found"
            ]
        }
        if (!snvCallingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath.exists()) {
            return [
                    error: "Sorry, there is no file available",
                    pid: snvCallingInstance.individual.pid
            ]
        }
        return [
            id: cmd.id,
            pid: snvCallingInstance.individual.pid,
            error: null
        ]
    }

    def renderPDF(RenderFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        SnvCallingInstance snvCallingInstance = snvService.getSnvCallingInstance(cmd.id as long)
        if (!snvCallingInstance) {
            render status: 404
            return
        }
        File stream = snvCallingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath
        if (!stream) {
            render status: 404
            return
        }
        if (!stream.exists()) {
            render status: 404
            return
        }

        render file: stream , contentType: "application/pdf"
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

    JSON dataTableSnvResults(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List results = snvService.getSnvCallingInstancesForProject(params.project)
        List data = results.collect { Map properties ->
            properties.libPrepKits = [properties.libPrepKit1, properties.libPrepKit2].unique().join(" / <br>")
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            if (properties.snvProcessingState != SnvProcessingStates.FINISHED) {
                properties.remove('snvInstanceId')
            }
            return properties
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}

class RenderFileCommand {
    String id

    static constraints = {
        id nullable: false
    }
}
