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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

@Component
@Slf4j
class WorkflowCreaterScheduler {

    @Autowired
    AllDecider allDecider

    @Autowired
    DataInstallationInitializationService dataInstallationInitializationService

    @Autowired
    FastqImportInstanceService fastqImportInstanceService

    @Autowired
    MetaDataFileService metaDataFileService

    @Autowired
    NotificationCreator notificationCreator

    @Autowired
    WorkflowSystemService workflowSystemService

    @Scheduled(fixedDelay = 5000L)
    void scheduleCreateWorkFlow() {
        if (!workflowSystemService.enabled) {
            return
        }

        FastqImportInstance fastqImportInstance = fastqImportInstanceService.waiting()
        if (!fastqImportInstance) {
            return
        }

        MetaDataFile metaDataFile = metaDataFileService.getByFastqImportInstance(fastqImportInstance)

        try {
            fastqImportInstanceService.updateState(fastqImportInstance, FastqImportInstance.WorkFlowTriggerState.PROCESSING)
            createWorkflows(metaDataFile)

            notificationCreator.sendWorkflowCreateSuccessMail(metaDataFile)
        } catch (Throwable throwable) {
            fastqImportInstanceService.updateState(fastqImportInstance, FastqImportInstance.WorkFlowTriggerState.FAILED)
            notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, throwable)
        }
    }

    @Transactional
    private void createWorkflows(MetaDataFile metaDataFile) {
        metaDataFile = MetaDataFile.get(metaDataFile.id)
        Long timeCreateWorkflowRuns = System.currentTimeMillis()
        FastqImportInstance fastqImportInstance = metaDataFile.fastqImportInstance

        log.debug("workflows for ${metaDataFile.fileName} (${FastqImportInstance.countByState(FastqImportInstance.WorkFlowTriggerState.WAITING)} in queue)")
        log.debug("  create workflow runs started")
        List<WorkflowRun> runs = dataInstallationInitializationService.createWorkflowRuns(fastqImportInstance)
        log.debug("  create workflow runs stopped took: ${System.currentTimeMillis() - timeCreateWorkflowRuns}")
        Long timeDecider = System.currentTimeMillis()
        log.debug("  decider started")
        allDecider.decide(runs.collectMany { it.outputArtefacts*.value }, false)
        log.debug("  decider stopped took: ${System.currentTimeMillis() - timeDecider}")

        fastqImportInstanceService.updateState(fastqImportInstance, FastqImportInstance.WorkFlowTriggerState.SUCCESS)
    }
}
