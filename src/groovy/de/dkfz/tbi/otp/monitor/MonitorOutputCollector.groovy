package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*

class MonitorOutputCollector {

    final static String INDENT = "    "
    final static String INDENT2 = "${INDENT}${INDENT}"
    final static String INDENT3 = "${INDENT2}${INDENT}"

    public static final String HEADER_NOT_TRIGGERED = 'The following objects are not triggered'
    public static final String HEADER_SHOULD_START = 'The following objects needs processing'
    public static final String HEADER_WAITING = 'The following objects are waiting'
    public static final String HEADER_RUNNING = 'The following objects are in processing'
    public static final String HEADER_FINISHED = 'The following objects are finished'
    public static final String HEADER_NOT_SUPPORTED_SEQTYPES = 'The following SeqTypes are unsupported by this workflow'


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
            long occupiedSlots = Process.countByFinishedAndJobExecutionPlan(false, JobExecutionPlan.findByName(workflowName))
            long totalSlots = ProcessingOptionService.findOptionAsNumber(ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null, 0)
            long fastTrackSlots = ProcessingOptionService.findOptionAsNumber(ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null, 1)
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

    private boolean checkProcessesForObject(String workflow, List noProcess, List processWithError, Object object, Closure valueToShow, Closure extractObjectToCheck) {
        Object objectToCheck = extractObjectToCheck(object)

        def processes = ProcessParameter.findAllByValue(objectToCheck.id, [sort: "id"])*.process.findAll {
            it.jobExecutionPlan.name == workflow
        }
        if (processes.size() == 1) {
            //normal case, no output needed
        } else if (processes.size() == 0) {
            output << "${INDENT}Attention: no process was created for the object ${valueToShow(object)} (${object.id})"
            output << "${INDENT}Please inform a maintainer"
            noProcess << object.id
            return true
        } else if (processes.size() > 1) {
            output << "${INDENT}Attention: There were ${processes.size()} processes created for the object ${valueToShow(object)} (${object.id}). That can cause problems."
        }
        Process lastProcess = processes.sort { it.id }.last()
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

