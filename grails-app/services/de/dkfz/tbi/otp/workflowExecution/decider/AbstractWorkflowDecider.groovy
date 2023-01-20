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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Transactional
import grails.util.Pair
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflowExecution.*

/**
 * creates workflow runs for one workflow
 * knows the needed input and created output workflow artefacts
 * knows all requirements
 * is called with a list of new/changed workflow artefacts
 */
@CompileDynamic
@Transactional
@Slf4j
abstract class AbstractWorkflowDecider implements Decider {

    abstract protected Workflow getWorkflow()

    /**
     * Returns the artefact types supported by this decider.
     */
    abstract protected Set<ArtefactType> getSupportedInputArtefactTypes()

    /**
     * Returns the sequencing type of an artefact.
     */
    abstract protected SeqType getSeqType(WorkflowArtefact inputArtefact)

    /**
     * Search in database for additional required WorkflowArtefacts.
     * That are workflowArtefacts created by an earlier import and therefore not in the input list.
     * Will be implemented in concrete Deciders, since the decider per workflows knows what is needed
     * Make sure that in the search for the other required workflow artefacts no artefacts in state FAILED or OMITTED_MISSING_PRECONDITION or
     * withdrawn artefacts are considered)
     */
    abstract protected Collection<WorkflowArtefact> findAdditionalRequiredInputArtefacts(Collection<WorkflowArtefact> inputArtefacts)

    /**
     * Group all workflow artefacts based on the fact if they can be processed together within WorkflowRun (e.g. individual, sample type).
     * The inner collection are inputs for one workflow run..
     */
    abstract protected Collection<Collection<WorkflowArtefact>> groupArtefactsForWorkflowExecution(Collection<WorkflowArtefact> inputArtefacts,
                                                                                                   Map<String, String> userParams)

    /**
     * Iterate over the different collections
     * Check if all needed input WorkflowArtefacts are available
     * The input WorkflowArtefacts need to match all constraints, e.g. MergingCriteria, Bam Artefact for ACEseq needs to be the same used for sophia Artefact
     * At least one of the provided artefacts must be „new/changed“ (from the initialArtefacts list)
     * Check if no workflowRun exists for the same input WorkflowArtefacts which is not withdrawn.
     *     If there is no other workflowRun -> true
     *     If there is already another workflowRun check if the flag forceRun is set
     *     If this is not the case -> false
     *     If it is set -> check if the configuration used for the workflowRun was already the one which is currently configured for the project
     *         If yes -> false
     *         If no -> true
     * Create a workflow run if true.
     *     Create Workflow run in state PENDING
     *     Collect also the configurations and store them to the workflowRun
     *     Create all corresponding output WorkflowArtefacts
     * Returns all output WorkflowArtefacts or an empty list
     */
    abstract protected Collection<WorkflowArtefact> createWorkflowRunsAndOutputArtefacts(Collection<Collection<WorkflowArtefact>> groupedArtefacts,
                                                                                         Collection<WorkflowArtefact> initialArtefacts, WorkflowVersion version)

    /**
     * Group the artefacts by project and seqtype.
     */
    abstract protected Map<Pair<Project, SeqType>, List<WorkflowArtefact>> groupInputArtefacts(Collection<WorkflowArtefact> inputArtefacts)

    @Override
    final Collection<WorkflowArtefact> decide(Collection<WorkflowArtefact> inputArtefacts, boolean forceRun = false, Map<String, String> userParams = [:]) {
        Set<ArtefactType> supportedTypes = supportedInputArtefactTypes
        Set<SeqType> supportedSeqTypes = workflow.supportedSeqTypes
        Collection<WorkflowArtefact> filteredInputArtefacts = LogUsedTimeUtils.logUsedTime(log, "        filter artefacts") {
            inputArtefacts
                    .findAll { it.artefactType in supportedTypes }
                    .findAll { getSeqType(it) in supportedSeqTypes }
        }
        Map<Pair<Project, SeqType>, Set<WorkflowArtefact>> groupedInputArtefacts =
                LogUsedTimeUtils.logUsedTime(log, "        group artefacts by project and seqType") {
                    groupInputArtefacts(filteredInputArtefacts)
                }
        return LogUsedTimeUtils.logUsedTimeStartEnd(log, "        handle artefact groups") {
            groupedInputArtefacts.collectMany { entry ->
                LogUsedTimeUtils.logUsedTimeStartEnd(log, "          handle artefact group: ${entry.key.aValue} ${entry.key.bValue}") {
                    assert !entry.key.aValue.archived
                    WorkflowVersionSelector matchingWorkflows = WorkflowVersionSelector.createCriteria().get {
                        eq('project', entry.key.aValue)
                        eq('seqType', entry.key.bValue)
                        workflowVersion {
                            eq('workflow', workflow)
                        }
                        isNull('deprecationDate')
                    }
                    if (!matchingWorkflows) {
                        return []
                    }
                    Collection<WorkflowArtefact> combinedWorkflowArtefacts =
                            LogUsedTimeUtils.logUsedTime(log, "          fetch additional artefacts for group") {
                                (entry.value + findAdditionalRequiredInputArtefacts(entry.value)).unique()
                            }
                    Collection<Collection<WorkflowArtefact>> artefactsPerWorkflowRun =
                            LogUsedTimeUtils.logUsedTime(log, "          group artefact for workflow run") {
                                groupArtefactsForWorkflowExecution(combinedWorkflowArtefacts, userParams)
                            }
                    return LogUsedTimeUtils.logUsedTimeStartEnd(log, "          create ${artefactsPerWorkflowRun.size()} new artefacts and workflow runs") {
                        createWorkflowRunsAndOutputArtefacts(artefactsPerWorkflowRun, filteredInputArtefacts, matchingWorkflows.workflowVersion)
                    }
                }
            }
        }
    }
}
