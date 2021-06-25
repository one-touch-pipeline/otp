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
package de.dkfz.tbi.otp.workflowTest

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Specification

import de.dkfz.roddy.BEException
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog

import javax.sql.DataSource
import java.nio.file.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.*

/**
 * Base class for workflow integration test.
 *
 * It provides following:
 * - setup database
 * - setup files system
 * - save and restore the database schema to remove all created objects
 * - show overview after the test:
 *   - about all {@link WorkflowRun}s, {@link WorkflowStep}s (jobs), {@link WorkflowLog}s, {@link ClusterJob}s and {@link WorkflowError}
 *   - path tree of the workingDirectory
 * - start/stop the workflow system
 * - start/stop needed cron jobs
 * - stops test, if a cron job fails
 * - monitor workflows to start in given time
 * - monitor workflows to finish in given time
 * - check, that workflow ends successfully
 */
@Slf4j
@Integration
abstract class AbstractWorkflowSpec extends Specification implements UserAndRoles, GroovyScriptAwareTestCase, WorkflowSystemDomainFactory, UserDomainFactory {

    /**
     * state considered as running or not started
     */
    static private final List<WorkflowRun.State> RUNNING_AND_WAITING_STATES = [
            WorkflowRun.State.PENDING,
            WorkflowRun.State.RUNNING_OTP,
            WorkflowRun.State.RUNNING_WES,
    ].asImmutable()

    ClusterJobManagerFactoryService clusterJobManagerFactoryService
    DataSource dataSource
    FileSystemService fileSystemService
    FileService fileService
    RemoteShellHelper remoteShellHelper
    TestConfigService configService
    WorkflowSystemService workflowSystemService

    /**
     * Needed to save the place of the database dump afterwards.
     */
    @Rule
    TemporaryFolder temporaryFolder

    /**
     * Allow the access of the current test method name
     */
    @Rule
    TestName methodName

    /**
     * holds the remote file system
     */
    protected FileSystem remoteFileSystem

    /**
     * the base directory for the current tests
     */
    protected Path inputDataDirectory

    /**
     * the base directory for the current tests
     */
    protected Path workflowResultDirectory

    /**
     * the directory for reference genomes
     */
    protected Path referenceGenomeDirectory

    /**
     * A directory where additional data needed for the test can be placed
     */
    protected Path additionalDataDirectory

    /**
     * the realm used for the tests
     */
    protected Realm realm

    /**
     * the priority used for the workflows
     */
    protected ProcessingPriority processingPriority

    /**
     * The file holding the dump for restore the database afterwards
     */
    protected File schemaDump

    /**
     * Access for the database
     */
    protected Sql sql

    /**
     * Holds the first exception occurred in a  {@link TestCronJob#execute()}.
     * Putting it here will stop the scheduler, the workflow system is halted and the test fail.
     */
    protected OtpRuntimeException exceptionInScheduler

