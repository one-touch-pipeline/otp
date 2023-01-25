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
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

/**
 * knows all deciders and also the order of the deciders based on the input and output  WorkflowArtefacts
 * can be called during Fastq or BAM import, realignment, …
 * is called with a list of new/changed workflowArtefacts (see method decide in Decider)
 */
@Component
@Transactional
@Slf4j
class AllDecider implements Decider {

    @Autowired
    WorkflowService workflowService

    /** list of Deciders in the correct order */
    List<Class<Decider>> deciders = [
            FastqcDecider,
            PanCancerDecider,
    ]

    @Override
    Collection<WorkflowArtefact> decide(Collection<WorkflowArtefact> allWorkflowArtefacts, boolean forceRun = false, Map<String,
            String> userParams = [:]) {

        Collection<WorkflowArtefact> newWorkflowArtefacts = []
        LogUsedTimeUtils.logUsedTimeStartEnd(log, "    AllDecider for ${allWorkflowArtefacts.size()} workflow artefacts") {
            deciders.each { deciderClass ->
                Decider decider = Holders.grailsApplication.mainContext.getBean(deciderClass)
                Collection<WorkflowArtefact> workflowArtefacts = LogUsedTimeUtils.logUsedTimeStartEnd(log, "      Decider ${deciderClass.simpleName}") {
                    decider.decide(allWorkflowArtefacts, forceRun, userParams)
                }
                allWorkflowArtefacts += workflowArtefacts
                newWorkflowArtefacts += workflowArtefacts
            }
        }
        return newWorkflowArtefacts
    }

    Collection<SeqTrack> findAllSeqTracksInNewWorkflowSystem(Collection<SeqTrack> seqTracks) {
        Set<SeqType> supportedSeqTypes = workflowService.getExactlyOneWorkflow(PanCancerWorkflow.WORKFLOW).supportedSeqTypes
        return deciders.contains(PanCancerDecider) ? seqTracks.findAll {
            supportedSeqTypes.contains(it.seqType)
        } : [] as Collection<SeqTrack>
    }
}
