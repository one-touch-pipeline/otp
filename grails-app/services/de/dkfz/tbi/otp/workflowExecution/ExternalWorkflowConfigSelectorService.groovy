/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Slf4j
@Transactional(readOnly = true)
class ExternalWorkflowConfigSelectorService {

    ConfigSelectorService configSelectorService

    ExternalWorkflowConfigSelector getById(long id) {
        return ExternalWorkflowConfigSelector.get(id)
    }

    ExternalWorkflowConfigSelector findExactlyOneByExternalWorkflowConfigFragment(ExternalWorkflowConfigFragment fragment) {
        return CollectionUtils.exactlyOneElement(
                ExternalWorkflowConfigSelector.findAllByExternalWorkflowConfigFragment(fragment))
    }

    ExternalWorkflowConfigSelectorLists searchExternalWorkflowConfigSelectors(ExternalWorkflowConfigSelectorSearchParameter searchParameter) {
        List<ExternalWorkflowConfigSelector> relatedSelectors, exactlyMatchingSelectors
        if (searchParameter.hasIds()) {
            Set<Workflow> selectedWorkflows = searchParameter.workflowIds.collect {
                CollectionUtils.exactlyOneElement(Workflow.findAllById(it))
            } as Set<Workflow>
            Set<WorkflowVersion> selectedWorkflowVersions = searchParameter.workflowVersionIds.collect {
                CollectionUtils.exactlyOneElement(WorkflowVersion.findAllById(it))
            } as Set<WorkflowVersion>
            Set<Project> selectedProjects = searchParameter.projectIds.collect {
                CollectionUtils.exactlyOneElement(Project.findAllById(it))
            } as Set<Project>
            Set<SeqType> selectedSeqTypes = searchParameter.seqTypeIds.collect {
                CollectionUtils.exactlyOneElement(SeqType.findAllById(it))
            } as Set<SeqType>
            Set<ReferenceGenome> selectedReferenceGenomes = searchParameter.referenceGenomeIds.collect {
                CollectionUtils.exactlyOneElement(ReferenceGenome.findAllById(it))
            } as Set<ReferenceGenome>
            Set<LibraryPreparationKit> selectedLibraryPreparationKits = searchParameter.libraryPreparationKitIds.collect {
                CollectionUtils.exactlyOneElement(LibraryPreparationKit.findAllById(it))
            } as Set<LibraryPreparationKit>

            MultiSelectSelectorExtendedCriteria criteria = new MultiSelectSelectorExtendedCriteria(
                    selectedWorkflows,
                    selectedWorkflowVersions,
                    selectedProjects,
                    selectedSeqTypes,
                    selectedReferenceGenomes,
                    selectedLibraryPreparationKits
            )

            exactlyMatchingSelectors = configSelectorService.findExactSelectors(criteria)

            relatedSelectors = configSelectorService.findAllRelatedSelectors(criteria)
            if (searchParameter.type) {
                relatedSelectors = relatedSelectors.findAll { ExternalWorkflowConfigSelector sel ->
                    searchParameter.type.contains(sel.selectorType.toString())
                }
            }
        } else {
            exactlyMatchingSelectors = []
            relatedSelectors = ExternalWorkflowConfigSelector.findAllBySelectorType(
                    SelectorType.DEFAULT_VALUES
            )
        }
        return new ExternalWorkflowConfigSelectorLists(relatedSelectors, exactlyMatchingSelectors)
    }
}
