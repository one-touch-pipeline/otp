/*
 * Copyright 2011-2026 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.transform.TupleConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.roddy.BEException
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.GroovyScriptAwareTestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.BaseFolder
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.scheduler.WesMonitor
import de.dkfz.tbi.otp.workflow.shared.WorkflowTestException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun

import javax.sql.DataSource
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.time.Duration
import java.time.LocalDateTime
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
@Integration
abstract class AbstractWorkflowSpec extends Specification implements UserAndRoles, GroovyScriptAwareTestCase,
        DomainFactoryCore, WorkflowSystemDomainFactory, UserDomainFactory {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractWorkflowSpec)

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
    LsdfFilesService lsdfFilesService
    ReferenceGenomeService referenceGenomeService
    RemoteShellHelper remoteShellHelper
    TestConfigService configService
    WorkflowSystemService workflowSystemService
    WorkflowLogService workflowLogService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    /**
     * Needed to save the place of the database dump afterwards.
     */
    @TempDir
    Path tempDir

    /**
     * holds the remote file system
     */
    protected FileSystem remoteFileSystem

    /**
     * the base directory for the current tests
     */
    protected Path referenceDataDirectory

    /**
     * the base directory for the current tests
     */
    protected Path workingDirectory

    /**
     * the directory for reference genomes in the reference data
     */
    protected Path referenceGenomeDirectory

    /**
     * A directory where additional data needed for the test can be placed
     */
    protected Path additionalDataDirectory

    /**
     * the priority used for the workflows
     */
    protected ProcessingPriority processingPriority

    /**
     * An instance for fastq import
     */
    protected FastqImportInstance fastqImportInstance

    /**
     * a input base folder for uuid
     */
    protected BaseFolder baseFolderInput

    /**
     * a base folder for uuid
     */
    protected BaseFolder baseFolder

    /**
     * Holds the new workflow artifacts
     */
    protected List<WorkflowRun> newWorkflowRuns = []

    /**
     * Holds the new workflow runs
     */
    protected List<WorkflowArtefact> newWorkflowArtefact = []

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
    protected JobSchedulerException exceptionInScheduler

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
     * This method can be overwritten if other job submission options are needed.
     */
    protected Map<JobSubmissionOption, String> getJobSubmissionOptions() {
        return [:]
    }

    /**
     * Returns the applicationProperties path
     */
    protected Path getRoddyApplicationPropertyFile() {
        return referenceDataDirectory.resolve("applicationProperties-test.ini")
    }

    @SuppressWarnings("CatchThrowable")
    void setup() {
        log.debug("Start to setup ${getClass().simpleName}.${specificationContext.currentIteration.name}")
        SessionUtils.withTransaction {
            sql = new Sql(dataSource)
            schemaDump = tempDir.resolve("test-database-dump.sql").toFile()
            sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])
            log.debug("database dump written to ${schemaDump}")

            preCheck()

            loadDefaultValuesScripts()
            initFastqImportInstance()
            createUserAndRoles()
            loadInitialisationScripts()
            loadCustomInitialisationScript()
            createProcessingPriorityObject()
            initFileSystem()
            initBaseFolder()
            initProcessingOption()
            initSubmissionOptions()

            initScheduler()

            log.debug("Finish setting up base test setup, workflow depending setup will follow")
        }
    }

    @SuppressWarnings("CatchThrowable")
    void cleanup() {
        String methodName = specificationContext.currentIteration.name
        try {
            log.info("--------------------------------------------------")
            log.info("Starting cleanup after test '${getClass().simpleName}.${methodName}' for base directory: ${workingDirectory}")

            workflowSystemService.stopWorkflowSystem()

            // stop scheduled tasks in SchedulerService
            log.debug("shutting done the spring scheduler")
            scheduler.shutdownNow()
            assert scheduler.awaitTermination(2, TimeUnit.MINUTES)
            log.debug("spring scheduler has terminated")

            killRemainingClusterJobs()

            showState()

            log.debug("reset database")
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])

            log.info "Finish test '${getClass().simpleName}.${methodName}' using base directory: ${workingDirectory}"
        } catch (Throwable t) {
            // exception in cleanup was not reported with stacktrace, therefore add own logging
            log.error("cleanup error", t)
            throw t
        }
    }

    private void killRemainingClusterJobs() {
        SessionUtils.withTransaction {
            List<ClusterJob> clusterJobs = ClusterJob.findAllByCheckStatusNotEqual(ClusterJob.CheckStatus.FINISHED)
            if (clusterJobs) {
                log.debug("prepare to kill ${clusterJobs.size()} still running cluster jobs: ${clusterJobs*.clusterJobId}")

                BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.jobManager
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
        SessionUtils.withTransaction {
            logEntries << "Tree of the file structure"
            if (workingDirectory && Files.exists(workingDirectory)) {
                logEntries << remoteShellHelper.executeCommandReturnProcessOutput("tree -augp ${workingDirectory}").assertExitCodeZero().stdout
            } else {
                logEntries << "workflowResultDirectory doesn't exist yet"
            }

            logEntries << ""
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
                        "workflow run name"         : workflowRun.shortDisplayName,
                        "priority name"             : workflowRun.priority.name,
                        "priority queue"            : workflowRun.priority.queue,
                        "priority roddyConfigSuffix": workflowRun.priority.roddyConfigSuffix,
                        "job count"                 : workflowSteps.size(),
                ].each { key, value ->
                    logEntries << "  - ${key}: ${value}"
                }
                workflowSteps.eachWithIndex { WorkflowStep workflowStep, int stepIndex ->
                    List<WorkflowLog> workflowLogs = workflowLogService.findAllByWorkflowStepInCorrectOrder(workflowStep)
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
                        String time = TimeFormats.DATE_TIME_DASHES.getFormattedDate(workflowLog.dateCreated)
                        logEntries << "    - log ${runIndex}.${stepIndex}.${logIndex}: ${time}: ${prefixForOutput(workflowLog.displayLog())}"
                    }
                    clusterJobs.eachWithIndex { ClusterJob clusterJob, int clusterLogIndex ->
                        logEntries << "    - cluster log ${runIndex}.${stepIndex}.${clusterLogIndex}:"
                        [
                                id               : clusterJob.id,
                                clusterId        : clusterJob.clusterJobId,
                                checkStatus      : clusterJob.checkStatus,
                                exitStatus       : clusterJob.exitStatus,
                                exitCode         : clusterJob.exitCode,
                                jobClass         : clusterJob.jobClass,
                                clusterJobName   : clusterJob.clusterJobName,
                                jobLog           : clusterJob.jobLog,
                                queued           : clusterJob.queued,
                                eligible         : clusterJob.eligible,
                                started          : clusterJob.started,
                                ended            : clusterJob.ended,
                                cpuTime          : clusterJob.cpuTime,
                                requestedWalltime: clusterJob.requestedWalltime,
                                usedCores        : clusterJob.usedCores,
                                requestedCores   : clusterJob.requestedCores,
                                usedMemory       : clusterJob.usedMemory,
                                requestedMemory  : clusterJob.requestedMemory,
                                usedSwap         : clusterJob.usedSwap,
                                node             : clusterJob.node,
                                accountName      : clusterJob.accountName,
                                dependencies     : clusterJob.dependencies*.id.join(","),
                        ].each { key, value ->
                            logEntries << "      - ${key}: ${prefixForOutput(value as String)}"
                        }
                    }
                    workflowStep.wesRuns.eachWithIndex { WesRun wesRun, int weskitIndex ->
                        logEntries << "    - weskit log ${runIndex}.${stepIndex}.${weskitIndex}:"
                        [
                                id           : wesRun.id,
                                wesIdentifier: wesRun.wesIdentifier,
                                subPath      : wesRun.subPath,
                                state        : wesRun.wesRunLog.state,
                                runRequest   : wesRun.wesRunLog.runRequest,
                                name         : wesRun.wesRunLog.runLog.name,
                                cmd          : wesRun.wesRunLog.runLog.cmd,
                                startTime    : wesRun.wesRunLog.runLog.startTime,
                                endTime      : wesRun.wesRunLog.runLog.endTime,
                                exitCode     : wesRun.wesRunLog.runLog.exitCode,
                                stdout       : wesRun.wesRunLog.runLog.stdout,
                                stderr       : wesRun.wesRunLog.runLog.stderr,
                        ].each { key, value ->
                            logEntries << "      - ${key}: ${prefixForOutput(value as String)}"
                        }
                    }
                    if (workflowError) {
                        logEntries << "    - error message: ${prefixForOutput(workflowError.message)}"
                        if (workflowError.stacktrace) {
                            logEntries << "      - stacktrace:"
                        }
                        logEntries << "        ${prefixForOutput(workflowError.stacktrace)}"
                    }
                }
            }
            log.debug(logEntries.join('\n'))
        }
    }

    /**
     * Helper to indent multiline string
     */
    private String prefixForOutput(String value) {
        String prefix = " " * 12
        return value ? value.replaceAll('\n', "\n${prefix}") : ""
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

        log.debug("checking that workflowSystem is stopped")
        assert !workflowSystemService.enabled
    }

    /**
     * create processingPriority and set it to default
     */
    private void createProcessingPriorityObject() {
        log.debug("creating processingPriority and set it to default")
        processingPriority = createProcessingPriority([
                priority                   : 0,
                name                       : 'workflow test',
                errorMailPrefix            : 'prefix',
                queue                      : configService.workflowTestQueue,
                roddyConfigSuffix          : configService.workflowTestConfigSuffix,
                allowedParallelWorkflowRuns: 1000,
        ])
        findOrCreateProcessingOption(name: OptionName.PROCESSING_PRIORITY_DEFAULT_NAME, value: processingPriority.name)
    }

    /**
     * create fastqImportInstance & ticket
     */
    private void initFastqImportInstance() {
        log.debug("creating fastqImportInstance & ticket")
        fastqImportInstance = createFastqImportInstance([
                ticket: createTicket()
        ])
    }

    /**
     * Initialized file system depending properties.
     * That includes:
     * - {@link TestConfigService}
     * - {@link #remoteFileSystem}
     * - diverse directories
     *   - {@link #referenceDataDirectory}
     *   - {@link #workingDirectory}
     *   - {@link #referenceGenomeDirectory}
     *   - {@link #additionalDataDirectory}
     *
     * Also the {@link #workingDirectory} is created.
     */
    private void initFileSystem() {
        log.debug("initializing fileSystem and depending options")
        findOrCreateProcessingOption(name: OptionName.MAXIMUM_PARALLEL_SSH_CALLS, value: '2')
        findOrCreateProcessingOption(name: OptionName.MAXIMUM_SFTP_CONNECTIONS, value: '2')

        remoteFileSystem = fileSystemService.remoteFileSystem

        String workSubDirectory = [
                System.getProperty('user.name'),
                this.class.simpleName,
                TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedLocalDateTime(LocalDateTime.now()),
                (Math.abs(HelperUtils.random.nextLong()) % 1000000).toString().padLeft(6, '0'),
        ].join('_')

        referenceDataDirectory = configService.workflowTestInputRootDir
        workingDirectory = configService.workflowTestResultRootDir.resolve(workSubDirectory)
        referenceGenomeDirectory = workingDirectory.resolve("reference-genomes")
        additionalDataDirectory = workingDirectory.resolve('additional-data')

        String unixGroup = configService.testingGroup
        fileService.createDirectoryRecursivelyAndSetPermissions(workingDirectory, unixGroup)

        [
                (OtpProperty.PATH_PROJECT_ROOT)    : "${workingDirectory}/projectPath",
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): "${workingDirectory}/loggingPath",
        ].each { key, value ->
            configService.addOtpProperty(key, value)
        }

        log.debug "Base directory: ${workingDirectory}"
    }

    /**
     * init the base folder for uuid
     */
    private void initBaseFolder() {
        log.debug("creating base folder object")
        baseFolderInput = createBaseFolder([
                path    : workingDirectory.resolve('baseFolderInput'),
                writable: false,
        ])
        baseFolder = createBaseFolder([
                path    : workingDirectory.resolve('baseFolder'),
                writable: true,
        ])
    }

    /**
     * initialized needed processing option.
     *
     * It depends on the initialisation of:
     * - {@link #initFileSystem()}
     * - {@link #createProcessingPriorityObject()}
     */
    private void initProcessingOption() {
        log.debug("creating processingOptions")
        // emails addresses
        findOrCreateProcessingOption(name: OptionName.EMAIL_TICKET_SYSTEM, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_REPLY_TO, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_SENDER, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, value: HelperUtils.randomEmail)
        findOrCreateProcessingOption(name: OptionName.EMAIL_CLUSTER_ADMINISTRATION, value: HelperUtils.randomEmail)

        // roddy and other paths
        findOrCreateProcessingOption(name: OptionName.RODDY_APPLICATION_INI, value: roddyApplicationPropertyFile.toString())
        findOrCreateProcessingOption(name: OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY, value: configService.workflowTestRoddySharedFilesBaseDir.toString())
        findOrCreateProcessingOption(name: OptionName.BASE_PATH_REFERENCE_GENOME, value: referenceGenomeDirectory.toString())

        // cluster and file system
        findOrCreateProcessingOption(name: OptionName.CLUSTER_NAME, value: 'CLUSTER NAME')
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_TIMEOUT, value: 2)
        findOrCreateProcessingOption(name: OptionName.OTP_SYSTEM_USER, value: createUser().username)
        findOrCreateProcessingOption(name: OptionName.OTP_USER_LINUX_GROUP, value: configService.testingGroup)
        findOrCreateProcessingOption(name: OptionName.WITHDRAWN_UNIX_GROUP, value: configService.testingGroup)
        findOrCreateProcessingOption(name: OptionName.PROCESSING_PRIORITY_DEFAULT_NAME, value: processingPriority.name)
        findOrCreateProcessingOption(name: OptionName.FILESYSTEM_TIMEOUT, value: '1')

        // other values
        findOrCreateProcessingOption(name: OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "90")
        findOrCreateProcessingOption(name: OptionName.TICKET_SYSTEM_URL, value: "url ${nextId}")
        findOrCreateProcessingOption(name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX, value: "prefix${nextId}")
    }

    /**
     * initialize submission options
     */
    private void initSubmissionOptions() {
        Map<JobSubmissionOption, String> options = jobSubmissionOptions
        if (options) {
            ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment(
                    configValues: JsonOutput.toJson([OTP_CLUSTER: options])
            )
            createExternalWorkflowConfigSelector(
                    workflowVersions: [],
                    workflows: [],
                    referenceGenomes: [],
                    libraryPreparationKits: [],
                    seqTypes: [],
                    projects: [],
                    externalWorkflowConfigFragment: fragment,
            )
        }
    }

    /**
     * loads all scripts located in the path 'scripts/initializations'
     */
    private void loadInitialisationScripts() {
        List<File> scripts = new File("scripts/initializations").listFiles().findAll {
            it.isFile()
        }
        log.debug("Loading ${scripts.size()} initializing scripts")
        doWithAuth(ADMIN) {
            scripts.each { File script ->
                log.debug("  - Load ${script}")
                runScript(script)
            }
        }
    }
    /**
     * loads the script provided by OtpProperty.TEST_WORKFLOW_INTI_SCRIPT'
     */
    private void loadCustomInitialisationScript() {
        Path script = configService.workflowTestInitScript
        if (Files.isRegularFile(script)) {
            doWithAuth(ADMIN) {
                log.debug("Loading custom init script: ${script}")
                runScript(script)
                log.debug("Custom init script executed successfully")

                log.debug("Validating workflow test properties")
                configService.validateWorkflowTestProperties()
                log.debug("Workflow test properties are valid")
            }
        } else {
            String msg = "Custom init script configured but not found: ${script}"
            log.error(msg)
            throw new WorkflowTestException(msg)
        }
    }

    /**
     * loads scripts located in the path 'migrations/changelogs/defaultValues'
     */
    private void loadDefaultValuesScripts() {
        log.debug("Loading default values:")
        Path dir = new File(".").toPath().resolve("migrations/changelogs/defaultValues")
        List<Path> files = [
                dir.resolve("seq-types.sql"),
                dir.resolve("workflows.sql"),
                dir.resolve("workflow-api-versions.sql"),
                dir.resolve("workflow-versions.sql"),
        ]
        Files.walkFileTree(dir.resolve('workflow-config'), new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file)
                return FileVisitResult.CONTINUE
            }
        })

        files.each {
            log.debug("  - Load ${it}")
            sql.execute(Files.readString(it).replaceAll("nextval('hibernate_sequence')", "NEXT VALUE FOR hibernate_sequence").replaceAll("ON CONFLICT DO NOTHING", ""))
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
                    exceptionInScheduler = new JobSchedulerException("Cron job '${testCronJob.name}' failed for: ${t.message}", t)
                    AbstractWorkflowSpec.log.error(exceptionInScheduler.message, t)
                }
            } as Runnable, 0, testCronJob.delay, testCronJob.timeUnit)
            log.debug("  - Started cron job: ${testCronJob.name} with delay ${testCronJob.delay} ${testCronJob.timeUnit}")
        }
    }

    protected void startWorkflow() {
        log.debug("starting workflow system")
        SessionUtils.withTransaction {
            int newWorkflowCountDataBase = WorkflowRun.countByState(WorkflowRun.State.PENDING)
            int newWorkflowCount = newWorkflowRuns.size()
            if (newWorkflowCountDataBase != newWorkflowCount) {
                throw new WorkflowTestException(
                        "The count of new workfowRuns is incorrect: found ${newWorkflowCountDataBase}, but expected ${newWorkflowCount}")
            }
            updateDomainValuesForTesting()
            workflowSystemService.startWorkflowSystem()
        }
    }
    /**
     * Starts the workflow system and then waits for the starting and ending of the given count of {@link WorkflowRun}.
     * All {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING} are considered as started.
     * All {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING},{@link WorkflowRun.State#RUNNING_OTP}, or {@link WorkflowRun.State#RUNNING_WES}
     * are considered as finished.
     *
     * If all workflows are finished, the workflow states are checked based on the given {@link CheckType}.
     *
     * The waiting time is limited by the timeouts {@link #getStartWorkflowTimeout()} and {@link #getRunningTimeout()}.
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     *
     * It is no problem if additional workflows exist, as long at least the given count of {@link WorkflowRun} are ended.
     */
    protected void execute(CheckType checkType = CheckType.SUCCESS) {
        startWorkflow()
        waitUntilWorkflowStarts()
        waitUntilWorkflowFinishes()
        log.debug("workflows finished")

        checkType.check(newWorkflowRuns, newWorkflowArtefact)
        ensureThatFilePermissionsAreCorrect()
    }

    /**
     * Updates some domain to use the correct value for running the tests:
     * - Project:
     *   - unixGroup
     *   - processingPriority
     * - WorkflowRun:
     *   - priority
     * - connect dataFiles to fastqImportInstance with ticket
     */
    private void updateDomainValuesForTesting() {
        String unixGroup = configService.workflowProjectUnixGroup
        log.debug("update projects")
        Project.list().each {
            it.unixGroup = unixGroup
            it.processingPriority = processingPriority
            it.save(flush: true)
        }
        log.debug("update workflow runs")
        WorkflowRun.list().each {
            it.priority = processingPriority
            it.save(flush: true)
        }

        log.debug("check correct connection of datafiles to fastqImportInstance")
        RawSequenceFile.list().each {
            assert it.fastqImportInstance == fastqImportInstance
        }
    }

    /**
     * Generic polling helper that blocks the process until {@code closure} returns {@code true} or the timeout expires.
     *
     * Polls every second. Logs a progress line every {@code interval} milliseconds. Aborts early with a
     * {@link JobSchedulerException} if a scheduler exception was detected.
     *
     * @param condition human-readable label used in log and timeout messages (e.g. {@code "start"}, {@code "finish"})
     * @param timeout maximum time to wait before throwing {@link TimeoutException}
     * @param interval milliseconds between periodic progress log messages
     * @param closure condition evaluated inside a transaction; polling stops when it returns {@code true}
     */
    protected void waitUntilWorkflowInState(String condition, Duration timeout, long interval, Closure closure) {
        long timeoutMillis = timeout.toMillis()
        log.debug "Wait for ${newWorkflowRuns.size()} workflows to ${condition}, max ${timeout}"
        long lastLog = System.currentTimeMillis()
        int counter = 0
        if (!ThreadUtils.waitFor({
            if (exceptionInScheduler) {
                throw new JobSchedulerException("Stop, since exception in one of the cron jobs occurred", exceptionInScheduler)
            }
            long milliSeconds = System.currentTimeMillis()
            if (lastLog < milliSeconds - interval) {
                if (interval == 60000L) { // 1 minute interval
                    log.debug "waiting for workflow to ${condition} in (${counter += 1} min) ... "
                } else { // otherwise in seconds
                    log.debug "waiting for workflow to ${condition} in (${counter += 10} sec) ... "
                }
                lastLog += interval
            }
            SessionUtils.withTransaction {
                return closure()
            }
        }, timeoutMillis, 1000L)) {
            TimeoutException e = new TimeoutException("Workflow(s) did not ${condition} within ${timeoutMillis} ms.")
            log.debug(e.message, e)
            throw e
        }
    }

    /**
     * Wait until all new workflows are started or the timeout {@link #getStartWorkflowTimeout()} is reached.
     * All {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING} are considered as started.
     *
     * It is no problem if additional workflows exist.
     *
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     */
    protected void waitUntilWorkflowStarts() {
        waitUntilWorkflowInState("start", startWorkflowTimeout, 10000L) {
            WorkflowRun.countByStateAndIdInList(WorkflowRun.State.PENDING, newWorkflowRuns*.id) == 0
        }
    }

    /**
     * Wait until the workflows in {@link #newWorkflowRuns} ends or the timeout {@link #getRunningTimeout()} is reached.
     *
     * All {@link WorkflowRun} not in state {@link WorkflowRun.State#PENDING},{@link WorkflowRun.State#RUNNING_OTP} or {@link WorkflowRun.State#RUNNING_WES}
     * are considered as finished.
     *
     * {@link WorkflowRun} not in {@link #newWorkflowRuns} are ignored.
     *
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     */
    protected void waitUntilWorkflowFinishes() {
        waitUntilWorkflowInState("finish", runningTimeout, 60000L) {
            WorkflowRun.countByStateInListAndIdInList(RUNNING_AND_WAITING_STATES, newWorkflowRuns*.id) == 0
        }
    }

    /**
     * Wait until at least one workflow in {@link #newWorkflowRuns} reaches state {@link WorkflowRun.State#RUNNING_WES},
     * or the timeout {@link #getRunningTimeout()} is reached.
     *
     * Use this before issuing a kill request to ensure cluster jobs have been submitted.
     *
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     */
    protected void waitUntilWorkflowRunWes() {
        waitUntilWorkflowInState("reach RUNNING_WES", runningTimeout, 60000L) {
            WorkflowRun.countByStateAndIdInList(WorkflowRun.State.RUNNING_WES, newWorkflowRuns*.id) > 0
        }
    }

    /**
     * Wait until all workflows in {@link #newWorkflowRuns} have left {@link WorkflowRun.State#RUNNING_WES}
     * (i.e. at least one has reached {@link WorkflowRun.State#KILLED}),
     * or the timeout {@link #getRunningTimeout()} is reached.
     *
     * Use this after issuing a kill request to confirm the kill has been processed.
     *
     * In case the timeout is reached, a {@link TimeoutException} is thrown.
     */
    protected void waitUntilWorkflowKilled() {
        waitUntilWorkflowInState("reach KILLED", runningTimeout, 60000L) {
            WorkflowRun.countByStateAndIdInList(WorkflowRun.State.KILLED, newWorkflowRuns*.id) > 0
        }
    }

    /**
     * check, that are files created have the correct permissions
     */
    protected void ensureThatFilePermissionsAreCorrect() {
        log.debug("Checking file permissions")
        Path rootPath = remoteFileSystem.getPath(configService.rootPath.toString())
        Files.walk(rootPath).withCloseable { stream ->
            stream.each { Path path ->
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
                    assert permissions == FileService.DEFAULT_DIRECTORY_PERMISSION
                }
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
                    assert permissions == FileService.DEFAULT_FILE_PERMISSION
                }
            }
        }
    }

    /**
     * Creates Fragments and Selector for Workflow
     */
    protected void createFragmentAndSelector(String name, String json, Map selectors) {
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                name        : name,
                configValues: json.replaceAll('[ \n]+', ' '),
        ])
        log.info("Create fragment ${name} ${fragment}")

        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector([
                name                          : name,
                workflowVersions              : [],
                workflows                     : [],
                referenceGenomes              : [],
                libraryPreparationKits        : [],
                seqTypes                      : [],
                projects                      : [],
                externalWorkflowConfigFragment: fragment,
                selectorType                  : SelectorType.GENERIC,
        ] + selectors)
        log.info("Create selector ${name} ${selector}")
    }

    /**
     * link the reference genome directory into the test structure
     */
    protected void linkReferenceGenomeDirectoryToReference(ReferenceGenome referenceGenome) {
        Path target = referenceDataDirectory.resolve("reference-genomes").resolve(referenceGenome.path)
        Path link = referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)
        fileService.createLink(link, target, configService.workflowProjectUnixGroup)
    }

    /**
     * enum for defining needed cron jobs
     */
    // the nested enum is not abstracts itself, its only inside an abstract class
    @SuppressWarnings(["AbstractClassName", "UnnecessaryPackageReference"])
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
                Holders.applicationContext.getBean(de.dkfz.tbi.otp.workflowExecution.JobScheduler).scheduleJob()
            }
        },
        CHECK_CLUSTER("check cluster", 5, TimeUnit.SECONDS){
            @Override
            void execute() {
                Holders.applicationContext.getBean(ClusterJobMonitor).check()
            }
        },
        CHECK_WES("check wes", 5, TimeUnit.SECONDS){
            @Override
            void execute() {
                Holders.applicationContext.getBean(WesMonitor).check()
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
         * the delay used for the cron job in the unit defined by  {@link #timeUnit}
         */
        final int delay

        /**
         * the time unit used for the {@link #delay} number
         */
        final TimeUnit timeUnit

        /**
         * the job to execute regularly
         */
        abstract void execute()
    }

    /**
     * The expected type of workflow state
     */
    // the nested enum is not abstract itself, it is only inside an abstract class
    @SuppressWarnings(["AbstractClassName"])
    enum CheckType {
        /**
         * All new workflows should be successfully finished
         */
        SUCCESS{
            @Override
            void check(List<WorkflowRun> newWorkflowRuns, List<WorkflowArtefact> newWorkflowArtefacts) {
                SessionUtils.withTransaction {
                    assert WorkflowRun.findAllByStateNotEqualAndIdInList(WorkflowRun.State.SUCCESS, newWorkflowRuns*.id).empty
                    assert WorkflowRun.findAllByStateAndIdInList(WorkflowRun.State.SUCCESS, newWorkflowRuns*.id) == newWorkflowRuns
                    assert WorkflowArtefact.findAllByStateNotEqualAndIdInList(WorkflowArtefact.State.SUCCESS, newWorkflowArtefacts*.id).empty
                    assert WorkflowArtefact.findAllByStateAndIdInList(WorkflowArtefact.State.SUCCESS, newWorkflowArtefacts*.id) == newWorkflowArtefacts
                    assert ClusterJob.findAllByCheckStatusNotEqual(ClusterJob.CheckStatus.FINISHED).empty
                    assert ClusterJob.findAllByExitStatusIsNull().empty
                    assert ClusterJob.findAllByJobLogIsNull().empty
                }
            }
        },

        /**
         * All new workflows should be in state skipped
         */
        SKIPPED{
            @Override
            void check(List<WorkflowRun> newWorkflowRuns, List<WorkflowArtefact> newWorkflowArtefacts) {
                SessionUtils.withTransaction {
                    assert WorkflowRun.findAllByStateNotEqualAndIdInList(WorkflowRun.State.SKIPPED_MISSING_PRECONDITION, newWorkflowRuns*.id).empty
                    assert WorkflowArtefact.findAllByStateNotEqualAndIdInList(WorkflowArtefact.State.SKIPPED, newWorkflowArtefacts*.id).empty
                    assert ClusterJob.count == 0
                }
            }
        },

        abstract void check(List<WorkflowRun> newWorkflowRuns, List<WorkflowArtefact> newWorkflowArtefacts)
    }
}
