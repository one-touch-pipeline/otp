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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class AbstractAlignmentStartJobIntegrationSpec extends Specification {

    void "restart creates new Process on new RoddyBamFile"() {
        given:
        RoddyBamFile failedInstance = DomainFactory.createRoddyBamFile(
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum: null,
        )
        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)
        failedInstance.mergingWorkPackage.bamFileInProjectFolder = failedInstance
        failedInstance.mergingWorkPackage.save(flush: true)

        AbstractAlignmentStartJob roddyAlignmentStartJob = new PanCanStartJob()
        roddyAlignmentStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameterSecond ->
                Process processSecond = DomainFactory.createProcess(
                        jobExecutionPlan: failedProcess.jobExecutionPlan
                )
                processParameterSecond.process = processSecond
                assert processParameterSecond.save(flush: true)
                return processSecond
            }
        }

        when:
        Process process
        LogThreadLocal.withThreadLog(System.out) {
            process = roddyAlignmentStartJob.restart(failedProcess)
        }
        RoddyBamFile restartedInstance = (RoddyBamFile) process.processParameterObject

        then:
        RoddyBamFile.list().size() == 2
        assert !failedInstance.mergingWorkPackage.needsProcessing
        restartedInstance.baseBamFile == failedInstance.baseBamFile
        restartedInstance.seqTracks == failedInstance.seqTracks
        restartedInstance.identifier > failedInstance.identifier
    }
}
