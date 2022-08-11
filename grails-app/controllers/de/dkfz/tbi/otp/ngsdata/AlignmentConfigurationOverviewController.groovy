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
import org.springframework.security.access.annotation.Secured

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DataTableCommand

@Secured('isFullyAuthenticated()')
class AlignmentConfigurationOverviewController {

    static allowedMethods = [
            index                         : "GET",
            getAlignmentInfo              : "GET",
            dataTableSourceReferenceGenome: "GET",
    ]

    AlignmentInfoService alignmentInfoService
    MergingCriteriaService mergingCriteriaService
    PipelineService pipelineService
    ProjectSelectionService projectSelectionService
    SeqTypeService seqTypeService

    Map index() {
        Project project = projectSelectionService.selectedProject

        Map<SeqType, AlignmentInfo> alignmentInfo = alignmentInfoService.getAlignmentInformationForProject(project)

        return [
                seqTypeMergingCriteria: getSeqTypeMergingCriteria(project),
                roddySeqTypes         : roddySeqTypes,
                alignmentInfo         : alignmentInfo,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system
     */
    @Deprecated
    private Map<SeqType, MergingCriteria> getSeqTypeMergingCriteria(Project project) {
        List<MergingCriteria> mergingCriteria = mergingCriteriaService.findAllByProject(project)
        return SeqTypeService.allAlignableSeqTypes.findAll { !(it in seqTypeService.seqTypesNewWorkflowSystem) }.collectEntries { SeqType seqType ->
            [(seqType): mergingCriteria.find { it.seqType == seqType }]
        }.sort { Map.Entry<SeqType, MergingCriteria> it -> it.key.displayNameWithLibraryLayout }
    }

    /**
     * @deprecated method is part of the old workflow system
     */
    @Deprecated
    private List<SeqType> getRoddySeqTypes() {
        return SeqTypeService.roddyAlignableSeqTypes.findAll { !(it in seqTypeService.seqTypesNewWorkflowSystem) }.sort {
            it.displayNameWithLibraryLayout
        }
    }

    /**
     * @deprecated method is part of the old workflow system
     */
    @SuppressWarnings('CatchThrowable')
    @Deprecated
    JSON getAlignmentInfo() {
        Project project = projectSelectionService.requestedProject
        Map<String, AlignmentInfo> alignmentInfo = null
        String alignmentError = null
        try {
            alignmentInfo = alignmentInfoService.getAlignmentInformation(project)
        } catch (Throwable e) {
            alignmentError = e.message
            log.error(e.message, e)
        }

        Map map = [alignmentInfo: alignmentInfo, alignmentError: alignmentError]
        render(map as JSON)
    }

    /**
     * @deprecated method is part of the old workflow system
     */
    @SuppressWarnings('Indentation')
    @Deprecated
    JSON dataTableSourceReferenceGenome(DataTableCommand cmd) {
        Project project = projectSelectionService.requestedProject
        Map dataToRender = cmd.dataToRender()
        List data = alignmentInfoService.listReferenceGenome(project)
                .findAll { !(it.seqType in seqTypeService.seqTypesNewWorkflowSystem) }
                .collect { ReferenceGenomeProjectSeqType it ->
                    String adapterTrimming = ""
                    if (!it.sampleType) {
                        adapterTrimming = it.seqType.wgbs ?:
                                RoddyWorkflowConfig.getLatestForProject(
                                        project,
                                        it.seqType,
                                        pipelineService.findByPipelineName(Pipeline.Name.PANCAN_ALIGNMENT)
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
        render(dataToRender as JSON)
    }
}