    /**
     * The scheduler used for the workflow system.
     */
    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)

    /**
     * A timeout for the workflow execution,
     * If the workflow doesn't finish within the given duration the execute() method fails.
     */
    abstract Duration getRunningTimeout()

    /**
     * should return the name of the workflow
     */
    abstract String getWorkflowName()

    /**
     * should return {@link OtpWorkflow} class
     */
    abstract Class<? extends OtpWorkflow> getWorkflowComponentClass()

    /**
     * the timeout for waiting to start workflows.
     * Subclass may override it.
     */
    protected Duration getStartWorkflowTimeout() {
        return Duration.ofMinutes(1)
    }

    /**
     * This method can be overridden if other job submission options are needed.
     */
    protected String getJobSubmissionOptions() {
        return JsonOutput.toJson([
                (JobSubmissionOption.WALLTIME): Duration.ofMinutes(15).toString(),
                (JobSubmissionOption.MEMORY)  : "5g",
        ])
    }

    /**
     * Returns the applicationProperties path
     */
    protected Path getRoddyApplicationPropertyFile() {
        return inputDataDirectory.resolve("applicationProperties-test.ini")
    }

    @SuppressWarnings("CatchThrowable")
    void setup() {
        log.debug("Start to setup ${getClass().simpleName}.${methodName.methodName}")
        SessionUtils.withNewSession {
            try {
                sql = new Sql(dataSource)
                schemaDump = new File(temporaryFolder.newFolder(), "test-database-dump.sql")
                sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])
                log.debug("database dump written to ${schemaDump}")

                preCheck()

                createWorkflowObject()
                createProcessingPriorityObject()
                createRealmObject()
                initFileSystem()
                initProcessingOption()

                DomainFactory.createAllAlignableSeqTypes()
                createUserAndRoles()

                loadInitialisationScripts()
                initScheduler()

                log.debug("Finish setting up base test setup, workflow depending setup will follow")
            } catch (Throwable t) {
                //exception in setup was not reported with stacktrace, therefore add own logging
                log.error("error during initialisation", t)
                throw t
            }
        }
    }

    @SuppressWarnings("CatchThrowable")
    void cleanup() {
        try {
            log.info "Starting cleanup after test '${getClass().simpleName}.${methodName.methodName}' for base directory: ${workflowResultDirectory}"

            workflowSystemService.stopWorkflowSystem()

            // stop scheduled tasks in SchedulerService
            log.debug("shutting done the spring scheduler")
            scheduler.shutdownNow()
            assert scheduler.awaitTermination(2, TimeUnit.MINUTES)
            log.debug("spring scheduler has terminated")

            killRemainingClusterJobs()

            showState()

            log.debug("closing remote file system")
            fileSystemService.closeFileSystem()
            remoteShellHelper.closeFileSystem()
            log.debug("remote file system are closed")

            log.debug("reset database")
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])

            log.info "Finish test '${getClass().simpleName}.${methodName.methodName}' using base directory: ${workflowResultDirectory}"
        } catch (Throwable t) {
            //exception in cleanup was not reported with stacktrace, therefore add own logging
            log.error("cleanup error", t)
            throw t
        }
    }

    private void killRemainingClusterJobs() {
        SessionUtils.withNewSession {
            List<ClusterJob> clusterJobs = ClusterJob.findAllByCheckStatusNotEqual(ClusterJob.CheckStatus.FINISHED)
            if (clusterJobs) {
                log.debug("prepare to kill ${clusterJobs.size()} still running cluster jobs: ${clusterJobs*.clusterJobId}")

                BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)
                List<BEJob> beJobs = clusterJobs.collect { ClusterJob clusterJob ->
                    BEJobID beJobId = new BEJobID(clusterJob.clusterJobId)
                    BEJob beJob = new BEJob(beJobId, jobManager)
                    beJob.runResult = new BEJobResult(null, beJob, null, null, null, null)
                    return beJob
                }

                try {
                    jobManager.killJobs(beJobs)
                    log.debug("killed ${clusterJobs.size()} cluster jobs: ${clusterJobs*.clusterJobId}")
                } catch (BEException e) {
                    log.debug("Fail to kill ${clusterJobs.size()} cluster jobs")
                }
            } else {
                log.debug("All Cluster jobs are finished")
            }
        }
    }

    /**
     * show some data of the workflow system, which could help to identify the problem for failed workflows.
     */
    private void showState() {
        List<CharSequence> logEntries = []
        SessionUtils.withNewSession {
            logEntries << "State overview of workflowRuns, workflowSteps and workflowLogs"
            logEntries << ""
            List<WorkflowRun> workflowRuns = WorkflowRun.list().sort {
                it.id
            }
            logEntries << "There are ${workflowRuns.size()} WorkflowRuns"
            workflowRuns.eachWithIndex { WorkflowRun workflowRun, int runIndex ->
                List<WorkflowStep> workflowSteps = workflowRun.workflowSteps
                logEntries << "- run ${runIndex}:"
                [
                        id                          : workflowRun.id,
                        state                       : workflowRun.state,
                        "workflow name"             : workflowRun.workflow.name,
                        "workflow run name"         : workflowRun.displayName,
                        "priority name"             : workflowRun.priority.name,
                        "priority queue"            : workflowRun.priority.queue,
                        "priority roddyConfigSuffix": workflowRun.priority.roddyConfigSuffix,
                        "job count"                 : workflowSteps.size(),
                ].each { key, value ->
                    logEntries << "  - ${key}: ${value}"
                }

                workflowSteps.eachWithIndex { WorkflowStep workflowStep, int stepIndex ->
                    List<WorkflowLog> workflowLogs = workflowStep.logs
                    WorkflowError workflowError = workflowStep.workflowError
                    List<ClusterJob> clusterJobs = workflowStep.clusterJobs.sort { it.id }

                    logEntries << "  - job ${runIndex}.${stepIndex}:"
                    [
                            id                     : workflowStep.id,
                            state                  : workflowStep.state,
                            "spring bean name"     : workflowStep.beanName,
                            "count of logs"        : workflowLogs.size(),
                            "has error"            : workflowError as boolean,
                            "count of cluster logs": clusterJobs.size(),
                    ].each { key, value ->
                        logEntries << "    - ${key}: ${value}"
                    }
                    workflowLogs.eachWithIndex { WorkflowLog workflowLog, int logIndex ->
                        logEntries << "    - log ${runIndex}.${stepIndex}.${logIndex}: ${prefixForOutput(workflowLog.displayLog())}"
                    }
                    if (workflowError) {
                        logEntries << "    - error message: ${prefixForOutput(workflowError.message)}"
                        if (workflowError.stacktrace) {
                            logEntries << "      - stacktrace:"
                        }
                        logEntries << "        ${prefixForOutput(workflowError.stacktrace)}"
                    }
                    clusterJobs.eachWithIndex { ClusterJob clusterJob, int clusterLogIndex ->
                        logEntries << "    - cluster log ${runIndex}.${stepIndex}.${clusterLogIndex}:"
                        [
                                id            : clusterJob.id,
                                clusterId     : clusterJob.clusterJobId,
                                checkStatus   : clusterJob.checkStatus,
                                exitStatus    : clusterJob.exitStatus,
                                exitCode      : clusterJob.exitCode,
                                jobClass      : clusterJob.jobClass,
                                clusterJobName: clusterJob.clusterJobName,
                                jobLog        : clusterJob.jobLog,
                        ].each { key, value ->
                            logEntries << "      - ${key}: ${value}"
                        }
                    }
                }
            }
            logEntries << "\nTree of the file structure"
            if (workflowResultDirectory && Files.exists(workflowResultDirectory)) {
                logEntries << LocalShellHelper.executeAndWait("tree -ugp ${workflowResultDirectory}").assertExitCodeZero().stdout
            } else {
                logEntries << "workflowResultDirectory doesn't exist yet"
            }

            log.debug(logEntries.join('\n'))
        }
    }

    /**
     * Helper to indent multiline string
     */
    private String prefixForOutput(String value) {
        String prefix = " " * 12
        return value.replaceAll('\n', "\n${prefix}")
    }

    /**
     * Ensures that nothing left from last run.
     *
     * That are:
     * - no objects for: Workflow, ExternalWorkflowConfigFragment, ProcessingPriority, ClusterJob
     * - no cached remote file system exist
     * - workflow system do not run
     */
    private void preCheck() {
        log.debug("checking that database is empty")
        assert !ProcessingPriority.list()
        assert !Workflow.list()
        assert !ExternalWorkflowConfigFragment.list()
        assert !ClusterJob.list()

        log.debug("checking that no remote file system exist")
        assert !fileSystemService.hasRemoteFileSystems()
        assert !remoteShellHelper.hasRemoteFileSystems()

        log.debug("checking that workflowSystem is stopped")
        assert !workflowSystemService.enabled
    }

    /**
     * create processingPriority and set it to default
     */
    private void createWorkflowObject() {
        createWorkflow([
                name    : workflowName,
                beanName: workflowComponentClass.simpleName.uncapitalize(),
                enabled : true,
        ])
    }

    /**
     * create processingPriority and set it to default
     */
    private void createProcessingPriorityObject() {
        log.debug("creating processingPriority and set it to default")
        processingPriority = createProcessingPriority([
                priority                   : 0,
                name                       : 'workflow test',
                errorMailPrefix            : '',
                queue                      : configService.workflowTestQueue,
                roddyConfigSuffix          : configService.workflowTestConfigSuffix,
                allowedParallelWorkflowRuns: 1000,
        ])
        DomainFactory.createProcessingOptionLazy(name: OptionName.PROCESSING_PRIORITY_DEFAULT_NAME, value: processingPriority.name)
    }

    /**
     * create a realm for the tests and set it to default
     */
    private void createRealmObject() {
        log.debug("creating realm and set it to default")
        realm = createRealm([
                name                       : 'WorkflowTest',
                jobScheduler               : configService.workflowTestScheduler,
                host                       : configService.workflowTestHost,
                port                       : 22,
                timeout                    : 0,
                defaultJobSubmissionOptions: jobSubmissionOptions,
        ])
        DomainFactory.createProcessingOptionLazy(name: OptionName.REALM_DEFAULT_VALUE, value: realm.name)
    }

    /**
     * Initialized file system depending properties.
     * That includes:
     * - {@link TestConfigService}
     * - {@link #remoteFileSystem}
     * - diverse directories
     *   - {@link #inputDataDirectory}
     *   - {@link #workflowResultDirectory}
     *   - {@link #referenceGenomeDirectory}
     *   - {@link #additionalDataDirectory}
     *
     * Also the {@link #workflowResultDirectory} is created.
     *
     * It requires a valid realm {@link #createRealmObject()}.
     */
    private void initFileSystem() {
        log.debug("initializing fileSystem and depending options")
        configService.setOtpProperty((OtpProperty.SSH_USER), configService.workflowTestAccountName)

        remoteFileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

        String workDirectory = [
                System.getProperty('user.name'),
                this.class.simpleName,
                methodName.methodName.replaceAll('[^a-zA-Z\\-_]', '-'),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd-HH-mm-ss-SSS', Locale.ENGLISH)),
                (Math.abs(HelperUtils.random.nextLong()) % 1000000).toString().padLeft(6, '0'),
        ].join('_')

        inputDataDirectory = remoteFileSystem.getPath(configService.workflowTestInputRootDir.absolutePath)
        workflowResultDirectory = remoteFileSystem.getPath(configService.workflowTestResultRootDir.absolutePath).resolve(workDirectory)
        referenceGenomeDirectory = workflowResultDirectory.resolve("reference-genomes")
        additionalDataDirectory = workflowResultDirectory.resolve('additional-data')

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workflowResultDirectory, realm)

        [
                (OtpProperty.PATH_PROJECT_ROOT)    : "${workflowResultDirectory}/projectPath",
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): "${workflowResultDirectory}/loggingPath",
        ].each { key, value ->
            configService.setOtpProperty(key, value)
        }

        log.debug "Base directory: ${workflowResultDirectory}"
    }

    /**
     * initialized needed processing option.
     *
     * It depends on the initialisation of:
     * - {@link #initFileSystem()}
     * - {@link #createProcessingPriorityObject()}
     * - {@link #createRealmObject()}
     */
    private void initProcessingOption() {
        log.debug("creating processingOptions")
        //emails addresses
        findOrCreateProcessingOption(name: OptionName.EMAIL_CLUSTER_ADMINISTRATION, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_OTP_MAINTENANCE, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_RECIPIENT_ERRORS, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_RECIPIENT_NOTIFICATION, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_REPLY_TO, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_SENDER, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, value: HelperUtils.randomEmail)

        //roddy and other paths
        findOrCreateProcessingOption(name: OptionName.RODDY_APPLICATION_INI, value: roddyApplicationPropertyFile.toString())
        findOrCreateProcessingOption(name: OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY, value: configService.workflowTestRoddySharedFilesBaseDir)
        findOrCreateProcessingOption(name: OptionName.BASE_PATH_REFERENCE_GENOME, value: referenceGenomeDirectory.toString())

        //cluster and file system
        findOrCreateProcessingOption(name: OptionName.CLUSTER_NAME, value: 'CLUSTER NAME')
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_BAM_IMPORT, value: realm.name)
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_FASTQ_IMPORT, value: realm.name)
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE, value: "true")
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_PROCESSING_USE_REMOTE, value: "true")
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_TIMEOUT, value: 2)
        findOrCreateProcessingOption(name: OptionName.OTP_SYSTEM_USER, value: createUser().username)
        findOrCreateProcessingOption(name: OptionName.OTP_USER_LINUX_GROUP, value: configService.testingGroup)
        findOrCreateProcessingOption(name: OptionName.WITHDRAWN_UNIX_GROUP, value: configService.testingGroup)
        findOrCreateProcessingOption(name: OptionName.PROCESSING_PRIORITY_DEFAULT_NAME, value: processingPriority.name)

        //other values
        findOrCreateProcessingOption(name: OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "90")
        findOrCreateProcessingOption(name: OptionName.TICKET_SYSTEM_URL, value: "url ${nextId}")
        findOrCreateProcessingOption(name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX, value: "prefix${nextId}")
        findOrCreateProcessingOption(name: OptionName.EMAIL_MONTHLY_KPI_RECEIVER, value: HelperUtils.randomEmail)
    }

    /**
     * loads all scripts located in the path 'scripts/initializations'
     */
    private void loadInitialisationScripts() {
        List<File> scripts = new File("scripts/initializations").listFiles().findAll {
            it.isFile()
        }
        log.debug("Loading ${scripts.size()} initializing scripts")
        SpringSecurityUtils.doWithAuth(ADMIN) {
            scripts.each { File script ->
                runScript(script)
            }
        }
    }

    /**
     * Manually setup scheduling task.
     * This is done here so they will be stopped when each test is finished,
     * otherwise there would be problems with the database being deleted
     */
    @SuppressWarnings("CatchThrowable")
    private void initScheduler() {
        log.debug("add needed cron jobs to scheduler")
        TestCronJob.values().each { TestCronJob testCronJob ->
            scheduler.scheduleWithFixedDelay({
                assert exceptionInScheduler == null
                try {
                    testCronJob.execute()
                } catch (Throwable t) {
                    exceptionInScheduler = new OtpRuntimeException("Cron job '${testCronJob.name}' failed for: ${t.message}", t)
                    AbstractWorkflowSpec.log.error(exceptionInScheduler.message, t)
                }
            } as Runnable, 0, testCronJob.delay, testCronJob.timeUnit)
            log.debug("  - Started cron job: ${testCronJob.name} with delay ${testCronJob.delay} ${testCronJob.timeUnit}")
        }
    }

    /**
     * Starts the workflow system and wait then for starting and ending of the given  count of {@link WorkflowRun}.
     * All {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING} are considered as started.
     * All {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING},{@link WorkflowRun.State#RUNNING_OTP} or {@link WorkflowRun.State#RUNNING_WES}
     * are considered as finish.
     *
     * The waiting time is limited by the timeouts {@link #getStartWorkflowTimeout()} and {@link #getRunningTimeout()}.
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     *
     * It is no problem if additional workflows exist, as long at least the given count of {@link WorkflowRun} are ended.
     */
    protected void execute(int requiredWorkflowRunCount = 1, boolean ensureNoFailure = true) {
        log.debug("starting workflow system")
        SessionUtils.withNewSession {
            if (WorkflowRun.count < requiredWorkflowRunCount) {
                throw new OtpRuntimeException("Not enough workfowRuns was created: expects at least ${requiredWorkflowRunCount}, found ${WorkflowRun.count}")
            }
            updateDomainValuesForTesting()
            workflowSystemService.startWorkflowSystem()

            waitUntilWorkflowStarts(requiredWorkflowRunCount)
            waitUntilWorkflowFinishes(runningTimeout, requiredWorkflowRunCount)

            if (ensureNoFailure) {
                ensureThatWorkflowFinishedSuccessfully()
            }
        }
    }

    /**
     * Updates some domain to use the correct value for running the tests:
     * - Project:
     *   - unixGroup
     *   - realm
     *   - processingPriority
     * - WorkflowRun:
     *   - priority
     */
    private void updateDomainValuesForTesting() {
        String unixGroup = configService.workflowProjectUnixGroup
        Project.list().each {
            it.unixGroup = unixGroup
            it.realm = realm
            it.processingPriority = processingPriority
            it.save(flush: true)
        }
        WorkflowRun.list().each {
            it.priority = processingPriority
            it.save(flush: true)
        }
    }

    /**
     * Wait until the given count of workflows started or the timeout {@link #getStartWorkflowTimeout()} is reached.
     * All  {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING} are considered as started.
     *
     * It is no problem if additional workflows exist.
     *
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     */
    private void waitUntilWorkflowStarts(int requiredWorkflowRunCount) {
        Duration timeout = startWorkflowTimeout
        log.debug "Wait for starting ${requiredWorkflowRunCount} workflows, max ${timeout}"
        long lastLog = 0L
        int counter = 0
        if (!ThreadUtils.waitFor({
            if (exceptionInScheduler) {
                throw new OtpRuntimeException("Stop, since exception in one of the cron jobs occurred", exceptionInScheduler)
            }
            long milliSeconds = System.currentTimeMillis()
            if (lastLog < milliSeconds - 10000L) {
                log.debug "waiting for workflow starting (${counter += 10} seconds) ... "
                lastLog = milliSeconds
            }
            return WorkflowRun.countByStateNotEqual(WorkflowRun.State.PENDING) >= requiredWorkflowRunCount
        }, timeout.toMillis(), 1000L)) {
            TimeoutException e = new TimeoutException("Workflow(s) did not started within ${timeout.toString().substring(2)}.")
            log.debug(e.message, e)
            throw e
        }
    }

    /**
     * Wait until the given count of workflows ends or the timeout {@link #getRunningTimeout()} is reached.
     * All  {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING},{@link WorkflowRun.State#RUNNING_OTP} or {@link WorkflowRun.State#RUNNING_WES}
     * are considered as finished.
     *
     * It is no problem if additional workflows exist.
     *
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     */
    private void waitUntilWorkflowFinishes(Duration timeout, int requiredWorkflowRunCount) {
        log.debug "Wait until ${requiredWorkflowRunCount} workflowRuns finished, max ${timeout}"
        long lastLog = 0L
        int counter = 0
        if (!ThreadUtils.waitFor({
            if (exceptionInScheduler) {
                throw new OtpRuntimeException("Stop, since exception in one of the cron jobs occurred", exceptionInScheduler)
            }
            long milliSeconds = System.currentTimeMillis()
            if (lastLog < milliSeconds - 60000L) {
                log.debug "waiting (${counter++} min) ... "
                lastLog = milliSeconds
            }
            return WorkflowRun.countByStateNotInList(RUNNING_AND_WAITING_STATES) >= requiredWorkflowRunCount
        }, timeout.toMillis(), 1000L)) {
            TimeoutException e = new TimeoutException("Workflow did not finish within ${timeout.toString().substring(2)}.")
            log.debug(e.message, e)
            throw e
        }
    }

    /**
     * check, that workflow objects has finished successfully
     */
    protected void ensureThatWorkflowFinishedSuccessfully() {
        assert WorkflowRun.findAllByStateNotEqual(WorkflowRun.State.SUCCESS).empty
        assert WorkflowArtefact.findAllByStateNotEqual(WorkflowArtefact.State.SUCCESS).empty
        assert ClusterJob.findAllByCheckStatusNotEqual(ClusterJob.CheckStatus.FINISHED).empty
        assert ClusterJob.findAllByExitStatusIsNull().empty
        assert ClusterJob.findAllByJobLogIsNull().empty
    }

    /**
     * enum for defining needed cron jobs
     */
    @TupleConstructor
    enum TestCronJob {
        WORKFLOW_STARTING("starting workflows", 5, TimeUnit.SECONDS){
            @Override
            void execute() {
                Holders.applicationContext.getBean(WorkflowRunScheduler).scheduleWorkflowRun()
            }
        },
        JOB_STARTING("starting jobs", 1, TimeUnit.SECONDS){
            @Override
            void execute() {
                Holders.applicationContext.getBean(JobScheduler).scheduleJob()
            }
        },
        CHECK_CLUSTER("check cluster", 5, TimeUnit.SECONDS){
            @Override
            void execute() {
                Holders.applicationContext.getBean(ClusterJobMonitor).check()
            }
        },
        KEEP_ALIVE_REMOTE_SHELL_HELPER("keep alive for RemoteShellHelper", 1, TimeUnit.MINUTES){
            @Override
            void execute() {
                Holders.applicationContext.getBean(RemoteShellHelper).keepAlive()
            }
        },
        KEEP_ALIVE_REMOTE_SFTP("keep alive for FileSystemService", 1, TimeUnit.MINUTES){
            @Override
            void execute() {
                Holders.applicationContext.getBean(FileSystemService).keepAlive()
            }
        }

        /**
         * name of the cron job
         */
        final String name

        /**
         * the delay used for the cron job
         */
        final int delay

        final TimeUnit timeUnit

        /**
         * the job to execute regularly
         */
        abstract void execute()
    }
}
