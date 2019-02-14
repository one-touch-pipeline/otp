package workflows

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.util.logging.Log4j
import org.hibernate.SessionFactory
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.scriptTests.GroovyScriptAwareTestCase
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import javax.sql.DataSource
import java.time.Duration
import java.util.concurrent.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static de.dkfz.tbi.otp.utils.LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout

/**
 * Base class for work-flow integration test cases.
 *
 * To run the workflow tests the preparation steps described in
 * src/docs/guide/testing.md have to be followed.
 */
@Log4j
@TestMixin(IntegrationTestMixin)
abstract class WorkflowTestCase extends GroovyScriptAwareTestCase {

    ErrorLogService errorLogService
    CreateClusterScriptService createClusterScriptService
    RemoteShellHelper remoteShellHelper
    ExecutionHelperService executionHelperService
    SessionFactory sessionFactory
    DataSource dataSource
    SchedulerService schedulerService
    TestConfigService configService

    ClusterJobMonitoringService clusterJobMonitoringService

    ReferenceGenomeService referenceGenomeService

    LinkFileUtils linkFileUtils

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // permissions to be applied to the source test data
    protected final static String TEST_DATA_MODE_DIR = "2750"
    protected final static String TEST_DATA_MODE_FILE = "640"

    final static String CHROMOSOME_NAMES_FILE = 'chromosome-names.txt'

    // The base directory for this test instance ("local root" directory).
    private File baseDirectory = null


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

    /**
     * This method can be overridden if other job submission options are needed.
     */
    protected String getJobSubmissionOptions() {
        JsonOutput.toJson([
                (JobSubmissionOption.WALLTIME): Duration.ofMinutes(20).toString(),
                (JobSubmissionOption.MEMORY)  : "5g",
        ])
    }

