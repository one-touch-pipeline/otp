package workflows

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.WorkflowTestRealms
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import grails.util.Environment
import grails.util.Holders
import groovy.sql.Sql
import groovy.util.logging.Log4j
import org.hibernate.SessionFactory
import org.joda.time.Duration
import org.joda.time.format.PeriodFormat
import org.junit.After
import org.junit.Assume
import org.junit.Before

import javax.sql.DataSource
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import static CollectionUtils.exactlyOneElement

/**
 * Base class for work-flow integration test cases.
 *
 * To run the workflow tests the preparation steps described in
 * src/docs/guide/devel/testing/workflowTesting.gdoc have to be followed.
 *
 */
@Log4j
@TestMixin(IntegrationTestMixin)
abstract class WorkflowTestCase extends GroovyScriptAwareTestCase {

    ErrorLogService errorLogService
    CreateClusterScriptService createClusterScriptService
    ExecutionService executionService
    ExecutionHelperService executionHelperService
    SessionFactory sessionFactory
    DataSource dataSource
    SchedulerService schedulerService

    PbsMonitorService pbsMonitorService

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // set this to true if you are working on the tests and want to keep the workflow results
    // don't forget to delete them manually
    protected static final boolean KEEP_TEMP_FOLDER = false

    // fast queue, here we come!
    static final String pbsOptions = '{"-l": {nodes: "1", walltime: "20:00", mem: "5g"}, "-j": "oe"}'

    // permissions to be applied to the source test data
    protected final static String TEST_DATA_MODE_DIR = "2750"
    protected final static String TEST_DATA_MODE_FILE = "640"

    // The base directory for this test instance ("local root" directory).
    private File baseDirectory = null


    String rootPath
    String processingRootPath
    String loggingRootPath
    String stagingRootPath
    String programsRootPath
    String testDataDir
    String ftpDir

    Realm realm

