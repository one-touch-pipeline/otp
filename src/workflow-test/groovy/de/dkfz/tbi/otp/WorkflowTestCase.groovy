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
package de.dkfz.tbi.otp

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowTest.AbstractWorkflowSpec

import javax.sql.DataSource
import java.nio.file.FileSystem
import java.time.Duration
import java.util.concurrent.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static de.dkfz.tbi.otp.utils.LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout

/**
 * Base class for work-flow integration test cases.
 *
 * To run the workflow tests the preparation steps described in
 * src/docs/guide/testing.md have to be followed.
 *
 * @Deprecated old workflow system, will replaced by {@link AbstractWorkflowSpec}
 */
@Deprecated
@Slf4j
@Integration
abstract class WorkflowTestCase extends Specification implements UserAndRoles, GroovyScriptAwareTestCase, DomainFactoryCore {

    ErrorLogService errorLogService
    CreateClusterScriptService createClusterScriptService
    RemoteShellHelper remoteShellHelper
    ExecutionHelperService executionHelperService
    DataSource dataSource
    SchedulerService schedulerService
    TestConfigService configService
    FileSystemService fileSystemService
    ProcessingOptionService processingOptionService
    ReferenceGenomeService referenceGenomeService

    LinkFileUtils linkFileUtils
    FileService fileService

    // permissions to be applied to the source test data
    protected final static String TEST_DATA_MODE_DIR = "2750"
    protected final static String TEST_DATA_MODE_FILE = "640"

    final static String CHROMOSOME_NAMES_FILE = 'chromosome-names.txt'

    // The base directory for this test instance ("local root" directory).
    protected File baseDirectory = null

    String testDataDir
    String ftpDir

    Realm realm
    ProcessingPriority processingPriority

