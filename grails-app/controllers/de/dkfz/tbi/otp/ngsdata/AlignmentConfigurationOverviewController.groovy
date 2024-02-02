/*
 * Copyright 2011-2024 The OTP authors
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

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.project.Project

@PreAuthorize('isFullyAuthenticated()')
class AlignmentConfigurationOverviewController {

    static allowedMethods = [
            index                         : "GET",
    ]

    AlignmentInfoService alignmentInfoService
    MergingCriteriaService mergingCriteriaService
    ProjectSelectionService projectSelectionService
    SeqTypeService seqTypeService

    Map index() {
        Project project = projectSelectionService.selectedProject

        try {
            Map<SeqType, AlignmentInfo> alignmentInfo = alignmentInfoService.getAlignmentInformationForProject(project)
            return [
                    seqTypeMergingCriteria: getSeqTypeMergingCriteria(project),
                    alignmentInfo         : alignmentInfo,
            ]
        } catch (ParsingException exp) {
            return [
                    seqTypeMergingCriteria: getSeqTypeMergingCriteria(project),
                    errorMessage          : exp.message,
            ]
        }
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
}
