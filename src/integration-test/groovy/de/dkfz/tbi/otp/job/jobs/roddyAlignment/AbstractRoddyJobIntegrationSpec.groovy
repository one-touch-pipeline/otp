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
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

@Rollback
@Integration
class AbstractRoddyJobIntegrationSpec extends Specification {

    static final String SNV_CALLING_META_SCRIPT_PBSID = "3504988"
    static final String SNV_CALLING_META_SCRIPT_JOB_NAME = "r150428_104246480_stds_snvCallingMetaScript"
    static final String SNV_CALLING_META_SCRIPT_JOB_CLASS = "snvCallingMetaScript"
    static final String SNV_ANNOTATION_PBSID = "3504989"
    static final String SNV_ANNOTATION_JOB_NAME = "r150428_104246480_stds_snvAnnotation"
    static final String SNV_ANNOTATION_JOB_CLASS = "snvAnnotation"
    static final String ALIGN_AND_PAIR_SLIM_PBSID = "3744601"
    static final String ALIGN_AND_PAIR_SLIM_JOB_NAME = "r150623_153422293_123456_alignAndPairSlim"
    static final String ALIGN_AND_PAIR_SLIM_JOB_CLASS = "alignAndPairSlim"
    static final String RODDY_EXECUTION_STORE_DIRECTORY_NAME = 'exec_150625_102449388_username_analysis'
    static final ProcessOutput OUTPUT_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
            stderr: "",
            stdout: "Running job ${SNV_CALLING_META_SCRIPT_JOB_NAME} => ${SNV_CALLING_META_SCRIPT_PBSID}  \n" +
                    "\n" +
                    "  Running job ${SNV_ANNOTATION_JOB_NAME} => ${SNV_ANNOTATION_PBSID}\n" +
                    "  \n" +
                    "Rerun job ${ALIGN_AND_PAIR_SLIM_JOB_NAME} => ${ALIGN_AND_PAIR_SLIM_PBSID}",
            exitCode: 0,
    )
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
    public TemporaryFolder tmpDir = new TemporaryFolder()

    void setupData() {
        executeCommandCounter = 0
        validateCounter = 0

        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = roddyBamFile.project.realm
        configService = new TestConfigService()

        outputNoClusterJobsSubmitted = new ProcessOutput(
                stderr: "Creating the following execution directory to store information about this process:\n" +
                        "\t${new File(TestCase.uniqueNonExistentPath, RODDY_EXECUTION_STORE_DIRECTORY_NAME)}" +
                        "${AbstractRoddyJob.NO_STARTED_JOBS_MESSAGE}",
                stdout: "",
                exitCode: 0,
        )

        roddyJob = [
                getProcessParameterObject: { -> roddyBamFile },
                prepareAndReturnWorkflowSpecificCommand: { Object instance, Realm realm -> return "workflowSpecificCommand" },
                validate: { Object instance -> validateCounter ++ },
                getProcessingStep : { -> return DomainFactory.createAndSaveProcessingStep() },
        ] as AbstractRoddyJob

        roddyJob.configService = configService
        roddyJob.clusterJobService = new ClusterJobService()
        roddyJob.clusterJobSchedulerService = [
                retrieveAndSaveJobInformationAfterJobStarted: { ClusterJob clusterJob -> },
        ] as ClusterJobSchedulerService
    }

    void cleanup() {
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
        roddyBamFile = null
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
        configService.clean()
    }

    private File setRootPathAndCreateWorkExecutionStoreDirectory() {
        configService.setOtpProperty((OtpProperty.PATH_PROJECT_ROOT), tmpDir.newFolder().path)
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
        roddyJob.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String cmd ->
                    executeCommandCounter++
                    return new ProcessOutput(stdout: output, stderr: error, exitCode: 0)
                }
        ] as RemoteShellHelper
    }

    private void mockProcessOutput_noClusterJobsSubmitted() {
        roddyJob.remoteShellHelper = [
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

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", configService.getSshUser(), processingStep)
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

    void "test createClusterJobObjects"() {
        given:
        setupData()
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(workRoddyExecutionDir.name)

        when:
        Collection<ClusterJob> result = roddyJob.createClusterJobObjects(roddyBamFile, realm, OUTPUT_CLUSTER_JOBS_SUBMITTED)

        then:
        containSame(result, ClusterJob.all)
        ClusterJob.all.find {
            it.clusterJobId == SNV_CALLING_META_SCRIPT_PBSID &&
                    it.clusterJobName == SNV_CALLING_META_SCRIPT_JOB_NAME &&
                    it.jobClass == SNV_CALLING_META_SCRIPT_JOB_CLASS &&
                    it.realm == realm &&
                    !it.validated &&
                    it.processingStep.id != null &&
                    it.seqType.id != null &&
                    it.queued != null &&
                    it.exitStatus == null &&
                    it.exitCode == null &&
                    it.started == null &&
                    it.ended == null &&
                    it.requestedWalltime == null &&
                    it.requestedCores == null &&
                    it.usedCores == null &&
                    it.cpuTime == null &&
                    it.requestedMemory == null &&
                    it.usedMemory == null
        }
        ClusterJob.all.find {
            it.clusterJobId == SNV_ANNOTATION_PBSID &&
                    it.clusterJobName == SNV_ANNOTATION_JOB_NAME &&
                    it.jobClass == SNV_ANNOTATION_JOB_CLASS &&
                    it.realm == realm &&
                    !it.validated &&
                    it.processingStep.id != null &&
                    it.seqType.id != null &&
                    it.queued != null &&
                    it.exitStatus == null &&
                    it.exitCode == null &&
                    it.started == null &&
                    it.ended == null &&
                    it.requestedWalltime == null &&
                    it.requestedCores == null &&
                    it.usedCores == null &&
                    it.cpuTime == null &&
                    it.requestedMemory == null &&
                    it.usedMemory == null
        }
        ClusterJob.all.find {
            it.clusterJobId == ALIGN_AND_PAIR_SLIM_PBSID &&
                    it.clusterJobName == ALIGN_AND_PAIR_SLIM_JOB_NAME &&
                    it.jobClass == ALIGN_AND_PAIR_SLIM_JOB_CLASS &&
                    it.realm == realm &&
                    !it.validated &&
                    it.processingStep.id != null &&
                    it.seqType.id != null &&
                    it.queued != null &&
                    it.exitStatus == null &&
                    it.exitCode == null &&
                    it.started == null &&
                    it.ended == null &&
                    it.requestedWalltime == null &&
                    it.requestedCores == null &&
                    it.usedCores == null &&
                    it.cpuTime == null &&
                    it.requestedMemory == null &&
                    it.usedMemory == null
        }
    }

    void "test createClusterJobObjects, none submitted"() {
        given:
        setupData()

        when:
        Collection<ClusterJob> result = roddyJob.createClusterJobObjects(roddyBamFile, realm, outputNoClusterJobsSubmitted)

        then:
        result.empty
        ClusterJob.count() == 0
    }

    void "test createClusterJobObjects, realm is null, should fail"() {
        given:
        setupData()

        when:
        roddyJob.createClusterJobObjects(roddyBamFile, null, OUTPUT_CLUSTER_JOBS_SUBMITTED)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("assert realm")
        ClusterJob.all.empty
    }

    void "test createClusterJobObjects, roddyResults is null, should fail"() {
        given:
        setupData()

        when:
        roddyJob.createClusterJobObjects(null, realm, OUTPUT_CLUSTER_JOBS_SUBMITTED)

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("assert roddyResult")
        ClusterJob.all.empty
    }

    void "test saveRoddyExecutionStoreDirectory, roddyResults is null, should fail"() {
        given:
        setupData()

        when:
        roddyJob.saveRoddyExecutionStoreDirectory(null, "")

        then:
        thrown(AssertionError)
    }

    void "test saveRoddyExecutionStoreDirectory, parsed execution store directory not equal to expected path, should fail"() {
        given:
        setupData()
        setUpWorkDirAndMockProcessOutput()

        roddyBamFile.metaClass.getWorkExecutionStoreDirectory = {
            return tmpDir.newFolder("Folder")
        }

        when:
        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)

        then:
        thrown(AssertionError)
    }

    void "test saveRoddyExecutionStoreDirectory, execution store directory doesn't exist on filesystem, should fail"() {
        given:
        setupData()
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        workRoddyExecutionDir.delete()

        when:
        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)

        then:
        thrown(AssertionError)
    }

    void "test saveRoddyExecutionStoreDirectory, execution store directory isn't a directory, should fail"() {
        given:
        setupData()
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        workRoddyExecutionDir.delete()
        tmpDir.newFile(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        when:
        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)

        then:
        thrown(AssertionError)
    }

    void "test saveRoddyExecutionStoreDirectory, latest execution store directory isn't last element, should fail"() {
        given:
        setupData()
        setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add("exec_999999_999999999_a_a")
        assert roddyBamFile.save(flush: true)

        when:
        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)

        then:
        thrown(AssertionError)
    }

    void "test saveRoddyExecutionStoreDirectory"() {
        given:
        setupData()
        setUpWorkDirAndMockProcessOutput()

        when:
        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)

        then:
        roddyBamFile.roddyExecutionDirectoryNames.last() == RODDY_EXECUTION_STORE_DIRECTORY_NAME
    }
}
