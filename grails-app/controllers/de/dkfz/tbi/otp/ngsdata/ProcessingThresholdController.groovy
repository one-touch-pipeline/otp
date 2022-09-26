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

import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.RolesService
import de.dkfz.tbi.otp.security.SecurityService

@Secured('isFullyAuthenticated()')
class ProcessingThresholdController {

    static allowedMethods = [
            index : "GET",
            update: "POST",
    ]

    ProcessingThresholdsService processingThresholdsService
    ProjectSelectionService projectSelectionService
    SamplePairDeciderService samplePairDeciderService
    SampleTypePerProjectService sampleTypePerProjectService
    SampleTypeService sampleTypeService
    UserService userService
    SecurityService securityService
    RolesService rolesService

    Map index(ProcThresholdsEditCommand cmd) {
        Project project = projectSelectionService.selectedProject

        boolean isAdmin = rolesService.isAdministrativeUser(securityService.currentUserAsUser)
        boolean edit = isAdmin ? cmd.edit : false

        List<SampleTypePerProject> sampleTypePerProjects = sampleTypePerProjectService.findByProject(project)
        List<ProcessingThresholds> processingThresholds = processingThresholdsService.findByProject(project)

        List<SampleType> sampleTypes = (sampleTypeService.findUsedSampleTypesForProject(project) +
                sampleTypePerProjects*.sampleType +
                processingThresholds*.sampleType
        ).unique().sort { it.name }

        Map<SampleType, SampleTypePerProject> groupedCategories = sampleTypePerProjects
                .groupBy { it.sampleType }
                .collectEntries { sampleType, sampleTypePerProjects1 ->
                    [sampleType, sampleTypePerProjects1.first()?.category]
                }

        Map<SampleType, Map<SeqType, ProcessingThresholds>> groupedThresholds = processingThresholds
                .groupBy { it.sampleType }
                .collectEntries { sampleType, processingThresholds1 ->
                    [sampleType, processingThresholds1
                            .groupBy { it.seqType }
                            .collectEntries { seqType, processingThresholds2 ->
                                [seqType, processingThresholds2.first()]
                            },
                    ]
                }

        List<SeqType> seqTypes = edit ? SeqTypeService.allAnalysableSeqTypes : processingThresholds*.seqType.unique()

        return [
                categories       : SampleTypePerProject.Category.values(),
                sampleTypes      : sampleTypes,
                seqTypes         : seqTypes.sort(),
                groupedCategories: groupedCategories,
                groupedThresholds: groupedThresholds,
                edit             : edit,
        ]
    }

    @Secured("hasRole('ROLE_OPERATOR')")
    def update(ProcThresholdsCommand cmd) {
        assert cmd.validate()
        Project project = projectSelectionService.requestedProject

        // Find the modified sampleTypes
        Map<SampleType, SampleTypePerProject.Category> categories = sampleTypePerProjectService.findByProject(project).collectEntries {
            [it.sampleType, it.category]
        }
        Set<SampleType> sampleTypesChanged = cmd.sampleTypes.findAll { ProcThresholdSampleTypeCommand sampleType ->
            return sampleType.category != categories[sampleType.sampleType]
        }*.sampleType

        Project.withTransaction {
            cmd.sampleTypes.each { ProcThresholdSampleTypeCommand sampleType ->
                sampleTypePerProjectService.createOrUpdate(project, sampleType.sampleType, sampleType.category)
                sampleType.seqTypes.each { ProcThresholdSeqTypeCommand seqType ->
                    processingThresholdsService.createUpdateOrDelete(
                            project, sampleType.sampleType, seqType.seqType, seqType.minNumberOfLanes ?: null, seqType.minCoverage ?: null
                    )
                }
            }
        }

        // Sample pairs are created only if their sample types have been changed
        samplePairDeciderService.createSamplePairs(project, sampleTypesChanged)
        flash.message = new FlashMessage("success")
        redirect(action: "index")
    }
}

class ProcThresholdsEditCommand {
    Boolean edit
}

class ProcThresholdsCommand {
    List<ProcThresholdSampleTypeCommand> sampleTypes
}

class ProcThresholdSampleTypeCommand {
    SampleType sampleType
    SampleTypePerProject.Category category
    List<ProcThresholdSeqTypeCommand> seqTypes
}

class ProcThresholdSeqTypeCommand {
    SeqType seqType
    Integer minNumberOfLanes
    Double minCoverage
}