    File schemaDump
    Sql sql

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)
    private boolean startJobRunning = false

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
    protected void setupForLoadingWorkflow() {
        // do nothing by default
    }


    @Before
    public void setUpWorkflowTests() {
        // NOTE: the following assumptions won't work (they will cause the tests to fail if not true)
        // before Grails 2.1. This means we can't remove the @Ignore annotations yet, but it will
        // prevent the tests from being run with wrong settings. (TODO: remove this comments when
        // we use Grails >2.1)
        // check whether the correct environment is set
        Assume.assumeTrue(Environment.current.name == "WORKFLOW_TEST")

        setupDirectoriesAndRealms()

        sql = new Sql(dataSource)
        schemaDump = new File(TestCase.createEmptyTestDirectory(), "test-database-dump.sql")
        sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])


        createUserAndRoles()
        loadWorkflow()

        assert schedulerService.running.empty
        assert schedulerService.queue.empty
        assert !ProcessingStep.list()

        // manually set up scheduled tasks
        // this is done here so they will be stopped when each test is finished,
        // otherwise there would be problems with the database being deleted
        scheduler.scheduleWithFixedDelay({ Holders.applicationContext.getBean(SchedulerService).pbsMonitorCheck() } as Runnable, 0, 10, TimeUnit.SECONDS)

        scheduler.scheduleWithFixedDelay({ Holders.applicationContext.getBean(SchedulerService).schedule() } as Runnable, 0, 100, TimeUnit.MILLISECONDS)
    }


    @After
    public void tearDownWorkflowTests() {
        // stop scheduled tasks in SchedulerService and start job
        scheduler.shutdownNow()
        startJobRunning = false
        assert scheduler.awaitTermination(1, TimeUnit.MINUTES)

        schedulerService.running.clear()
        schedulerService.queue.clear()
        pbsMonitorService.queuedJobs.clear()

        if (sql) {
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])
        }
        sessionFactory.currentSession.clear()
        TestCase.cleanTestDirectory()

        cleanupDirectories()
    }


    private void setupDirectoriesAndRealms() {
        // check whether the wf test root dir is mounted
        // (assume it is mounted if it exists and contains files)
        File rootDirectory = getRootDirectory()
        assert rootDirectory.list()?.size() : "${rootDirectory} seems not to be mounted"

        rootPath = "${getBaseDirectory()}/root_path"
        processingRootPath = "${getBaseDirectory()}/processing_root_path"
        loggingRootPath = "${getBaseDirectory()}/logging_root_path"
        stagingRootPath = "${getBaseDirectory()}/staging_root_path"
        programsRootPath = "/"
        testDataDir = "${getRootDirectory()}/files"
        ftpDir = "${getBaseDirectory()}/ftp"

        Map realmParams = [
                rootPath: rootPath,
                processingRootPath: processingRootPath,
                programsRootPath: programsRootPath,
                loggingRootPath: loggingRootPath,
                stagingRootPath: stagingRootPath,
                unixUser: getAccountName(),
                pbsOptions: pbsOptions,
        ]

        assert WorkflowTestRealms.createRealmDataManagementDKFZ(realmParams).save(flush: true)
        realm = WorkflowTestRealms.createRealmDataProcessingDKFZ(realmParams).save(flush: true)
        assert realm

        assert !getBaseDirectory().exists()
        createDirectories([
                getBaseDirectory(),
                new File(realm.rootPath),
                new File(realm.loggingRootPath, JobStatusLoggingService.STATUS_LOGGING_BASE_DIR),
                new File(realm.stagingRootPath),
        ])
    }

    public void createDirectories(List<File> files) {
        createDirectories(files, "2770")
    }

    public void createDirectories(List<File> files, String mode) {
        String mkDirs = createClusterScriptService.makeDirs(files, mode)
        assert executionService.executeCommand(realm, mkDirs).toInteger() == 0
        files.each {
            WaitingFileUtils.waitUntilExists(it)
        }
    }

    public void createDirectoriesString(List<String> fileNames) {
        createDirectories(fileNames.collect { new File(it) })
    }

    public void createFilesWithContent(Map<File, String> files) {
        createDirectories(files.keySet()*.parentFile.unique())
        String cmd = files.collect {File key, String value ->
            "echo '${value}' > ${key}"
        }.join('\n')
        executionService.executeCommand(realm, cmd)
        files.each  {File key, String value ->
            WaitingFileUtils.waitUntilExists(key)
            assert key.text == value + '\n'
        }
    }


    private void cleanupDirectories() {
        String cleanUpCommand = createClusterScriptService.removeDirs([getBaseDirectory()], CreateClusterScriptService.RemoveOption.RECURSIVE_FORCE)
        if(!KEEP_TEMP_FOLDER) {
            assert executionService.executeCommand(realm, cleanUpCommand).toInteger() == 0
        } else {
            println "Base directory: ${getBaseDirectory()}"
        }
    }


    private waitUntilWorkflowFinishes(Duration timeout, int numberOfProcesses = 1) {
        println "Started to wait (until workflow is finished or timeout)"
        long lastPrintln = 0L
        int counter = 0
        if (!ThreadUtils.waitFor({
            if (lastPrintln < System.currentTimeMillis() - 60000L) {
                println "waiting (${counter++}) ... "
                lastPrintln = System.currentTimeMillis()
            }
            return areAllProcessesFinished(numberOfProcesses)
        }, timeout.millis, 1000L)) {
            throw new TimeoutException("Workflow did not finish within ${PeriodFormat.default.print(timeout.toPeriod())}.")
        }
    }

    private void outputFailureInfoAndThrowException(Collection<ProcessingStepUpdate> failureProcessingStepUpdates) {
        List<String> combinedErrorMessage = []
        failureProcessingStepUpdates.each {
            println "ProcessingStep ${it.processingStep.id} failed."
            if (it.error) {
                println 'Error message:'
                println it.error.errorMessage
                combinedErrorMessage << it.error.errorMessage
                if (it.error.stackTraceIdentifier) {
                    println 'Stack trace:'
                    println errorLogService.loggedError(it.error.stackTraceIdentifier)
                } else {
                    println 'The stackTraceIdentifier property of the ProcessingError is not set.'
                }
            } else {
                println 'The error property of the ProcessingStepUpdate is not set.'
            }
        }
        if (!failureProcessingStepUpdates.empty) {
            throw new RuntimeException("There were ${failureProcessingStepUpdates.size()} failures:\n${combinedErrorMessage.join("\n")}\nDetails have been written to standard output. See the test report or run grails test-app -echoOut.")
        }
    }


    private void ensureThatWorkflowHasNotFailed() {
        Collection<ProcessingStepUpdate> failureProcessingStepUpdates = ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE)
        outputFailureInfoAndThrowException(failureProcessingStepUpdates)
    }

    private void ensureThatRestartedWorkflowHasNotFailed(ProcessingStepUpdate existingFailureProcessingStepUpdate) {
        Collection<ProcessingStepUpdate> allFailureProcessingStepUpdates = ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE)
        Collection<ProcessingStepUpdate> failureProcessingStepUpdatesAfterRestart = allFailureProcessingStepUpdates - [existingFailureProcessingStepUpdate]
        outputFailureInfoAndThrowException(failureProcessingStepUpdatesAfterRestart)
    }

    private boolean areAllProcessesFinished(int numberOfProcesses) {
        Collection<Process> processes = Process.list()
        assert processes.size() <= numberOfProcesses
        return processes.size() == numberOfProcesses && processes.every {
            it.refresh(); it.finished
        }
    }

    /**
     * The account name to use on the DKFZ cluster. This can be overwritten by the key
     * <code>otp.testing.workflows.account</code> in the configuration file.
     *
     * @return the account name set in the configuration file, or the default account name otherwise.
     */
    protected String getAccountName() {
        return grailsApplication.config.otp.testing.workflows.account
    }

    /**
     * The base directory for this test instance. It can be considered as "local root", meaning all files
     * and directories should be created under this directory.
     *
     * @return the base directory
     */
    protected File getBaseDirectory() {
        // Create the directory like a "singleton", since randomness is involved
        if (!baseDirectory) {
            File workflowDirectory = getWorkflowDirectory()
            // FIXME: This should be replaced when we updated to JDK7, finally: OTP-1502
            baseDirectory = uniqueDirectory(workflowDirectory)
        }
        return baseDirectory
    }

    protected File getWorkflowDirectory() {
        File workflowDirectory = new File(getRootDirectory(), getNonQualifiedClassName())
        return workflowDirectory
    }

    /**
     * Extracts the non-qualified class name from the test case name.
     * <p>
     * Note: This method should not be called directly. It's used only for path construction.
     *
     * @return the non-qualified class name.
     */
    private getNonQualifiedClassName() {
        final FIRST_MATCH = 0
        final FIRST_GROUP = 1
        def classNameMatcher = (this.class.name =~ /\.?(\w+)Tests$/)
        if(!classNameMatcher) {
            throw new Exception("Class name must end with Tests")
        }
        return classNameMatcher[FIRST_MATCH][FIRST_GROUP]
    }

    /**
     * The root directory for all tests. This can be overwritten by the key <code>otp.testing.workflows.rootdir</code>
     * in the configuration file. This method should not be called directly; check out {@link #getBaseDirectory()}
     * first.
     *
     * @return the root directory set in the configuration file, or the default location otherwise.
     *
     * @see #getBaseDirectory()
     */
    protected File getRootDirectory() {
        return new File(grailsApplication.config.otp.testing.workflows.rootdir)
    }

    /**
     * Construct a unique directory under the given under root directory.
     *
     * This may have security implications and should be replaced by createTempDir() from JDK 7.
     *
     * @param root the root directory under which the unique directory is constructed.
     * @return the unique directory
     */
    private File uniqueDirectory(File root) {
        assert root : 'No root directory provided. Unable to construct a unique directory.'
        return new File(root, "tmp-${System.getProperty('user.name')}-${HelperUtils.getUniqueString()}")
    }

    /**
     *  Load the workflow and update processing options
     */
    private void loadWorkflow() {
        setupForLoadingWorkflow()

        assert workflowScripts : 'No workflow script provided.'
        SpringSecurityUtils.doWithAuth("admin") {
            JobExecutionPlan.withTransaction {
                workflowScripts.each { String script ->
                    runScript(script)
                }
            }
        }

        ProcessingOption.findAllByNameLike("${PbsOptionMergingService.PBS_PREFIX}%").each {
            it.value = pbsOptions
            it.save(failOnError: true, flush: true)
        }
    }


    /**
     *  find the start job and execute it, then wait for
     *  either the workflow to finish or the timeout
     */
    protected void execute(int numberOfProcesses = 1, ensureNoFailure = true) {
        if(!startJobRunning) {
            AbstractStartJobImpl startJob = Holders.applicationContext.getBean(JobExecutionPlan.list()?.first()?.startJob?.bean, AbstractStartJobImpl)
            assert startJob : 'No start job found.'

            scheduler.scheduleWithFixedDelay({
                try {
                    startJob.execute()
                } catch (Throwable t) {
                    println 'Exception in StartJob'
                    t.printStackTrace(System.out)
                    throw t
                }
            } as Runnable, 0, 5, TimeUnit.SECONDS)
            startJobRunning = true
        }

        waitUntilWorkflowFinishes(timeout, numberOfProcesses)
        if(ensureNoFailure) {
            ensureThatWorkflowHasNotFailed()
        }
    }

    protected void restartWorkflowFromFailedStep(ensureNoFailure = true) {
        ProcessingStepUpdate failureStepUpdate = exactlyOneElement(ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE))
        ProcessingStep step = failureStepUpdate.processingStep
        schedulerService.restartProcessingStep(step)
        println "RESTARTED THE WORKFLOW FROM THE FAILED JOB"
        waitUntilWorkflowFinishes(timeout)
        if (ensureNoFailure) {
            ensureThatRestartedWorkflowHasNotFailed(failureStepUpdate)
        }
    }

    /**
     * Convenience method to persist a domain instance into the database. Assert whether
     * the operation was successful.
     *
     * @param instance the domain instance to persist
     * @return the instance, or <code>null</code> otherwise.
     */
    protected persist(instance) {
        def saved = instance.save(flush: true)
        assert saved : "Failed to persist object \"${instance}\" to database!"
        return saved
    }

    String calculateMd5Sum(File file) {
        assert file : 'file is null'
        WaitingFileUtils.waitUntilExists(file)
        String md5sum
        LogThreadLocal.withThreadLog(System.out) {
            md5sum = ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("md5sum ${file}").split(' ')[0]
        }
        assert md5sum ==~ /^[a-f0-9]{32}$/
        return md5sum
    }

    protected void setPermissionsRecursive(File directory, String modeDir, String modeFile) {
        assert directory.absolutePath.startsWith(baseDirectory.absolutePath)
        String cmd = "find -L ${directory} -user ${getAccountName()} -type d -not -perm ${modeDir} -exec chmod ${modeDir} '{}' \\; 2>&1"
        assert executionService.executeCommand(realm, cmd).empty
        cmd = "find -L ${directory} -user ${getAccountName()} -type f -not -perm ${modeFile} -exec chmod ${modeFile} '{}' \\; 2>&1"
        assert executionService.executeCommand(realm, cmd).empty
    }
}
