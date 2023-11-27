/*
 * Copyright 2011-2023 The OTP authors
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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflowExecution.*

/**
 * creates workflow runs for one workflow
 * knows the needed input and created output workflow artefacts
 * knows all requirements
 * is called with a list of new/changed workflow artefacts
 */
@Transactional
@Slf4j
abstract class AbstractWorkflowDecider<ADL extends ArtefactDataList, G extends BaseDeciderGroup, AD extends AdditionalData> implements Decider {

    @Autowired
    WorkflowService workflowService

    /**
     * returns the workflow the decider created workflow runs for.
     */
    abstract protected Workflow getWorkflow()

    /**
     * Returns the artefact types supported by this decider.
     */
    abstract protected Set<ArtefactType> getSupportedInputArtefactTypes()

    abstract protected ADL fetchInputArtefacts(Collection<WorkflowArtefact> inputArtefacts, Set<SeqType> seqTypes)

    abstract protected ADL fetchAdditionalArtefacts(ADL inputArtefactDataList)

    abstract protected AD fetchAdditionalData(ADL inputArtefactDataList, Workflow workflow)

    abstract protected List<WorkflowVersionSelector> fetchWorkflowVersionSelector(ADL inputArtefactDataList, Workflow workflow)

    abstract protected Map<G, ADL> groupData(ADL inputArtefactDataList, AD additionalData, Map<String, String> userParams)

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
    @SuppressWarnings("ParameterCount")
    abstract protected DeciderResult createWorkflowRunsAndOutputArtefacts(
            ProjectSeqTypeGroup projectSeqTypeGroup, G group,
            ADL givenArtefacts, ADL additionalArtefacts,
            AD additionalData, WorkflowVersion version)

    @Override
    final DeciderResult decide(Collection<WorkflowArtefact> inputWorkflowArtefacts, Map<String, String> userParams = [:]) {
        DeciderResult deciderResult = new DeciderResult()
        Workflow w = workflow
        deciderResult.infos << "start decider for ${w}".toString()
        Set<SeqType> supportedSeqTypes = (workflowService.getSupportedSeqTypesOfVersions(w) ?: SeqType.list()) as Set

        ADL inputArtefactDataList = LogUsedTimeUtils.logUsedTime(log, "        fetch concrete Artefacts") {
            fetchInputArtefacts(inputWorkflowArtefacts, supportedSeqTypes)
        }

        if (inputArtefactDataList.empty) {
            String msg = "no data found for ${workflow}, skipp"
            log.debug("        ${msg}")
            deciderResult.infos << msg.toString()
            return deciderResult
        }

        ADL additionalArtefactDataList = LogUsedTimeUtils.logUsedTime(log, "        fetch additional Artefacts") {
            fetchAdditionalArtefacts(inputArtefactDataList)
        }

        AD additionalData = LogUsedTimeUtils.logUsedTime(log, "        fetch additional Data") {
            fetchAdditionalData(inputArtefactDataList, w)
        }

        Map<ProjectSeqTypeGroup, WorkflowVersionSelector> workflowVersionSelectorMap =
                LogUsedTimeUtils.logUsedTime(log, "        fetch workflow selectors") {
                    fetchWorkflowVersionSelector(inputArtefactDataList, w).collectEntries {
                        assert !it.project.archived
                        [(new ProjectSeqTypeGroup(it.project, it.seqType)): it]
                    }
                }

        Map<G, ADL> groupedInputData = LogUsedTimeUtils.logUsedTime(log, "        group given Artefacts") {
            groupData(inputArtefactDataList, additionalData, userParams)
        }

        Map<G, ADL> groupedAdditionalData = LogUsedTimeUtils.logUsedTime(log, "        group additional Artefacts") {
            groupData(additionalArtefactDataList, additionalData, userParams)
        }

        Map<ProjectSeqTypeGroup, Map<G, ADL>> groupedPerProjectSeqType =
                LogUsedTimeUtils.logUsedTime(log, "        grouped per project / seqType") {
                    groupedInputData.groupBy {
                        new ProjectSeqTypeGroup(it.key.individual.project, it.key.seqType)
                    }
                }

        LogUsedTimeUtils.logUsedTimeStartEnd(log, "        handle ${groupedPerProjectSeqType.size()} base groups") {
            groupedPerProjectSeqType.each { ProjectSeqTypeGroup baseGroup, Map<G, ADL> groups ->
                LogUsedTimeUtils.logUsedTimeStartEnd(log, "          handle base group ${baseGroup} with ${groups.size()} groups") {
                    WorkflowVersionSelector matchingWorkflows = workflowVersionSelectorMap[baseGroup]
                    if (!matchingWorkflows) {
                        log.debug("            skip, since no workflow version is configured")
                        deciderResult.infos << "${w}: Ignore ${baseGroup}, since no workflow version available".toString()
                        return
                    }

                    groups.each { G group, ADL givenArtefacts ->
                        LogUsedTimeUtils.logUsedTimeStartEnd(log, "            handle group ${group}") {
                            ADL additionalArtefacts = groupedAdditionalData[group]
                            deciderResult.add(createWorkflowRunsAndOutputArtefacts(
                                    baseGroup, group,
                                    givenArtefacts, additionalArtefacts,
                                    additionalData,
                                    matchingWorkflows.workflowVersion))
                        }
                    }
                }
            }
        }
        deciderResult.infos << "end decider for ${w}".toString()
        return deciderResult
    }
}
