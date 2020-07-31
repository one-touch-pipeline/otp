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

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DataTableCommand

import static de.dkfz.tbi.otp.utils.CollectionUtils.getOrPut

class ProjectOverviewController {

    ProjectOverviewService projectOverviewService
    CommentService commentService
    ProjectSelectionService projectSelectionService
    SampleService sampleService

    Map index() {
        return [:]
    }

    /**
     * The basic data for the page projectOverview/laneOverview.
     * The table content are retrieved asynchronously from {@link #dataTableSourceLaneOverview} via JavaScript.
     */
    Map laneOverview() {
        Project project = projectSelectionService.selectedProject

        List<SeqType> seqTypes = projectOverviewService.seqTypeByProject(project)
        List<String> sampleTypes = projectOverviewService.sampleTypeByProject(project)
        String sampleTypeName = (params.sampleType && sampleTypes.contains(params.sampleType)) ? params.sampleType : sampleTypes[0]

        return [
                seqTypes            : seqTypes,
                sampleTypes         : sampleTypes,
                sampleType          : sampleTypeName,
                pipelines           : findPipelines(),
        ]
    }

    class InfoAboutOneSample {
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
        Project project = projectSelectionService.requestedProject

        List<SeqType> seqTypes = projectOverviewService.seqTypeByProject(project)
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
            InfoAboutOneSample informationOfSample = getDataForMockPidAndSampleTypeName(it.individual.mockPid, it.sampleType.name)
            getOrPut(getOrPut(informationOfSample.bamFilesInProjectFolder, it.seqType.id, [:]), it.pipeline.id, []).add(it)
        }

        sampleService.getSamplesOfProject(project).each { Sample sample ->
            getDataForMockPidAndSampleTypeName(sample.individual.mockPid, sample.sampleType.name)
        }

        List data = []
        dataLastMap.each { String individual, Map<String, InfoAboutOneSample> dataMap ->
            dataMap.each { String sampleType, InfoAboutOneSample informationOfSample ->
                List<String> line = [individual, sampleType]
                seqTypes.each { SeqType seqType ->
                    line << informationOfSample.laneCountRegistered[seqType.id]

                    Map<Long, Collection<AbstractMergedBamFile>> bamFilesPerWorkflow = informationOfSample.bamFilesInProjectFolder.get(seqType.id)

                    findPipelines().each { Pipeline pipeline ->
                        String cell = ""
                        bamFilesPerWorkflow?.get(pipeline.id).each {
                            String subCell
                            if (pipeline.name == Pipeline.Name.RODDY_RNA_ALIGNMENT) {
                                subCell = "${it.numberOfMergedLanes} | ${informationOfSample.laneCountRegistered[seqType.id]}"
                            } else if (pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED) {
                                subCell = "<span class='icon-OKAY'></span>"
                                if (it.coverage) {
                                    subCell += "| ${it.coverage ? String.format(Locale.ENGLISH, '%.2f', it.coverage) : "unknown"}"
                                }
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

    JSON individualCountByProject() {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = [individualCount: projectOverviewService.individualCountByProject(project)]
        render dataToRender as JSON
    }

    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Project project = projectSelectionService.requestedProject
        List data = projectOverviewService.overviewProjectQuery(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourcePatientsAndSamplesGBCountPerProject(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.patientsAndSamplesGBCountPerProject(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceSampleTypeNameCountBySample(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.sampleTypeNameCountBySample(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceCenterNameRunId(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
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
}
