package workflows

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

import java.util.concurrent.TimeoutException

import org.joda.time.Duration
import org.joda.time.format.PeriodFormat

import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest
import de.dkfz.tbi.otp.utils.ThreadUtils

abstract class AbstractWorkflowTest extends GroovyScriptAwareIntegrationTest {

    ErrorLogService errorLogService

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
            throw new TimeoutException("Workflow did not finish within ${PeriodFormat.default.print(timeout.toPeriod())} milliseconds.")
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
        return processes.size() == numberOfProcesses && processes.every { it.refresh(); it.finished }
    }
}
