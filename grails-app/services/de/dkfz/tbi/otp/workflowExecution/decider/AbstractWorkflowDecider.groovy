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

import grails.util.Pair

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ActiveProjectWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

/**
 * creates workflow runs for one workflow
 * knows the needed input and created output workflowartefacts
 * knows all requirements
 * is called with a list of new/changed workflow artefacts
 */
abstract class AbstractWorkflowDecider implements Decider {

    /**
     * Filters the provided workflow artefacts to the artefact types used by this decider.
     * WorkflowArtefacts of other types should not appear in the returned list.
    */
    abstract Collection<WorkflowArtefact> filterForNeededArtefacts(Collection<WorkflowArtefact> artefacts)

    /**
     * Search in database for additional required WorkflowArtefacts.
     * That are workflowArtefacts created by an earlier import and therefore not in the input list.
     * Will be implemented in concreate Deciders, since the decider per workflows knows what is needed
     * Make sure that in the search for the other required workflow artefacts no artefacts in state FAILED or SKIPPED or withdrawn artefacts are considered)
    */
    abstract Collection<WorkflowArtefact> findAdditionalRequiredWorkflowArtefacts(Collection<WorkflowArtefact> artefacts)

    /**
     * Group all workflow artefacts based on the fact if they can be processed together within WorkflowRun (e.g. individual, sample type).
     * The inner collection are inputs for one workflow run..
    */
    abstract Collection<Collection<WorkflowArtefact>> groupArtefactsForWorkflowExecution(Collection<WorkflowArtefact> artefacts)

    abstract Workflow getWorkflow()

    /**
    Iterate over the different collections
    Check if all needed input WorkflowArtefacts are available
    The input WorkflowArtefacts need to match all constraints, e.g. MergingCriteria, Bam Artefact for ACEseq needs to be the same used for sophia Artefact
    At least one of the provided artefacts must be „new/changed“ (from the initialArtefacts list)
    Check if no workflowRun exists for the same input WorkflowArtefacts which is not withdrawn.
        If there is no other workflowRun -> true
        If there is already another workflowRun check if the flag forceRun is set
        If this is not the case -> false
        If it is set -> check if the configuration used for the workflowRun was already the one which is currently configured for the project
            If yes -> false
            If no -> true
    Create a workflow run if true.
        Create Workflow run in state PENDING
        Collect also the configurations and store them to the workflowRun
        Create all corresponding output WorkflowArtefacts
    Returns all output WorkflowArtefacts or an empty list
    */
    abstract Collection<WorkflowArtefact> createWorkflowRunIfPossible(Collection<Collection< WorkflowArtefact >> groupedArtefacts,
                                                                      Collection<WorkflowArtefact> initialArtefacts)

    /**
     * Group the artefacts by project and seqtype.
    */
    Map<Pair<Project, SeqType>, Set<WorkflowArtefact>> groupArtefacts(Collection<WorkflowArtefact> toGroup) {
        /**
         * We could have use .collectEntries(), but in this way the compiler can understand the type transformations.
         * (collectEntries() has (?) as return type)
         */
        Map<Pair<Project, SeqType>, Set<WorkflowArtefact>> result = [:]
        toGroup.groupBy { it ->
            new Pair<Project, SeqType>(it.project, it.seqType)
        }.each { Pair <Project, SeqType> k, List<WorkflowArtefact> v ->
            result.put(k, v.toSet())
        }
        return result
    }

    @Override
    Collection<WorkflowArtefact> decide(Collection<WorkflowArtefact> newArtefacts, boolean forceRun = false, Map<String, String> userParams = [:]) {
        Collection<WorkflowArtefact> allWorkflowArtefactsOrEmptyList
        Collection<WorkflowArtefact> filteredArtefacts = filterForNeededArtefacts(newArtefacts)
        Map<Pair<Project, SeqType>, Set<WorkflowArtefact>> groupedArtefacts = groupArtefacts(filteredArtefacts)
        groupedArtefacts.each { it ->
            def matchingWorkflows = ActiveProjectWorkflow.createCriteria().get {
                eq('project', it.project)
                eq('seqType', it.seqType)
                wokflowVersion {
                    eq('workflow', workflow)
                }
                isNull('deprecationDate')
            }
            if (!matchingWorkflows) {
                return []
            }
            Collection<WorkflowArtefact> combinedWorkflowArtefacts = findAdditionalRequiredWorkflowArtefacts(filteredArtefacts)
            Collection<Collection<WorkflowArtefact>> artefactsPerWorkflowRun = groupArtefactsForWorkflowExecution(combinedWorkflowArtefacts)
            allWorkflowArtefactsOrEmptyList += createWorkflowRunIfPossible(artefactsPerWorkflowRun, filteredArtefacts)
        }
        return allWorkflowArtefactsOrEmptyList
    }
}
