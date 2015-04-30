package workflows

import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ThreadUtils
import groovy.util.logging.Log4j
import org.joda.time.Duration
import org.joda.time.format.PeriodFormat

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * Base class for work-flow integration test cases.
 */
@Log4j
class WorkflowTestCase extends GroovyScriptAwareTestCase {

    ErrorLogService errorLogService

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false


    /** The base directory for this test instance ("local root" directory). */
    private File baseDirectory = null


    void waitUntilWorkflowFinishesWithoutFailure(Duration timeout, int numberOfProcesses = 1) {
        waitUntilWorkflowFinishes(timeout, numberOfProcesses)
        ensureThatWorkflowHasNotFailed()
    }

    void waitUntilWorkflowFinishes(Duration timeout, int numberOfProcesses = 1) {
        println "Started to wait (until workflow is finished or timeout)"
        long lastPrintln = 0L
        if (!ThreadUtils.waitFor({
            if (lastPrintln < System.currentTimeMillis() - 60000L) {
                println "waiting ..."
                lastPrintln = System.currentTimeMillis()
            }
            return areAllProcessesFinished(numberOfProcesses)
        }, timeout.millis, 1000L)) {
            throw new TimeoutException("Workflow did not finish within ${PeriodFormat.default.print(timeout.toPeriod())}.")
        }
    }

    void ensureThatWorkflowHasNotFailed() {
        Collection<ProcessingStepUpdate> failureProcessingStepUpdates = ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE)
        failureProcessingStepUpdates.each {
            println "ProcessingStep ${it.processingStep.id} failed."
            if (it.error) {
                println 'Error message:'
                println it.error.errorMessage
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
            throw new RuntimeException("There were ${failureProcessingStepUpdates.size()} failures. Details have been written to standard output. See the test report or run grails test-app -echoOut.")
        }
    }

    boolean areAllProcessesFinished(int numberOfProcesses) {
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
            File workflowDirectory = new File("${rootDirectory}", "${nonQualifiedClassName}")
            // FIXME: This should be replaced when we updated to JDK7, finally: OTP-1502
            baseDirectory = uniqueDirectory(workflowDirectory)
        }
        return baseDirectory
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
    private String getRootDirectory() {
        return grailsApplication.config.otp.testing.workflows.rootdir
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
        return new File(root, "tmp-${HelperUtils.getUniqueString()}")
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
}
