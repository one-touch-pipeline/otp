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
import grails.transaction.Rollback
import org.codehaus.groovy.control.io.NullWriter
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class AbstractRoddyJobIntegrationSpec extends Specification {

    static final String RODDY_EXECUTION_STORE_DIRECTORY_NAME = 'exec_150625_102449388_username_analysis'

    // has to be set in setupData because of dependency to AbstractRoddyJob
    static ProcessOutput outputNoClusterJobsSubmitted

    ClusterJobService clusterJobService

    AbstractRoddyJob roddyJob
    RoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService
    int executeCommandCounter
    int validateCounter

    String stderr

    @Rule
    TemporaryFolder tmpDir = new TemporaryFolder()

    void setupData() {
        executeCommandCounter = 0
        validateCounter = 0

        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = roddyBamFile.project.realm

        outputNoClusterJobsSubmitted = new ProcessOutput(
                stderr: "Creating the following execution directory to store information about this process:\n" +
                        "\t${new File(TestCase.uniqueNonExistentPath, RODDY_EXECUTION_STORE_DIRECTORY_NAME)}" +
                        "${RoddyExecutionService.NO_STARTED_JOBS_MESSAGE}",
                stdout: "",
                exitCode: 0,
        )

        roddyJob = [
                getProcessParameterObject: { -> roddyBamFile },
                prepareAndReturnWorkflowSpecificCommand: { Object instance, Realm realm -> return "workflowSpecificCommand" },
                validate: { Object instance -> validateCounter ++ },
                getProcessingStep : { -> return DomainFactory.createAndSaveProcessingStep() },
        ] as AbstractRoddyJob

        roddyJob.roddyExecutionService = new RoddyExecutionService()
        roddyJob.roddyExecutionService.configService = configService
        roddyJob.roddyExecutionService.clusterJobService = new ClusterJobService()
        roddyJob.roddyExecutionService.processingOptionService = new ProcessingOptionService()
        roddyJob.clusterJobSchedulerService = [
                retrieveAndSaveJobInformationAfterJobStarted: { ClusterJob clusterJob -> },
        ] as ClusterJobSchedulerService
        roddyJob.fileSystemService = new TestFileSystemService()
    }

    void cleanup() {
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
        roddyBamFile = null
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
        configService.clean()
    }

    private File setRootPathAndCreateWorkExecutionStoreDirectory() {
        configService.addOtpProperties(tmpDir.newFolder().toPath())
        File workRoddyExecutionDir = new File(roddyBamFile.workExecutionStoreDirectory, RODDY_EXECUTION_STORE_DIRECTORY_NAME)
        assert workRoddyExecutionDir.mkdirs()
        return workRoddyExecutionDir
    }

    private File setUpWorkDirAndMockProcessOutput() {
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        String stdout = "Running job abc_def => 3504988"
        stderr = """newLine
Creating the following execution directory to store information about this process:
${workExecutionDir.absolutePath}
newLine"""

        mockProcessOutput(stdout, stderr)

        return workExecutionDir
    }

    private File setupWorkDirAndMockProcessOutputWithError(String error) {
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        stderr = """newLine
Creating the following execution directory to ${error} store information about this process:
${workExecutionDir.absolutePath}
newLine"""
        mockProcessOutput("", stderr)

        return workExecutionDir
    }

    private void mockProcessOutput(String output, String error) {
        roddyJob.roddyExecutionService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String cmd ->
                    executeCommandCounter++
                    return new ProcessOutput(stdout: output, stderr: error, exitCode: 0)
                }
        ] as RemoteShellHelper
    }

    private void mockProcessOutput_noClusterJobsSubmitted() {
        roddyJob.roddyExecutionService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String cmd ->
                    executeCommandCounter++
                    return outputNoClusterJobsSubmitted
                }
        ] as RemoteShellHelper
    }

    void "test maybeSubmit"() {
        given:
        setupData()
        setUpWorkDirAndMockProcessOutput()

        when:
        NextAction result
        LogThreadLocal.withThreadLog(System.out) {
            result = roddyJob.maybeSubmit()
        }

        then:
        result == NextAction.WAIT_FOR_CLUSTER_JOBS
        executeCommandCounter == 1
        validateCounter == 0
    }

    void "test maybeSubmit, no cluster jobs submitted, validate succeeds"() {
        given:
        setupData()
        mockProcessOutput_noClusterJobsSubmitted()

        when:
        NextAction result
        LogThreadLocal.withThreadLog(new NullWriter()) {
            result = roddyJob.maybeSubmit()
        }

        then:
        result == NextAction.SUCCEED
        executeCommandCounter == 1
        validateCounter == 1
    }

    void "test maybeSubmit, no cluster jobs submitted, validate fails"() {
        given:
        setupData()
        mockProcessOutput_noClusterJobsSubmitted()
        roddyJob.metaClass.validate = { ->
            validateCounter++
            throw new RuntimeException(TestConstants.ARBITRARY_MESSAGE)
        }

        when:
        LogThreadLocal.withThreadLog(new NullWriter()) {
            roddyJob.maybeSubmit()
        }

        then:
        RuntimeException e = thrown(RuntimeException)
        e.message == 'validate() failed after Roddy has not submitted any cluster jobs.'
        e.cause?.message == TestConstants.ARBITRARY_MESSAGE
        executeCommandCounter == 1
    }

    void "test validate"() {
        given:
        setupData()

        when:
        roddyJob.validate()

        then:
        validateCounter == 1
    }

    void "test execute, finishedClusterJobs is null"() {
        given:
        setupData()
        setUpWorkDirAndMockProcessOutput()
        roddyJob.metaClass.maybeSubmit = {
            return NextAction.WAIT_FOR_CLUSTER_JOBS
        }
        roddyJob.metaClass.validate = {
            throw new RuntimeException("should not come here")
        }

        when:
        NextAction result
        LogThreadLocal.withThreadLog(System.out) {
            result = roddyJob.execute(null)
        }

        then:
        result == NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    void "test maybeSubmit, with Roddy errors"() {
        given:
        setupData()
        setupWorkDirAndMockProcessOutputWithError(cause)

        when:
        LogThreadLocal.withThreadLog(System.out) {
            roddyJob.maybeSubmit()
        }

        then:
        RuntimeException e = thrown(RuntimeException)
        e.message == message

        where:
        cause                                             || message
        "java.lang.OutOfMemoryError"                      || "An out of memory error occurred when executing Roddy."
        "An uncaught error occurred during a run. SEVERE" || "An unexpected error occurred when executing Roddy."
    }

    void "test execute"() {
        given:
        setupData()
        roddyJob = [
                validate                      : { -> validateCounter++ },
                failedOrNotFinishedClusterJobs: { Collection<? extends ClusterJobIdentifier> finishedClusterJobs -> [:] },
        ] as AbstractRoddyJob

        Realm realm = DomainFactory.createRealm()
        realm.save(flush: true)

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", configService.sshUser, processingStep)
        assert clusterJob

        final ClusterJobIdentifier jobIdentifier = new ClusterJobIdentifier(realm, clusterJob.clusterJobId)

        roddyJob.metaClass.maybeSubmit = {
            throw new RuntimeException("should not come here")
        }

        when:
        NextAction result = roddyJob.execute([jobIdentifier])

        then:
        result == NextAction.SUCCEED
        executeCommandCounter == 0
        validateCounter == 1
    }
}