    @Before
    void setUpWorkflowTests() {
        // NOTE: the following assumptions won't work (they will cause the tests to fail if not true)
        // before Grails 2.1. This means we can't remove the @Ignore annotations yet, but it will
        // prevent the tests from being run with wrong settings. (TODO: remove this comments when
        // we use Grails >2.1)
        // check whether the correct environment is set
        Assume.assumeTrue(Environment.current.name == "WORKFLOW_TEST")
        setupDirectoriesAndRealm()

        sql = new Sql(dataSource)
        schemaDump = new File(TestCase.createEmptyTestDirectory(), "test-database-dump.sql")
        sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])

        DomainFactory.createAllAlignableSeqTypes()

        DomainFactory.createProcessingOptionForNotificationRecipient()
        DomainFactory.createProcessingOptionLazy(name :OptionName.OTP_USER_LINUX_GROUP, value: configService.getTestingGroup())
        DomainFactory.createProcessingOptionLazy(name: OptionName.CLUSTER_SUBMISSIONS_FAST_TRACK_QUEUE, value: "fasttrack")
        DomainFactory.createProcessingOptionLazy(name: OptionName.TICKET_SYSTEM_URL, value: "1234")
        DomainFactory.createProcessingOptionLazy(name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX, value: "asdf")
        DomainFactory.createProcessingOptionLazy(name: OptionName.FILESYSTEM_FASTQ_IMPORT, value: "")
        DomainFactory.createProcessingOptionLazy(name: OptionName.FILESYSTEM_BAM_IMPORT, value: "")
        DomainFactory.createProcessingOptionLazy(name: OptionName.FILESYSTEM_PROCESSING_USE_REMOTE, value: "true")
        DomainFactory.createProcessingOptionLazy(name: OptionName.FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE, value: "true")
        DomainFactory.createProcessingOptionLazy(name: OptionName.REALM_DEFAULT_VALUE, value: realm.name)
        DomainFactory.createProcessingOptionLazy(name: OptionName.EMAIL_RECIPIENT_ERRORS, value: HelperUtils.randomEmail)
        DomainFactory.createProcessingOptionLazy(name: OptionName.EMAIL_SENDER, value: HelperUtils.randomEmail)
        DomainFactory.createProcessingOptionLazy(name: OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION, value: HelperUtils.randomEmail)
        DomainFactory.createProcessingOptionLazy(name: OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, value: HelperUtils.randomEmail)

        createUserAndRoles()
        loadWorkflow()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            new File("scripts/initializations").listFiles().each { File script ->
                runScript(script)
            }
        }

        DomainFactory.createProcessingOptionLazy(
                name: OptionName.RODDY_APPLICATION_INI,
                value: new File(getInputRootDirectory(), "applicationProperties-3.3-lsf-remote.ini").absolutePath
        )


        assert schedulerService.running.empty
        assert schedulerService.queue.empty
        assert !ProcessingStep.list()

        // manually set up scheduled tasks
        // this is done here so they will be stopped when each test is finished,
        // otherwise there would be problems with the database being deleted
        scheduler.scheduleWithFixedDelay({
            Holders.applicationContext.getBean(SchedulerService).clusterJobCheck()
        } as Runnable, 0, 10, TimeUnit.SECONDS)

        scheduler.scheduleWithFixedDelay({
            Holders.applicationContext.getBean(SchedulerService).schedule()
        } as Runnable, 0, 100, TimeUnit.MILLISECONDS)
    }


    @After
    void tearDownWorkflowTests() {
        // stop scheduled tasks in SchedulerService and start job
        scheduler.shutdownNow()
        startJobRunning = false
        assert scheduler.awaitTermination(1, TimeUnit.MINUTES)

        schedulerService.running.clear()
        schedulerService.queue.clear()
        clusterJobMonitoringService.queuedJobs.clear()

        if (sql) {
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])
        }
        sessionFactory.currentSession.clear()
        TestCase.cleanTestDirectory()

        println "Base directory: ${getBaseDirectory()}"
    }


    private void setupDirectoriesAndRealm() {
        // check whether the wf test root dir is mounted
        // (assume it is mounted if it exists and contains files)
        File rootDirectory = getInputRootDirectory()
        assert rootDirectory.list()?.size(): "${rootDirectory} seems not to be mounted"

        configService.setOtpProperty((OtpProperty.SSH_USER), configService.getWorkflowTestAccountName())

        Map realmParams = [
                name                       : 'REALM_NAME',
                jobScheduler               : configService.getWorkflowTestScheduler(),
                host                       : configService.getWorkflowTestHost(),
                port                       : 22,
                timeout                    : 0,
                defaultJobSubmissionOptions: jobSubmissionOptions,
        ]

        realm = DomainFactory.createRealm(realmParams)

        println "Base directory: ${getBaseDirectory()}"

        [
                (OtpProperty.PATH_PROJECT_ROOT)    : "${getBaseDirectory()}/root_path",
                (OtpProperty.PATH_PROCESSING_ROOT) : "${getBaseDirectory()}/processing_root_path",
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): "${getBaseDirectory()}/logging_root_path",
        ].each { key, value ->
            configService.setOtpProperty(key, value)
        }

        createDirectories([
                configService.getRootPath(),
                new File(configService.getLoggingRootPath(), JobStatusLoggingService.STATUS_LOGGING_BASE_DIR),
        ])
        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(configService.getProcessingRootPath(), "reference_genomes").absolutePath)

        testDataDir = "${getInputRootDirectory()}/files"
        ftpDir = "${getBaseDirectory()}/ftp"
    }

    void createDirectories(List<File> files) {
        createDirectories(files, "2770")
    }

    void createDirectories(List<File> files, String mode) {
        String mkDirs = createClusterScriptService.makeDirs(files, mode)
        assert remoteShellHelper.executeCommand(realm, mkDirs).toInteger() == 0
        files.each {
            WaitingFileUtils.waitUntilExists(it)
        }
    }

    void createDirectoriesString(List<String> fileNames) {
        createDirectories(fileNames.collect { new File(it) })
    }

    void createFilesWithContent(Map<File, String> files) {
        createDirectories(files.keySet()*.parentFile.unique())
        String cmd = files.collect { File key, String value ->
            "echo '${value}' > ${key}"
        }.join('\n')
        remoteShellHelper.executeCommand(realm, cmd)
        files.each { File key, String value ->
            WaitingFileUtils.waitUntilExists(key)
            assert key.text == value + '\n'
        }
    }

    private waitUntilWorkflowFinishes(Duration timeout, int numberOfProcesses = 1) {
        println "Started to wait (until workflow is finished or timeout)"
        long lastPrintln = 0L
        int counter = 0
        if (!ThreadUtils.waitFor({
            if (lastPrintln < System.currentTimeMillis() - 60000L) {
                println "waiting (${counter++} min) ... "
                lastPrintln = System.currentTimeMillis()
            }
            return areAllProcessesFinished(numberOfProcesses)
        }, timeout.toMillis(), 1000L)) {
            throw new TimeoutException("Workflow did not finish within ${timeout.toString().substring(2)}.")
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
            throw new RuntimeException("""\
                |There were ${failureProcessingStepUpdates.size()} failures:
                |${combinedErrorMessage.join("\n")}
                |Details have been written to standard output. See the test report or run grails test-app -echoOut.
                |""".stripMargin())
        }
    }

    private void ensureThatWorkflowHasNotFailed(List<ProcessingStepUpdate> existingFailureProcessingStepUpdate = []) {
        Collection<ProcessingStepUpdate> allFailureProcessingStepUpdates = ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE)
        Collection<ProcessingStepUpdate> failureProcessingStepUpdatesAfterRestart = allFailureProcessingStepUpdates - existingFailureProcessingStepUpdate
        outputFailureInfoAndThrowException(failureProcessingStepUpdatesAfterRestart)

        checkForFailedClusterJobs()
    }

    void checkForFailedClusterJobs() {
        assert ClusterJob.all.every { it.exitStatus != null }
        assert ClusterJob.all.every { it.jobLog != null }
    }

    private boolean areAllProcessesFinished(int numberOfProcesses) {
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
    protected File getBaseDirectory() {
        // Create the directory like a "singleton", since randomness is involved
        if (!baseDirectory) {
            String mkDirs = """\
TEMP_DIR=`mktemp -d -p ${getResultRootDirectory().absolutePath} ${getNonQualifiedClassName()}-${System.getProperty('user.name')}-${HelperUtils.formatter.print(new org.joda.time.DateTime())}-XXXXXXXXXXXXXXXX`
chmod g+rwx \$TEMP_DIR
echo \$TEMP_DIR
"""
            baseDirectory = new File(remoteShellHelper.executeCommandReturnProcessOutput(realm, mkDirs)
                    .assertExitCodeZeroAndStderrEmpty().stdout.trim())
        }
        return baseDirectory
    }

    /**
     * The general directory for all runs of this workflow test
     */
    protected File getWorkflowDirectory() {
        File workflowDirectory = new File(getInputRootDirectory(), getNonQualifiedClassName())
        return workflowDirectory
    }

    protected File getReferenceGenomeDirectory() {
        return new File(getInputRootDirectory(), 'reference-genomes')
    }

    ReferenceGenome createReferenceGenomeWithFile(String referenceGenomeSpecificPath, String fileNamePrefix, String cytosinePositionsIndex = null) {
        File sourceDir = new File(getReferenceGenomeDirectory(), referenceGenomeSpecificPath)
        File source = new File(sourceDir, CHROMOSOME_NAMES_FILE)

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome(
                path: referenceGenomeSpecificPath,
                fileNamePrefix: fileNamePrefix,
                cytosinePositionsIndex: cytosinePositionsIndex,
                chromosomeLengthFilePath: 'hg19_chrTotalLength.tsv',
                chromosomeSuffix: '',
                chromosomePrefix: '',
        )
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(source)

        ["21","22"].each { String chromosomeName ->
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
        if (!classNameMatcher) {
            throw new Exception("Class name must end with Tests")
        }
        return classNameMatcher[FIRST_MATCH][FIRST_GROUP]
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
        return configService.getWorkflowTestInputRootDir()
    }

    /**
     * The result root directory for all tests. This can be overwritten by the key <code>OtpProperty#TEST_WORKFLOW_RESULT</code>
     * in the configuration file. This method should not be called directly; check out {@link #getBaseDirectory()}
     * first.
     *
     * @return the root directory set in the configuration file, or the default location otherwise.
     */
    protected File getResultRootDirectory() {
        return configService.getWorkflowTestResultRootDir()
    }

    /**
     *  Load the workflow and update processing options
     */
    private void loadWorkflow() {
        setupForLoadingWorkflow()

        assert workflowScripts : 'No workflow script provided.'
        SpringSecurityUtils.doWithAuth(ADMIN) {
            JobExecutionPlan.withTransaction {
                workflowScripts.each { String script ->
                    runScript(script)
                }
            }
        }

        ProcessingOption.findAllByName(OptionName.CLUSTER_SUBMISSIONS_OPTION).each {
            it.value = jobSubmissionOptions
            it.save(failOnError: true, flush: true)
        }
    }

    /**
     *  find the start job and execute it, then wait for
     *  either the workflow to finish or the timeout
     */
    protected void execute(int numberOfProcesses = 1, ensureNoFailure = true) {
        schedulerService.startup()
        if (!startJobRunning) {
            AbstractStartJobImpl startJob = Holders.applicationContext.getBean(JobExecutionPlan.list()?.first()?.startJob?.bean, AbstractStartJobImpl)
            assert startJob: 'No start job found.'

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
        if (ensureNoFailure) {
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
            ensureThatWorkflowHasNotFailed([failureStepUpdate])
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
        assert saved: "Failed to persist object \"${instance}\" to database!"
        return saved
    }

    String calculateMd5Sum(File file) {
        assert file: 'file is null'
        WaitingFileUtils.waitUntilExists(file)
        String md5sum
        LogThreadLocal.withThreadLog(System.out) {
            md5sum = executeAndAssertExitCodeAndErrorOutAndReturnStdout("md5sum ${file}").split(' ')[0]
        }
        assert md5sum ==~ /^[a-f0-9]{32}$/
        return md5sum
    }

    protected void setPermissionsRecursive(File directory, String modeDir, String modeFile) {
        assert directory.absolutePath.startsWith(baseDirectory.absolutePath)
        String cmd = "find -L ${directory} -user ${configService.getWorkflowTestAccountName()} -type d -not -perm ${modeDir} -exec chmod ${modeDir} '{}' \\; 2>&1"
        assert remoteShellHelper.executeCommand(realm, cmd).empty
        cmd = "find -L ${directory} -user ${configService.getWorkflowTestAccountName()} -type f -not -perm ${modeFile} -exec chmod ${modeFile} '{}' \\; 2>&1"
        assert remoteShellHelper.executeCommand(realm, cmd).empty
    }
}
