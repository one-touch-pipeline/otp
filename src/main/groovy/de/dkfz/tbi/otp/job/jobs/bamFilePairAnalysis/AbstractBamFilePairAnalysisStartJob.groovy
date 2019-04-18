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

package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Slf4j
abstract class AbstractBamFilePairAnalysisStartJob extends AbstractStartJobImpl implements RestartableStartJob, BamFilePairAnalysisStartJobTrait {

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    ConfigService configService

    @Scheduled(fixedDelay = 60000L)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
                return
            }
            SamplePair.withTransaction {
                SamplePair samplePair = findSamplePairToProcess(minPriority)
                if (samplePair) {
                    samplePair.lock()
                    samplePair.refresh()

                    ConfigPerProjectAndSeqType config = getConfig(samplePair)

                    AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
                    AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

                    BamFilePairAnalysis analysis = getBamFileAnalysisService().getAnalysisClass().newInstance(
                            samplePair: samplePair,
                            instanceName: getInstanceName(config),
                            config: config,
                            sampleType1BamFile: sampleType1BamFile,
                            sampleType2BamFile: sampleType2BamFile,
                    )
                    analysis.save(flush: true)
                    prepareCreatingTheProcessAndTriggerTracking(analysis)
                    createProcess(analysis)
                    log.debug "Analysis started for: ${analysis.toString()}"
                }
            }
        }
    }


    @Override
    Process restart(Process process) {
        assert process

        BamFilePairAnalysis failedAnalysis = process.getProcessParameterObject()

        tryToDeleteResultFilesOfFailedInstance(failedAnalysis)

        BamFilePairAnalysis.withTransaction {

            failedAnalysis.withdrawn = true
            assert failedAnalysis.save(flush: true)

            BamFilePairAnalysis newAnalysis = getBamFileAnalysisService().getAnalysisClass().newInstance(
                    samplePair: failedAnalysis.samplePair,
                    instanceName: getInstanceName(failedAnalysis.config),
                    config: failedAnalysis.config,
                    sampleType1BamFile: failedAnalysis.sampleType1BamFile,
                    sampleType2BamFile: failedAnalysis.sampleType2BamFile,
            )
            assert newAnalysis.save(flush: true)

            return createProcess(newAnalysis)
        }
    }


    private void tryToDeleteResultFilesOfFailedInstance(BamFilePairAnalysis analysis) {
        final Realm realm = analysis.project.realm

        String deleteFiles = "rm -rf ${analysis.getWorkDirectory()}"

        remoteShellHelper.executeCommandReturnProcessOutput(realm, deleteFiles)
    }

    SamplePair findSamplePairToProcess(ProcessingPriority minPriority) {
        return getBamFileAnalysisService().samplePairForProcessing(minPriority)
    }

    @Override
    String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm")
        ZonedDateTime time = ZonedDateTime.of(LocalDateTime.now(), configService.getTimeZoneId())
        return time.format(formatter).replaceAll('/', '_')
    }
}
