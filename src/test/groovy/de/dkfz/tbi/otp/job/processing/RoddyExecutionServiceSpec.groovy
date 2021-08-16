/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystems

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class RoddyExecutionServiceSpec extends Specification implements ServiceUnitTest<RoddyExecutionService>, DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
        ]
    }

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
    static final String RANDOM_RODDY_EXECUTION_DIR = "/exec_150707_142149946_USER_WGS"
    static final ProcessOutput OUTPUT_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
            stderr: "",
            stdout: "Running job ${SNV_CALLING_META_SCRIPT_JOB_NAME} => ${SNV_CALLING_META_SCRIPT_PBSID}  \n" +
                    "\n" +
                    "  Running job ${SNV_ANNOTATION_JOB_NAME} => ${SNV_ANNOTATION_PBSID}\n" +
                    "  \n" +
                    "Rerun job ${ALIGN_AND_PAIR_SLIM_JOB_NAME} => ${ALIGN_AND_PAIR_SLIM_PBSID}",
            exitCode: 0,
    )
    static final ProcessOutput OUTPUT_NO_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
            stderr: "Creating the following execution directory to store information about this process:\n" +
                    "\t${new File(TestCase.uniqueNonExistentPath, RODDY_EXECUTION_STORE_DIRECTORY_NAME)}" +
                    "${RoddyExecutionService.NO_STARTED_JOBS_MESSAGE}",
            stdout: "",
            exitCode: 0,
    )

    RoddyExecutionService roddyExecutionService
    RoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService
    int executeCommandCounter
    int validateCounter

    String stderr

    @Rule
    TemporaryFolder tmpDir

    void setupData() {
        executeCommandCounter = 0
        validateCounter = 0

        roddyBamFile = createBamFile()

        realm = roddyBamFile.project.realm
        configService = new TestConfigService()

        roddyExecutionService = new RoddyExecutionService()
        roddyExecutionService.configService = configService
        roddyExecutionService.clusterJobService = new ClusterJobService()
        roddyExecutionService.processingOptionService = new ProcessingOptionService()
        roddyExecutionService.afterPropertiesSet()
    }

    void cleanup() {
        configService.clean()
    }

    void "test execute, success"() {
        given:
        setupData()
        String cmd = "asdfasdf"
        ProcessOutput output = new ProcessOutput("", "", 0)
        roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper)

        when:
        ProcessOutput result = roddyExecutionService.execute(cmd, realm)

        then:
        1 * roddyExecutionService.remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd) >> { output }
        result == output
    }

    void "test execute, with Roddy errors"() {
        given:
        setupData()
        roddyExecutionService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                return new ProcessOutput("", "sadfasdf${cause}asdfsadf", 0)
            }
        }

        when:
        roddyExecutionService.execute("qwertz", realm)

        then:
        RuntimeException e = thrown(RoddyException)
        e.message == message

        where:
        cause                                             || message
        "java.lang.OutOfMemoryError"                      || "An out of memory error occurred when executing Roddy."
        "An uncaught error occurred during a run. SEVERE" || "An unexpected error occurred when executing Roddy."
    }

    void "test createClusterJobObjects"() {
        given:
        setupData()
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()
        WorkflowStep workflowStep = createWorkflowStep()

        roddyBamFile.roddyExecutionDirectoryNames.add(workRoddyExecutionDir.name)

        when:
        Collection<ClusterJob> result = roddyExecutionService.createClusterJobObjects(roddyBamFile, OUTPUT_CLUSTER_JOBS_SUBMITTED, workflowStep)

        then:
        containSame(result, ClusterJob.all)
        ClusterJob.all.find {
            it.clusterJobId == SNV_CALLING_META_SCRIPT_PBSID &&
                    it.clusterJobName == SNV_CALLING_META_SCRIPT_JOB_NAME &&
                    it.jobClass == SNV_CALLING_META_SCRIPT_JOB_CLASS &&
                    it.realm == realm &&
                    !it.validated &&
                    it.workflowStep == workflowStep &&
                    it.processingStep == null &&
                    it.seqType == null &&
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
                    it.workflowStep == workflowStep &&
                    it.processingStep == null &&
                    it.seqType == null &&
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
                    it.workflowStep == workflowStep &&
                    it.processingStep == null &&
                    it.seqType == null &&
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
        Collection<ClusterJob> result = roddyExecutionService.createClusterJobObjects(roddyBamFile, OUTPUT_NO_CLUSTER_JOBS_SUBMITTED, createWorkflowStep())

        then:
        result.empty
        ClusterJob.count() == 0
    }

    void "test createClusterJobObjects, roddyResults is null, should fail"() {
        given:
        setupData()

        when:
        roddyExecutionService.createClusterJobObjects(null, OUTPUT_CLUSTER_JOBS_SUBMITTED, createWorkflowStep())

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("assert roddyResult")
        ClusterJob.all.empty
    }

    void "test saveRoddyExecutionStoreDirectory, roddyResults is null, should fail"() {
        given:
        setupData()

        when:
        roddyExecutionService.saveRoddyExecutionStoreDirectory(null, "", FileSystems.default)

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
        roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr, FileSystems.default)

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
        roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr, FileSystems.default)

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
        roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr, FileSystems.default)

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
        roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr, FileSystems.default)

        then:
        thrown(AssertionError)
    }

    void "test saveRoddyExecutionStoreDirectory"() {
        given:
        setupData()
        setUpWorkDirAndMockProcessOutput()

        when:
        roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr, FileSystems.default)

        then:
        roddyBamFile.roddyExecutionDirectoryNames.last() == RODDY_EXECUTION_STORE_DIRECTORY_NAME
    }

    void "test parseRoddyExecutionStoreDirectoryFromRoddyOutput, when matches once, should return roddyExecutionDirectory"() {
        given:
        setupData()
        String roddyExecutionDir = TestCase.uniqueNonExistentPath.absolutePath + RANDOM_RODDY_EXECUTION_DIR
        String output = """\
            some String
            Creating the following execution directory to store information about this process:
            ${roddyExecutionDir}
            some String""".stripIndent()

        expect:
        roddyExecutionDir == roddyExecutionService.parseRoddyExecutionStoreDirectoryFromRoddyOutput(output, FileSystems.default).toString()
    }

    void "test parseRoddyExecutionStoreDirectoryFromRoddyOutput, when no match, should fail"() {
        given:
        setupData()
        String output = "some wrong String"

        when:
        roddyExecutionService.parseRoddyExecutionStoreDirectoryFromRoddyOutput(output, FileSystems.default)

        then:
        thrown(RoddyException)
    }

    void "test parseRoddyExecutionStoreDirectoryFromRoddyOutput, when matches more than once, should fail"() {
        given:
        setupData()
        String roddyExecutionDir = TestCase.uniqueNonExistentPath.absolutePath + RANDOM_RODDY_EXECUTION_DIR
        String output = """\
            some String
            Creating the following execution directory to store information about this process:
            ${roddyExecutionDir}
            some String
            Creating the following execution directory to store information about this process:
            ${roddyExecutionDir}
            some String""".stripIndent()

        when:
        roddyExecutionService.parseRoddyExecutionStoreDirectoryFromRoddyOutput(output, FileSystems.default)

        then:
        thrown(AssertionError)
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

    private void mockProcessOutput(String output, String error) {
        roddyExecutionService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String cmd ->
                    executeCommandCounter++
                    return new ProcessOutput(stdout: output, stderr: error, exitCode: 0)
                }
        ] as RemoteShellHelper
    }
}
