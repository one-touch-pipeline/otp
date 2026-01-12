/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.workflow.TriggerWorkflowsService

@PreAuthorize('isFullyAuthenticated()')
class SampleCategoryController {

    static allowedMethods = [
            index : "GET",
            update: "POST",
    ]

    ProjectSelectionService projectSelectionService
    SamplePairDeciderService samplePairDeciderService
    SampleTypePerProjectService sampleTypePerProjectService
    SampleTypeService sampleTypeService
    SecurityService securityService
    TriggerWorkflowsService triggerWorkflowsService

    Map index(SampleCategoryEditCommand cmd) {
        Project project = projectSelectionService.selectedProject

        boolean isAdmin = securityService.hasCurrentUserAdministrativeRoles()
        boolean edit = isAdmin ? cmd.edit : false

        List<SampleTypePerProject> sampleTypePerProjects = sampleTypePerProjectService.findByProject(project)

        List<SampleType> sampleTypes = (sampleTypeService.findUsedSampleTypesForProject(project) +
                sampleTypePerProjects*.sampleType
        ).unique().sort { it.name }

        Map<SampleType, SampleTypePerProject> groupedCategories = sampleTypePerProjects
                .groupBy { it.sampleType }
                .collectEntries { sampleType, sampleTypePerProjects1 ->
                    [sampleType, sampleTypePerProjects1.first()?.category]
                }

        return [
                categories       : SampleTypePerProject.Category.values(),
                sampleTypes      : sampleTypes,
                groupedCategories: groupedCategories,
                edit             : edit,
        ]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def update(SampleCategoryCommand cmd) {
        assert cmd.validate()
        Project project = projectSelectionService.requestedProject

        // Find the modified sampleTypes
        Map<SampleType, SampleTypePerProject.Category> categories = sampleTypePerProjectService.findByProject(project).collectEntries {
            [it.sampleType, it.category]
        }
        Set<SampleType> sampleTypesChanged = cmd.sampleTypes.findAll { SampleCategorySampleTypeCommand sampleType ->
            return sampleType.category != categories[sampleType.sampleType]
        }*.sampleType

        Project.withTransaction {
            cmd.sampleTypes.each { SampleCategorySampleTypeCommand sampleType ->
                sampleTypePerProjectService.createOrUpdate(project, sampleType.sampleType, sampleType.category)
            }
        }

        // Sample pairs are created only if their sample types have been changed. Has to stay until snv, indel and sophia are created in new system
        samplePairDeciderService.createSamplePairs(project, sampleTypesChanged)
        triggerWorkflowsService.triggerWorkflowByProjectAndSampleTypes(project, sampleTypesChanged)

        flash.message = new FlashMessage(g.message(code: "sampleCategory.edit.success") as String)
        redirect(action: "index")
    }
}

class SampleCategoryEditCommand {
    Boolean edit
}

class SampleCategoryCommand {
    List<SampleCategorySampleTypeCommand> sampleTypes
}

class SampleCategorySampleTypeCommand {
    SampleType sampleType
    SampleTypePerProject.Category category
}
