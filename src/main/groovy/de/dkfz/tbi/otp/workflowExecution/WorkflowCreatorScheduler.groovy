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

import grails.async.Promise
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.SystemUserUtils
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

import static grails.async.Promises.task

@Component
@Slf4j
class WorkflowCreatorScheduler {

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
    SamplePairDeciderService samplePairDeciderService

    @Autowired
    WorkflowSystemService workflowSystemService

    @Scheduled(fixedDelay = 5000L)
    void scheduleCreateWorkflow() {
        if (!workflowSystemService.enabled) {
            return
        }

        FastqImportInstance fastqImportInstance = fastqImportInstanceService.waiting()
        if (!fastqImportInstance) {
            return
        }

        MetaDataFile metaDataFile = metaDataFileService.findByFastqImportInstance(fastqImportInstance)

        fastqImportInstanceService.updateState(fastqImportInstance, FastqImportInstance.WorkflowCreateState.PROCESSING)

        createWorkflowsAsync(metaDataFile)
    }

    /**
     * only protected for testing, should not used outside this class
     */
    protected Promise<Void> createWorkflowsAsync(MetaDataFile metaDataFile) {
        return task {
            createWorkflowsTask(metaDataFile)
        }
    }

    /**
     * only protected for testing, should not used outside this class
     */
    @SuppressWarnings("CatchThrowable")
    protected void createWorkflowsTask(MetaDataFile metaDataFile) {
        try {
            createWorkflowsTransactional(metaDataFile)

            notificationCreator.sendWorkflowCreateSuccessMail(metaDataFile)
        } catch (Throwable throwable) {
            fastqImportInstanceService.updateState(metaDataFile.fastqImportInstance, FastqImportInstance.WorkflowCreateState.FAILED)
            notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, throwable)
            log.debug("  workflow creation for ${metaDataFile.fileName} failed")
        }
    }

    @Transactional
    private void createWorkflowsTransactional(MetaDataFile metaDataFile) {
        MetaDataFile metaDataFileFromDb = MetaDataFile.get(metaDataFile.id)
        Long timeCreateWorkflowRuns = System.currentTimeMillis()
        FastqImportInstance fastqImportInstance = metaDataFileFromDb.fastqImportInstance
        int count = fastqImportInstance.dataFiles.size()

        log.debug("create workflows starts for ${metaDataFileFromDb.fileName} " +
                "(dataFiles: ${count}, ${fastqImportInstanceService.countInstancesInWaitingState()} in queue)")
        log.debug("  create workflow runs started")
        List<WorkflowRun> runs = dataInstallationInitializationService.createWorkflowRuns(fastqImportInstance)
        log.debug("  create workflow runs finished for ${count} datafiles after: ${System.currentTimeMillis() - timeCreateWorkflowRuns}ms")
        Long timeDecider = System.currentTimeMillis()
        log.debug("  decider started")
        Collection<WorkflowArtefact> workflowArtefacts = allDecider.decide(runs.collectMany { it.outputArtefacts*.value }, false)
        log.debug("  decider finished for ${count} datafiles after: ${System.currentTimeMillis() - timeDecider}ms")

        createSamplePairs(workflowArtefacts, count)

        fastqImportInstanceService.updateState(fastqImportInstance, FastqImportInstance.WorkflowCreateState.SUCCESS)
        log.debug("create workflows finishs for ${metaDataFileFromDb.fileName} " +
                "(dataFiles: ${count}, ${fastqImportInstanceService.countInstancesInWaitingState()} in queue)")
    }

    /**
     * creates the sample pairs for the alignments.
     *
     * Needed, as long not all analysis workflows are migrated.
     *
     * @deprecated old workflow system
     */
    @Deprecated
    private void createSamplePairs(Collection<WorkflowArtefact> workflowArtefacts, int count) {
        Long timeSamplePairs = System.currentTimeMillis()
        log.debug("  sample pair creation started")
        List<SeqType> seqTypes = SeqTypeService.allAnalysableSeqTypes
        Collection<AbstractMergingWorkPackage> mergingWorkPackages = workflowArtefacts.findAll {
            it.artefactType == ArtefactType.BAM
        }.collect {
            return (it.artefact.get() as AbstractMergedBamFile).workPackage
        }.findAll {
            it.seqType in seqTypes
        }
        SystemUserUtils.useSystemUser {
            samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
        }
        log.debug("  sample pair creation finished for ${count} datafiles after: ${System.currentTimeMillis() - timeSamplePairs}ms")
    }
}