    File schemaDump
    Sql sql

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)
    protected boolean startJobRunning = false

    /**
     * This method must return a list of relative paths (as strings) to the workflow scripts
     * that are required for the test.
     */
    abstract List<String> getWorkflowScripts()

    /**
     * A timeout for the workflow execution,
     * if the workflow doesn't finish within the given duration the execute() method fails.
     */
    abstract Duration getTimeout()

    /**
     * This method can be overridden if a workflow script needs some additional setup
     * before it can be loaded.
     */
    //empty callback for subclasses
    @SuppressWarnings('EmptyMethodInAbstractClass')
    protected void setupForLoadingWorkflow() {
        // do nothing by default
    }

    /**
     * This method can be overridden if other job submission options are needed.
     */
    protected String getJobSubmissionOptions() {
        return JsonOutput.toJson([
                (JobSubmissionOption.WALLTIME): Duration.ofMinutes(20).toString(),
                (JobSubmissionOption.MEMORY)  : "5g",
        ])
    }

    void setup() {
        doCleanup()
        SessionUtils.withNewSession {
            setupDirectoriesAndRealm()

            sql = new Sql(dataSource)
            schemaDump = new File(TestCase.createEmptyTestDirectory(), "test-database-dump.sql")
            sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])

            DomainFactory.createAllAlignableSeqTypes()
            createTestProcessingPriority()

            DomainFactory.with {
                createProcessingOptionForNotificationRecipient()
                createProcessingOptionLazy(name: OptionName.OTP_USER_LINUX_GROUP, value: configService.testingGroup)
                createProcessingOptionLazy(name: OptionName.TICKET_SYSTEM_URL, value: "1234")
                createProcessingOptionLazy(name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX, value: "asdf")
                createProcessingOptionLazy(name: OptionName.FILESYSTEM_FASTQ_IMPORT, value: "")
                createProcessingOptionLazy(name: OptionName.FILESYSTEM_BAM_IMPORT, value: "")
                createProcessingOptionLazy(name: OptionName.FILESYSTEM_PROCESSING_USE_REMOTE, value: "true")
                createProcessingOptionLazy(name: OptionName.FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE, value: "true")
                createProcessingOptionLazy(name: OptionName.REALM_DEFAULT_VALUE, value: realm.name)
                createProcessingOptionLazy(name: OptionName.EMAIL_RECIPIENT_ERRORS, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.EMAIL_REPLY_TO, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.EMAIL_SENDER, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.EMAIL_CLUSTER_ADMINISTRATION, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.EMAIL_OTP_MAINTENANCE, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, value: HelperUtils.randomEmail)
                createProcessingOptionLazy(name: OptionName.FILESYSTEM_TIMEOUT, value: 2)
                createProcessingOptionLazy(name: OptionName.CLUSTER_NAME, value: 'CLUSTER NAME')
                createProcessingOptionLazy(name: OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY, value: configService.workflowTestRoddySharedFilesBaseDir)
                createProcessingOptionLazy(name: OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "90")
                createProcessingOptionLazy(name: OptionName.PROCESSING_PRIORITY_DEFAULT_NAME, value: processingPriority.name)
                createProcessingOptionLazy(name: OptionName.EMAIL_MONTHLY_KPI_RECEIVER, value: HelperUtils.randomEmail)
            }

            createUserAndRoles()
            loadWorkflow()

            SpringSecurityUtils.doWithAuth(ADMIN) {
                new File("scripts/initializations").listFiles().findAll {
                    it.isFile()
                }.each { File script ->
                    runScript(script)
                }
            }

            DomainFactory.createProcessingOptionLazy(
                    name: OptionName.RODDY_APPLICATION_INI,
                    value: new File(inputRootDirectory, "applicationProperties-test.ini").absolutePath
            )

            assert schedulerService.running.empty
            assert schedulerService.queue.empty
            assert !ProcessingStep.list()

            // manually set up scheduled tasks
            // this is done here so they will be stopped when each test is finished,
            // otherwise there would be problems with the database being deleted
            scheduler.scheduleWithFixedDelay({
                Holders.applicationContext.getBean(OldClusterJobMonitor).check()
            } as Runnable, 0, 10, TimeUnit.SECONDS)

            scheduler.scheduleWithFixedDelay({
                Holders.applicationContext.getBean(SchedulerService).schedule()
            } as Runnable, 0, 1, TimeUnit.SECONDS)

            scheduler.scheduleWithFixedDelay({
                Holders.applicationContext.getBean(RemoteShellHelper).keepAlive()
            } as Runnable, 0, 60, TimeUnit.SECONDS)

            scheduler.scheduleWithFixedDelay({
                Holders.applicationContext.getBean(FileSystemService).keepAlive()
            } as Runnable, 0, 60, TimeUnit.SECONDS)
        }
    }

    void cleanup() {
        // stop scheduled tasks in SchedulerService and start job
        scheduler.shutdownNow()
        startJobRunning = false
        assert scheduler.awaitTermination(1, TimeUnit.MINUTES)

        doCleanup()

        log.info "Base directory: ${baseDirectory}"
    }

    void doCleanup() {
        SessionUtils.withNewSession {
            JobExecutionPlan.list()*.startJob*.bean.each {
                ((AbstractStartJobImpl) Holders.applicationContext.getBean(it)).onApplicationEvent(null)
            }
        }

        schedulerService.running.clear()
        schedulerService.queue.clear()

        fileSystemService.createdFileSystems.each { Realm realm, FileSystem fileSystem ->
            fileSystem.close()
        }
        remoteShellHelper.sessionPerRealm.each { Realm realm, com.jcraft.jsch.Session session ->
            session.disconnect()
        }
        remoteShellHelper.sessionPerRealm = [:]
        fileSystemService.createdFileSystems = [:]

        if (sql) {
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])
        }
        TestCase.cleanTestDirectory()
    }

    protected void setupDirectoriesAndRealm() {
        // check whether the wf test root dir is mounted
        // (assume it is mounted if it exists and contains files)
        File rootDirectory = inputRootDirectory
        assert rootDirectory.list()?.size(): "${rootDirectory} seems not to be mounted"

        configService.setOtpProperty((OtpProperty.SSH_USER), configService.workflowTestAccountName)

        Map realmParams = [
                name                       : 'REALM_NAME',
                jobScheduler               : configService.workflowTestScheduler,
                host                       : configService.workflowTestHost,
                port                       : 22,
                timeout                    : 0,
                defaultJobSubmissionOptions: jobSubmissionOptions,
        ]

        realm = Realm.list().find() ?: DomainFactory.createRealm(realmParams)

        setupBaseDirectory()

        log.debug "Base directory: ${baseDirectory}"
        [
                (OtpProperty.PATH_PROJECT_ROOT)    : "${baseDirectory}/root_path",
                (OtpProperty.PATH_PROCESSING_ROOT) : "${baseDirectory}/processing_root_path",
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): "${baseDirectory}/logging_root_path",
        ].each { key, value ->
            configService.setOtpProperty(key, value)
        }

        createDirectories([
                configService.rootPath,
                new File(configService.loggingRootPath, JobStatusLoggingService.STATUS_LOGGING_BASE_DIR),
        ])
        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(configService.processingRootPath, "reference_genomes").absolutePath)

        testDataDir = "${inputRootDirectory}/files"
        ftpDir = "${baseDirectory}/ftp"
    }

    private void createTestProcessingPriority() {
        processingPriority = createProcessingPriority([
                priority         : 0,
                name             : 'workflow test',
                errorMailPrefix  : '',
                queue            : configService.workflowTestQueue,
                roddyConfigSuffix: configService.workflowTestConfigSuffix,
        ])
    }

    void updateProcessingPriorityToFastrack() {
        processingPriority.with {
            queue = configService.workflowTestFasttrackQueue
            roddyConfigSuffix = configService.workflowTestFasttrackConfigSuffix
            save(flush: true)
        }
    }

    void createDirectories(List<File> files) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
        files.each {
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(fileSystem.getPath(it.toString()), realm, '',
                    FileService.DEFAULT_DIRECTORY_PERMISSION_STRING)
        }
    }

    void createFilesWithContent(Map<File, String> files) {
        createDirectories(files.keySet()*.parentFile.unique())
        String cmd = files.collect { File key, String value ->
            "echo '${value}' > ${key}"
        }.join('\n')
        remoteShellHelper.executeCommand(realm, cmd)
        files.each { File key, String value ->
            FileService.waitUntilExists(key.toPath())
            assert key.text == value + '\n'
        }
    }

    protected void waitUntilWorkflowFinishes(Duration timeout, int numberOfProcesses = 1) {
        log.debug "Started to wait (until workflow is finished or timeout)"
        long lastLog = 0L
        int counter = 0
        if (!ThreadUtils.waitFor({
            if (lastLog < System.currentTimeMillis() - 60000L) {
                log.debug "waiting (${counter++} min) ... "
                lastLog = System.currentTimeMillis()
            }
            return areAllProcessesFinished(numberOfProcesses)
        }, timeout.toMillis(), 1000L)) {
            throw new TimeoutException("Workflow did not finish within ${timeout.toString().substring(2)}.")
        }
    }

    // ignored: will be removed with the old workflow system
    @SuppressWarnings('ThrowRuntimeException')
    protected void outputFailureInfoAndThrowException(Collection<ProcessingStepUpdate> failureProcessingStepUpdates) {
        List<String> combinedErrorMessage = []
        failureProcessingStepUpdates.each {
            log.error "ProcessingStep ${it.processingStep.id} failed."
            if (it.error) {
                log.error 'Error message:'
                log.error it.error.errorMessage
                combinedErrorMessage << it.error.errorMessage
                if (it.error.stackTraceIdentifier) {
                    log.error 'Stack trace:'
                    log.error errorLogService.loggedError(it.error.stackTraceIdentifier)
                } else {
                    log.error 'The stackTraceIdentifier property of the ProcessingError is not set.'
                }
            } else {
                log.error 'The error property of the ProcessingStepUpdate is not set.'
            }
        }
        if (!failureProcessingStepUpdates.empty) {
            throw new OtpRuntimeException("""\
                |There were ${failureProcessingStepUpdates.size()} failures:
                |${combinedErrorMessage.join("\n")}
                |Details have been written to standard output. See the test report or run grails test-app -echoOut.
                |""".stripMargin())
        }
    }

    protected void ensureThatWorkflowHasNotFailed(List<ProcessingStepUpdate> existingFailureProcessingStepUpdate = []) {
        Collection<ProcessingStepUpdate> allFailureProcessingStepUpdates = ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE)
        Collection<ProcessingStepUpdate> failureProcessingStepUpdatesAfterRestart = allFailureProcessingStepUpdates - existingFailureProcessingStepUpdate
        outputFailureInfoAndThrowException(failureProcessingStepUpdatesAfterRestart)

        checkForFailedClusterJobs()
    }

    void checkForFailedClusterJobs() {
        assert ClusterJob.all.every { it.exitStatus != null }
        assert ClusterJob.all.every { it.jobLog != null }
    }

    protected static boolean areAllProcessesFinished(int numberOfProcesses) {
        Collection<Process> processes = Process.list()
        assert processes.size() <= numberOfProcesses
        return processes.size() == numberOfProcesses && processes.every {
            it.refresh(); it.finished
        }
    }

    /**
     * The base directory for this test instance. It can be considered as "local root", meaning all files
     * and directories should be created under this directory.
     *
     * @return the base directory
     */
    protected void setupBaseDirectory() {
        // Create the directory like a "singleton", since randomness is involved
        if (!baseDirectory) {
            String mkDirs = """\
TEMP_DIR=`mktemp -d -p ${resultRootDirectory.absolutePath} ${this.class.simpleName[0..-6]}-${System.getProperty('user.name')}-${
                HelperUtils.formatter.print(new org.joda.time.DateTime())
            }-XXXXXXXXXXXXXXXX`
chmod g+rwx \$TEMP_DIR
echo \$TEMP_DIR
"""
            baseDirectory = new File(remoteShellHelper.executeCommandReturnProcessOutput(realm, mkDirs)
                    .assertExitCodeZeroAndStderrEmpty().stdout.trim())
        }
    }

    protected File getReferenceGenomeDirectory() {
        return new File(inputRootDirectory, 'reference-genomes')
    }

    ReferenceGenome createReferenceGenomeWithFile(String referenceGenomeSpecificPath, String fileNamePrefix, String cytosinePositionsIndex = null) {
        File sourceDir = new File(referenceGenomeDirectory, referenceGenomeSpecificPath)
        File source = new File(sourceDir, CHROMOSOME_NAMES_FILE)

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome(
                path: referenceGenomeSpecificPath,
                fileNamePrefix: fileNamePrefix,
                cytosinePositionsIndex: cytosinePositionsIndex,
                chromosomeLengthFilePath: 'hg19_chrTotalLength.tsv',
                chromosomeSuffix: '',
                chromosomePrefix: '',
        )
        FileService.ensureFileIsReadableAndNotEmpty(source.toPath())

        ["21", "22"].each { String chromosomeName ->
            DomainFactory.createReferenceGenomeEntry(
                    referenceGenome: referenceGenome,
                    classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
                    name: chromosomeName,
                    alias: chromosomeName,
            )
        }

        linkFileUtils.createAndValidateLinks(
                [(sourceDir): referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)],
                realm
        )

        return referenceGenome
    }

    ReferenceGenome createAndSetup_Bwa06_1K_ReferenceGenome() {
        return createReferenceGenomeWithFile('bwa06_1KGRef', 'hs37d5')
    }

    /**
     * The input root directory for all tests. This can be overwritten by the key <code>OtpProperty#TEST_WORKFLOW_INPUT</code>
     * in the configuration file.
     *
     * @return the root directory set in the configuration file, or the default location otherwise.
     *
     * @see #getBaseDirectory()
     */
    protected File getInputRootDirectory() {
        return configService.workflowTestInputRootDir
    }

    /**
     * The result root directory for all tests. This can be overwritten by the key <code>OtpProperty#TEST_WORKFLOW_RESULT</code>
     * in the configuration file. This method should not be called directly; check out {@link #getBaseDirectory()}
     * first.
     *
     * @return the root directory set in the configuration file, or the default location otherwise.
     */
    protected File getResultRootDirectory() {
        return configService.workflowTestResultRootDir
    }

    /**
     *  Load the workflow and update processing options
     */
    protected void loadWorkflow() {
        setupForLoadingWorkflow()

        assert workflowScripts: 'No workflow script provided.'
        SpringSecurityUtils.doWithAuth(ADMIN) {
            JobExecutionPlan.withTransaction {
                workflowScripts.each { String script ->
                    runScript(script)
                }
            }
        }

        ProcessingOption.findAllByName(OptionName.CLUSTER_SUBMISSIONS_OPTION).each {
            it.value = jobSubmissionOptions
            it.save(flush: true)
        }
    }

    /**
     *  find the start job and execute it, then wait for
     *  either the workflow to finish or the timeout
     */
    @SuppressWarnings("CatchThrowable")
    protected void execute(int numberOfProcesses = 1, boolean ensureNoFailure = true) {
        SessionUtils.withNewSession {
            updateProjectValuesForTestRunning()
            schedulerService.startup()
            assert schedulerService.startupOk
            assert schedulerService.active
            if (!startJobRunning) {
                AbstractStartJobImpl startJob = Holders.applicationContext.getBean(JobExecutionPlan.list()?.first()?.startJob?.bean, AbstractStartJobImpl)
                assert startJob: 'No start job found.'

                scheduler.scheduleWithFixedDelay({
                    try {
                        startJob.execute()
                    } catch (Throwable t) {
                        log.error 'Exception in StartJob', t
                        throw t
                    }
                } as Runnable, 0, 5, TimeUnit.SECONDS)
                startJobRunning = true
            }

            waitUntilWorkflowFinishes(timeout, numberOfProcesses)
            if (ensureNoFailure) {
                ensureThatWorkflowHasNotFailed()
            }
        }
    }

    private void updateProjectValuesForTestRunning() {
        String unixGroup = configService.workflowProjectUnixGroup
        Project.list().each {
            it.unixGroup = unixGroup
            it.realm = realm
            it.processingPriority = processingPriority
            it.save(flush: true)
        }
    }

    protected void restartWorkflowFromFailedStep(boolean ensureNoFailure = true) {
        ProcessingStepUpdate failureStepUpdate = exactlyOneElement(ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE))
        ProcessingStep step = failureStepUpdate.processingStep
        schedulerService.restartProcessingStep(step)
        log.debug "RESTARTED THE WORKFLOW FROM THE FAILED JOB"
        waitUntilWorkflowFinishes(timeout)
        if (ensureNoFailure) {
            ensureThatWorkflowHasNotFailed([failureStepUpdate])
        }
    }

    String calculateMd5Sum(File file) {
        assert file: 'file is null'
        FileService.waitUntilExists(file.toPath())
        String md5sum
        LogThreadLocal.withThreadLog(System.out) {
            md5sum = executeAndAssertExitCodeAndErrorOutAndReturnStdout("md5sum ${file}").split(' ')[0]
        }
        assert md5sum ==~ /^[a-f0-9]{32}$/
        return md5sum
    }

    protected void setPermissionsRecursive(File directory, String modeDir, String modeFile) {
        assert directory.absolutePath.startsWith(baseDirectory.absolutePath)
        String cmd = "find -L ${directory} -user ${configService.workflowTestAccountName} -type d -not -perm ${modeDir} -exec chmod ${modeDir} '{}' \\; 2>&1"
        assert remoteShellHelper.executeCommand(realm, cmd).empty
        cmd = "find -L ${directory} -user ${configService.workflowTestAccountName} -type f -not -perm ${modeFile} -exec chmod ${modeFile} '{}' \\; 2>&1"
        assert remoteShellHelper.executeCommand(realm, cmd).empty
    }
}
