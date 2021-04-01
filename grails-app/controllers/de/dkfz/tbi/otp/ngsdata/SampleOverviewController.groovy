/*
 * Copyright 2011-2020 The OTP authors
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
import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DataTableCommand

@Secured('isFullyAuthenticated()')
class SampleOverviewController {

    ProjectSelectionService projectSelectionService
    SampleOverviewService sampleOverviewService
    SampleService sampleService

    /**
     * The basic data for the page projectOverview/laneOverview.
     * The table content are retrieved asynchronously from {@link #dataTableSourceLaneOverview} via JavaScript.
     */
    Map index() {
        Project project = projectSelectionService.selectedProject

        List<SeqType> seqTypes = sampleOverviewService.seqTypeByProject(project)
        List<String> sampleTypes = sampleOverviewService.sampleTypeByProject(project)
        String sampleTypeName = (params.sampleType && sampleTypes.contains(params.sampleType)) ? params.sampleType : sampleTypes[0]

        return [
                seqTypes   : seqTypes,
                sampleTypes: sampleTypes,
                sampleType : sampleTypeName,
                pipelines  : findPipelines(),
        ]
    }

    /**
     * Retrieves the data shown in the table of projectOverview/laneOverview.
     * The data structure is:
     * <pre>
     * [[mockPid1, sampleTypeName1, one sample name for mockPid1.sampleType1, lane count for seqType1, lane count for seqType2, ...],
     * [mockPid1, sampleTypeName2, one sample name for mockPid1.sampleType2, lane count for seqType1, lane count for seqType2, ...],
     * [mockPid2, sampleTypeName2, one sample name for mockPid1.sampleType2, lane count for seqType1, lane count for seqType2, ...],
     * ...]
     * </pre>
     * The available seqTypes are depend on the selected Project.
     */
    JSON dataTableSourceLaneOverview(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject

        List<SeqType> seqTypes = sampleOverviewService.seqTypeByProject(project)
        /*Map<mockPid, Map<sampleTypeName, InformationOfSample>>*/
        Map<String, Map<String, InfoAboutOneSample>> dataLastMap = [:].withDefault { [:].withDefault { new InfoAboutOneSample() } }

        sampleOverviewService.laneCountForSeqtypesPerPatientAndSampleType(project).each {
            dataLastMap[it.mockPid as String][it.sampleTypeName as String].laneCountRegistered[it.seqType.id as Long] = it.laneCount as String
        }

        sampleOverviewService.abstractMergedBamFilesInProjectFolder(project).each {
            dataLastMap[it.individual.mockPid][it.sampleType.name].bamFilesInProjectFolder[it.seqType.id][it.pipeline.id].add(it)
        }

        sampleService.getSamplesOfProject(project).each { Sample sample ->
            dataLastMap[sample.individual.mockPid][sample.sampleType.name]
        }

        List<Pipeline> pipelines = findPipelines()

        boolean anythingWithdrawn = false
        int numberOfFixesCols = 2
        int numberOfCols = numberOfFixesCols + (seqTypes.size() * (1 + pipelines.size()))
        Set<Integer> columnsToHide = numberOfCols == numberOfFixesCols ?
                [] as Set :
                (numberOfFixesCols..(numberOfCols - 1)) as Set

        List data = []
        dataLastMap.each { String individual, Map<String, InfoAboutOneSample> dataMap ->
            dataMap.each { String sampleType, InfoAboutOneSample informationOfSample ->
                List<String> line = [individual, sampleType]
                int columnNumber = numberOfFixesCols
                seqTypes.each { SeqType seqType ->
                    String laneCount = informationOfSample.laneCountRegistered[seqType.id]
                    line << laneCount
                    if (laneCount) {
                        columnsToHide.remove(columnNumber)
                    }
                    Map<Long, Collection<AbstractMergedBamFile>> bamFilesPerWorkflow = informationOfSample.bamFilesInProjectFolder.get(seqType.id)

                    pipelines.each { Pipeline pipeline ->
                        columnNumber++
                        String cell = ""
                        bamFilesPerWorkflow?.get(pipeline.id).each {
                            String subCell
                            if (pipeline.name == Pipeline.Name.RODDY_RNA_ALIGNMENT) {
                                subCell = "${it.numberOfMergedLanes} | ${informationOfSample.laneCountRegistered[seqType.id]}"
                            } else if (pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED) {
                                subCell = "<span class='icon-OKAY'>yes</span>"
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
                        if (cell) {
                            columnsToHide.remove(columnNumber)
                        }
                        line << cell
                    }
                    columnNumber++
                }
                data << line
            }
        }

        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        dataToRender.anythingWithdrawn = anythingWithdrawn
        dataToRender.columnsToHide = columnsToHide

        render dataToRender as JSON
    }

    private List<Pipeline> findPipelines() {
        Pipeline.findAllByType(Pipeline.Type.ALIGNMENT, [sort: "id"])
    }
}

class InfoAboutOneSample {
    // Map<SeqType.id, value>>
    Map<Long, String> laneCountRegistered = [:]
    // Map<SeqType.id, Map<Pipeline.id, Collection<bamFileInProjectFolder>>>
    Map<Long, Map<Long, Collection<AbstractMergedBamFile>>> bamFilesInProjectFolder = [:].withDefault { [:].withDefault { [] } }
}
