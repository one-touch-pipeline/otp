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

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService

class ConfigureAnalysisController {

    IndividualService individualService
    ProcessingThresholdsService processingThresholdsService
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    SampleTypePerProjectService sampleTypePerProjectService
    SampleTypeService sampleTypeService

    Map index(AnalysisCommand cmd) {
        if (params.submit) {
            handleSubmit(params, cmd.project)
        }

        Map map = [
                project: cmd.project,
        ] + fetchData(cmd.project)
        return map
    }

    private handleSubmit(Map params, Project project) {
        Project.withTransaction {
            Map map = fetchData(project)
            map['sampleTypes'].each { SampleType sampleType ->
                String categoryString = params["${project.name}!${sampleType.name}"]

                SampleType.Category category = categoryString as SampleType.Category
                assert category
                sampleTypePerProjectService.createOrUpdate(project, sampleType, category)
                map['seqTypes'].each { SeqType seqType ->
                    String numberOfLanesString = params["${project.name}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!numberOfLanes"]
                    String coverageString = params["${project.name}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!coverage"]
                    Long numberOfLanes = numberOfLanesString ? numberOfLanesString as Long : null
                    Double coverage = coverageString ? coverageString as Double : null
                    if (numberOfLanes != null || coverage != null) {
                        processingThresholdsService.createOrUpdate(project, sampleType, seqType, numberOfLanes, coverage)
                    }
                }
            }
        }
        projectSelectionService.setSelectedProject([project], project.name)
        redirect(controller: "projectConfig")
    }

    private Map fetchData(Project project) {
        List<SampleTypePerProject> sampleTypePerProjects = sampleTypePerProjectService.findByProject(project)
        List<ProcessingThresholds> processingThresholds = processingThresholdsService.findByProject(project)
        List<SampleType> sampleTypes = [
                sampleTypeService.findUsedSampleTypesForProject(project),
                sampleTypePerProjects*.sampleType,
                processingThresholds*.sampleType,
        ].flatten().unique { it.id }.sort { it.name }
        List<SeqType> seqTypes = SeqTypeService.getAllAnalysableSeqTypes()
        Map groupedDiseaseTypes = sampleTypePerProjects.groupBy { it.sampleType.id }
        Map groupedThresholds = processingThresholds.groupBy([{ it.sampleType.id }, { it.seqType.id }])
        return [
                categories         : SampleType.Category.values(),
                sampleTypes        : sampleTypes,
                seqTypes           : seqTypes,
                groupedDiseaseTypes: groupedDiseaseTypes,
                groupedThresholds  : groupedThresholds,
        ]
    }
}

class AnalysisCommand implements Serializable {
    Project project
    static constraints = {
        project(nullable: false)
    }
}
