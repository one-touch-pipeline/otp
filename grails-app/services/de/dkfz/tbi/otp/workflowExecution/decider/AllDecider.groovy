/*
 * Copyright 2011-2026 The OTP authors
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
import grails.util.Holders
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowService
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.*

/**
 * knows all deciders and also the order of the deciders based on the input and output  WorkflowArtefacts
 * can be called during Fastq or BAM import, realignment, …
 * is called with a list of new/changed workflowArtefacts (see method decide in Decider)
 */
@CompileDynamic
@Component
@Transactional
@Slf4j
class AllDecider implements Decider {

    @Autowired
    WorkflowService workflowService

    @Autowired
    ApplicationContext applicationContext

    /** list of Deciders in the correct order */
    List<Class<? extends Decider>> deciders = [
            FastqcDecider,
            CellRangerDecider,
            PanCancerDecider,
            WgbsDecider,
            RnaAlignmentDecider,
            SnvDecider,
            IndelDecider,
            SophiaDecider,
            AceseqDecider,
    ]

    Class<? extends Decider> getDeciderClassByName(String name) {
        return deciders.find { it.simpleName == name }
    }

    @Override
    DeciderResult decide(Collection<WorkflowArtefact> allWorkflowArtefacts, Map<String, String> userParams = [:],
                         Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [:]) {
        DeciderResult deciderResultAll = new DeciderResult()
        LogUsedTimeUtils.logUsedTimeStartEnd(log, "    AllDecider for ${allWorkflowArtefacts.size()} workflow artefacts") {
            deciders.each { deciderClass ->
                Decider decider = Holders.grailsApplication.mainContext.getBean(deciderClass)
                DeciderResult deciderResult = LogUsedTimeUtils.logUsedTimeStartEnd(log,
                        "      Decider ${deciderClass.simpleName} with ${allWorkflowArtefacts.size()} artefacts") {
                    decider.decide(allWorkflowArtefacts, userParams, deciderAction)
                }
                deciderResultAll.add(deciderResult)
                allWorkflowArtefacts += deciderResult.newArtefacts
            }
        }
        return deciderResultAll
    }

    Collection<SeqTrack> findAlignableSeqTracks(Collection<SeqTrack> seqTracks) {
        Set<SeqType> supportedSeqTypes = workflowService.getSupportedSeqTypesOfVersions([
                PanCancerWorkflow.WORKFLOW,
                WgbsWorkflow.WORKFLOW,
                RnaAlignmentWorkflow.WORKFLOW,
        ].collect {
            workflowService.getExactlyOneWorkflow(it)
        }) as Set<SeqType>
        return seqTracks.findAll {
            supportedSeqTypes.contains(it.seqType)
        }
    }

    /**
     * Returns the names of workflows enabled by AllDecider.
     * Each decider is responsible for its own workflow.
     *
     * @return List of unique workflow names
     */
    Set<String> getEnabledWorkflowNames() {
        return deciders.collectMany {
            applicationContext.getBeansOfType(it).values()*.workflowName
        } as Set<String>
    }

    @Override
    List<DeciderCreateWorkflowAction> getSupportedActions() {
        return DeciderCreateWorkflowAction.values() as List<DeciderCreateWorkflowAction>
    }

    /**
     * Get all deciders with their supported actions for workflow creation
     * @return all deciders with actions as a Map
     */
    Map<Class, List<DeciderCreateWorkflowAction>> getAllDeciderActionsMap() {
        return deciders.collectEntries {
            Decider decider = applicationContext.getBean(it)
            return [(it), decider.supportedActions]
        }
    }
}
