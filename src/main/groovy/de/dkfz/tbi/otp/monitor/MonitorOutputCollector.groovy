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
package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*

class MonitorOutputCollector {

    final static String INDENT = "    "
    final static String INDENT2 = "${INDENT}${INDENT}"
    final static String INDENT3 = "${INDENT2}${INDENT}"

    static final String HEADER_NOT_TRIGGERED = 'The following objects are not triggered'
    static final String HEADER_SHOULD_START = 'The following objects needs processing'
    static final String HEADER_WAITING = 'The following objects are waiting'
    static final String HEADER_RUNNING = 'The following objects are in processing'
    static final String HEADER_FINISHED = 'The following objects are finished'
    static final String HEADER_NOT_SUPPORTED_SEQTYPES = 'The following SeqTypes are unsupported by this workflow'


    final boolean showFinishedEntries

    final boolean showNotSupportedSeqTypes


    private List<String> output = []


    MonitorOutputCollector(boolean showFinishedEntries = false, boolean showNotSupportedSeqTypes = false) {
        this.showFinishedEntries = showFinishedEntries
        this.showNotSupportedSeqTypes = showNotSupportedSeqTypes
    }

    MonitorOutputCollector leftShift(Object value) {
        output << value?.toString()
        return this
    }


    String getOutput() {
        output.join('\n')
    }


    String prefix(String text, String prefix = INDENT) {
        return "${prefix}${text.replace('\n', "\n${prefix}")}"
    }

    List<String> objectsToStrings(Collection objects, Closure valueToShow = { it }) {
        return objects.collect {
            valueToShow(it)?.toString()
        }.sort {
            it
        }
    }

    void showWorkflow(String workflowName, boolean withSlots = true) {
        output << '\n' << workflowName
        if (withSlots) {
            List<JobExecutionPlan> jobExecutionPlans = JobExecutionPlan.findAllByName(workflowName)
            long occupiedSlots = Process.countByFinishedAndJobExecutionPlanInList(false, jobExecutionPlans)
            long totalSlots = ProcessingOptionService.findOptionAsNumber(ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null)
            long fastTrackSlots = ProcessingOptionService.findOptionAsNumber(
                    ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null
            )
            long normalSlots = totalSlots - fastTrackSlots
            output << "${INDENT}Used Slots: ${occupiedSlots}, Normal priority slots: ${normalSlots}, additional fasttrack slots: ${fastTrackSlots}"
        }
        output << ''
    }


    void showList(String name, List objects, Closure valueToShow = { it }) {
        if (!objects) {
            return
        }

        output << prefix("""\
${name} (${objects.size()}):
${prefix(objectsToStrings(objects, valueToShow).join('\n'))}
""")
    }

    void showUniqueList(String name, List objects, Closure valueToShow = { it }) {
        if (!objects) {
            return
        }
        showList(name, objectsToStrings(objects, valueToShow).groupBy {
            it
        }.collect { key, value ->
            "${key}  ${value.size() == 1 ? '' : "(count: ${value.size()})"}"
        })
    }


    void showUniqueNotSupportedSeqTypes(List objects, Closure valueToShow = { it }) {
        if (showNotSupportedSeqTypes) {
            showUniqueList(HEADER_NOT_SUPPORTED_SEQTYPES, objects, valueToShow)
        }
    }

    void showNotTriggered(List objects, Closure valueToShow = { it }) {
        showList(HEADER_NOT_TRIGGERED, objects, valueToShow)
    }

    void showShouldStart(List objects, Closure valueToShow = { it }) {
        showList(HEADER_SHOULD_START, objects, valueToShow)
    }

    void showWaiting(List objects, Closure valueToShow = { it }) {
        showList(HEADER_WAITING, objects, valueToShow)
    }

    void showRunning(String workflow, List objects,
                     Closure valueToShow = { it as String },
                     Closure objectToCheck = { it }) {
        showRunningWithHeader(HEADER_RUNNING, workflow, objects, valueToShow, objectToCheck)
    }

    void showRunningWithHeader(String header, String workflow, List objects,
                               Closure valueToShow = { it as String },
                               Closure objectToCheck = { it }) {
        showList(header, objects, valueToShow)
        addInfoAboutProcessErrors(workflow, objects, valueToShow, objectToCheck)
    }

    void showFinished(List objects, Closure valueToShow = { it }) {
        if (showFinishedEntries) {
            showList(HEADER_FINISHED, objects, valueToShow)
        }
    }


    void addInfoAboutProcessErrors(String workflow, Collection<Object> objects, Closure valueToShow, Closure objectToCheck) {
        if (!objects) {
            return
        }
        int errorCount = 0
        List noProcess = []
        List processWithError = []
        objects.sort { valueToShow(it) }.each {
            if (checkProcessesForObject(workflow, noProcess, processWithError, it, valueToShow, objectToCheck)) {
                errorCount++
            }
        }
        if (errorCount) {
            output << "\n${INDENT}Count of errors: ${errorCount}"
        }
        if (noProcess) {
            output << "\n${INDENT}objects without process: ${noProcess}"
        }
        if (processWithError) {
            output << "\n${INDENT}objects with error: ${processWithError.join(', ')}"
        }
        output << ''
    }

    private boolean checkProcessesForObject(
            String workflow, List noProcess, List processWithError, Object object, Closure valueToShow, Closure extractObjectToCheck
    ) {
        Object objectToCheck = extractObjectToCheck(object)

        def processes = ProcessParameter.findAllByValue(objectToCheck.id, [sort: "id"])*.process.findAll {
            it.jobExecutionPlan.name == workflow
        }

        //if processes.size() is 1, then nothing should be done
        if (processes.size() == 0) {
            output << "${INDENT}Attention: no process was created for the object ${valueToShow(object)} (${object.id})"
            output << "${INDENT}Please inform a maintainer"
            noProcess << object.id
            return true
        } else if (processes.size() > 1) {
            output << "${INDENT}Attention: There were ${processes.size()} processes created for the object ${valueToShow(object)} (${object.id}). That can cause problems."
        }
        Process lastProcess = processes.max { it.id }
        ProcessingStep ps = ProcessingStep.findByProcessAndNextIsNull(lastProcess)
        ProcessingStepUpdate update = ps.latestProcessingStepUpdate
        def state = update?.state
        if (state == ExecutionState.FAILURE || update == null) {
            output << prefix("An error occur for the object: ${valueToShow(object)}")
            List errorOutput = []
            errorOutput << """\
object class/id: ${object.class} / ${object.id}
the OTP link: https://otp.dkfz.de/otp/processes/process/${lastProcess.id}
the error: ${ps.latestProcessingStepUpdate?.error?.errorMessage?.replaceAll('\n', "\n${INDENT3}")}"""

            Comment comment = ps.process.comment
            if (comment) {
                errorOutput << "the comment (${comment.modificationDate.format("yyyy-MM-dd")} by ${comment.author}): ${ps.process.comment.comment.replaceAll('\n', "\n${INDENT3}")}"
            }
            if (update == null) {
                errorOutput << "no update available: Please inform a maintainer\n"
            }
            output << prefix(errorOutput.join('\n'), INDENT2)
            processWithError << object.id
            return true
        }
        return false
    }
}

