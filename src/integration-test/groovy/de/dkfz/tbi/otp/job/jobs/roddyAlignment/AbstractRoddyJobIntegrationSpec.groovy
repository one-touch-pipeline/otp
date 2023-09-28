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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.codehaus.groovy.control.io.NullWriter
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.nio.file.Path

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

    @Qualifier('TestConfigService')
    TestConfigService configService

    @TempDir
    Path tempDir

    void setupData() {
        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = roddyBamFile.project.realm

        outputNoClusterJobsSubmitted = new ProcessOutput(
                stderr: "Creating the following execution directory to store information about this process:\n" +
                        "\t${new File(TestCase.uniqueNonExistentPath, RODDY_EXECUTION_STORE_DIRECTORY_NAME)}" +
                        "${RoddyExecutionService.NO_STARTED_JOBS_MESSAGE}",
                stdout: "",
                exitCode: 0,
        )

        roddyJob = Spy(AbstractRoddyJob) {
            _ * getProcessParameterObject() >> roddyBamFile
            _ * prepareAndReturnWorkflowSpecificCommand(_, _) >> "workflowSpecificCommand"
            _ * getProcessingStep() >> DomainFactory.createAndSaveProcessingStep()
        }

        configService.addOtpProperty(OtpProperty.SSH_USER, 'user')
        roddyJob.roddyExecutionService = new RoddyExecutionService()
        roddyJob.roddyExecutionService.configService = configService
        roddyJob.roddyExecutionService.clusterJobService = new ClusterJobService()
        roddyJob.roddyExecutionService.processingOptionService = new ProcessingOptionService()
        roddyJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            _ * retrieveAndSaveJobInformationAfterJobStarted(_)
        }
        roddyJob.fileSystemService = new TestFileSystemService()
    }

    void cleanup() {
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
        roddyBamFile = null
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
        configService.clean()
    }

    private File setRootPathAndCreateWorkExecutionStoreDirectory() {
        configService.addOtpProperties(tempDir)
        File workRoddyExecutionDir = new File(roddyBamFile.workExecutionStoreDirectory, RODDY_EXECUTION_STORE_DIRECTORY_NAME)
        assert workRoddyExecutionDir.mkdirs()
        return workRoddyExecutionDir
    }

    void "test maybeSubmit"() {
        given:
        setupData()
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        String stdout = "Running job abc_def => 3504988"
        String stderr = """newLine
Creating the following execution directory to store information about this process:
${workExecutionDir.absolutePath}
newLine"""

        roddyJob.roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper)

        when:
        NextAction result
        LogThreadLocal.withThreadLog(System.out) {
            result = roddyJob.maybeSubmit()
        }

        then:
        result == NextAction.WAIT_FOR_CLUSTER_JOBS

        and:
        1 * roddyJob.roddyExecutionService.remoteShellHelper.executeCommandReturnProcessOutput(_) >>
                new ProcessOutput(stdout: stdout, stderr: stderr, exitCode: 0)
        0 * roddyJob.validate()
        0 * roddyJob.validate(_)
    }

    void "test maybeSubmit, no cluster jobs submitted, validate succeeds"() {
        given:
        setupData()
        roddyJob.roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper)

        when:
        NextAction result
        LogThreadLocal.withThreadLog(new NullWriter()) {
            result = roddyJob.maybeSubmit()
        }

        then:
        result == NextAction.SUCCEED

        and:
        1 * roddyJob.validate() >> _
        1 * roddyJob.roddyExecutionService.remoteShellHelper.executeCommandReturnProcessOutput(_) >> outputNoClusterJobsSubmitted
    }

    @SuppressWarnings("ThrowRuntimeException")
    // ignored: will be removed with the old workflow system
    void "test maybeSubmit, no cluster jobs submitted, validate fails"() {
        given:
        setupData()
        roddyJob.roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper)

        when:
        LogThreadLocal.withThreadLog(new NullWriter()) {
            roddyJob.maybeSubmit()
        }

        then:
        RuntimeException e = thrown(RuntimeException)
        e.message == 'validate() failed after Roddy has not submitted any cluster jobs.'
        e.cause?.message == TestConstants.ARBITRARY_MESSAGE

        and:
        1 * roddyJob.roddyExecutionService.remoteShellHelper.executeCommandReturnProcessOutput(_) >> outputNoClusterJobsSubmitted
        _ * roddyJob.validate() >> { throw new RuntimeException(TestConstants.ARBITRARY_MESSAGE) }
    }

    void "test validate"() {
        given:
        setupData()

        when:
        roddyJob.validate()

        then:
        1 * roddyJob.validate(_) >> _
    }

    void "test execute, finishedClusterJobs is null"() {
        given:
        setupData()
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        String stdout = "Running job abc_def => 3504988"
        String stderr = """newLine
Creating the following execution directory to store information about this process:
${workExecutionDir.absolutePath}
newLine"""

        roddyJob.roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper)

        when:
        NextAction result
        LogThreadLocal.withThreadLog(System.out) {
            result = roddyJob.execute(null)
        }

        then:
        result == NextAction.WAIT_FOR_CLUSTER_JOBS

        and:
        0 * roddyJob.metaClass.validate()
        0 * roddyJob.validate()
        1 * roddyJob.roddyExecutionService.remoteShellHelper.executeCommandReturnProcessOutput(_) >>
                new ProcessOutput(stdout: stdout, stderr: stderr, exitCode: 0)
    }

    void "test maybeSubmit, with Roddy errors"() {
        given:
        setupData()
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()
        String stderr = """newLine
Creating the following execution directory to ${cause} store information about this process:
${workExecutionDir.absolutePath}
newLine"""
        roddyJob.roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper)

        when:
        LogThreadLocal.withThreadLog(System.out) {
            roddyJob.maybeSubmit()
        }

        then:
        RuntimeException e = thrown(RuntimeException)
        e.message == message

        and:
        1 * roddyJob.roddyExecutionService.remoteShellHelper.executeCommandReturnProcessOutput(_) >>
                new ProcessOutput(stdout: stderr, stderr: cause, exitCode: 0)

        where:
        cause                                             || message
        "java.lang.OutOfMemoryError"                      || "An out of memory error occurred when executing Roddy."
        "An uncaught error occurred during a run. SEVERE" || "An unexpected error occurred when executing Roddy."
    }

    void "test execute"() {
        given:
        setupData()

        Realm realm = DomainFactory.createRealm()
        realm.save(flush: true)

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", configService.sshUser, processingStep)
        assert clusterJob

        final ClusterJobIdentifier jobIdentifier = new ClusterJobIdentifier(realm, clusterJob.clusterJobId)

        when:
        NextAction result = roddyJob.execute([jobIdentifier])

        then:
        result == NextAction.SUCCEED

        and:
        0 * roddyJob.metaClass.maybeSubmit()
        0 * roddyJob.maybeSubmit()
        1 * roddyJob.validate() >> _
        _ * roddyJob.failedOrNotFinishedClusterJobs(_) >> { Collection<? extends ClusterJobIdentifier> finishedClusterJobs -> [:] }
    }
}
