package overviews

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * This is a script to get information for the run-time statistics @ https://sharepoint.local/Shared%20Documents/Run-Time-Statistics/.
 * It allows us to get all information that are needed for the run-time statistics and that are stored in OTP.
 *
 * output:
 *
 * IlseIds: [IlseIds]
 * Projects: [Projects]
 * SeqTypes: [SeqTypes]
 * Alignment: [true/false for each Project]
 * Info from GPCF: [OTRS-Ticket]
 * Import Started: [command for execution]
 * Import Finished: [Date Import finished]
 * Alignment Started: [Date Alignment started]
 * Alignment Finished: [Date alignment finished]
 * SNV Started: [Date SNV started]
 * SNV Finished; [Date SNV finished]
 * Notification: [OTRS-Ticket]
 */

// ilse ids
String ilseIds = """
"""

// run ids, use this just in case of external data, when no ildeids are known
String runIds = """
"""

// defines how many entries will be searched through
Integer entries = 100

List<String> ilseIdList = ilseIds.split("\n") - ''
List<String> runIdList = runIds.split("\n") - ''
List<SeqTrack> seqTracks

if(ilseIdList) {
    seqTracks = SeqTrack.findAllByIlseIdInList(ilseIdList)
}
else if(runIdList) {
    List<Run> runList = runIdList.collect { Run.findByName(it) }
    seqTracks = runList.collect { SeqTrack.findAllByRun(it) }.flatten()
    ilseIdList = (seqTracks*.ilseId).unique()
}

List<Sample> samples = seqTracks*.sample
List<Project> projects = (samples*.project).unique()
List<SeqType> seqTypes = (seqTracks*.seqType).unique()

List<String> alignments = projects.collect { Project project ->
    return "${project} alignment: ${!(project.alignmentDeciderBeanName == 'noAlignmentDecider')}"
}

def bySamples = { String workflow ->
    List dates = []

    JobExecutionPlan plan = JobExecutionPlan.findByName(workflow)
    Map<Process, ProcessingStepUpdate> processes = ctx.getBean("jobExecutionPlanService").getLatestUpdatesForPlan(plan, entries)
    processes.each { Process process, ProcessingStepUpdate processingStepUpdate ->
        ProcessParameter processParameter = ProcessParameter.findByProcess(process)
        samples.each { Sample sample ->
            if(processParameter.toObject().toString().contains(sample.toString())) {
                dates << [process.started, processingStepUpdate.date]
            }
        }
    }
    if(!dates) {
        return null
    }
    return dates
}

def importStarted = {
    String folderPath = "STORAGE_ROOTSEQUENCING_INBOX"
    List list = ilseIdList.collect {
        def matcher = it =~ /(\d)\d{3}/
        return "[ll ${folderPath}/00${matcher[0][1]}/00${it}/]"
    }
    return list.join("\n")
}

def importFinished = {
    List dates = bySamples('FastqcWorkflow')
    return dates?.collect { it[1] }?.max()
}

def alignmentStarted = {
    List dates = bySamples('ConveyBwaAlignmentWorkflow')
    return dates?.collect { it[0] }?.min()
}

def alignmentFinished = {
    List dates = bySamples('transferMergedBamFileWorkflow')
    return dates?.collect { it[1] }?.max()
}

def panCanStartedAndFinished = {
    List dates = bySamples('PanCanWorkflow')
    def started = dates?.collect { it[0] }?.min()
    def ended = dates?.collect { it[1] }?.max()
    return [started, ended]
}

def snvStartedAndFinished = {
    List dates = bySamples('SnvWorkflow')
    def started = dates?.collect { it[0] }?.min()
    def ended = dates?.collect { it[1] }?.max()
    return [started, ended]
}

println "IlseIds:\n\t" + ilseIdList
println "Projects:\n\t" + projects
println "SeqTypes:\n\t" + seqTypes*.displayName
println "Alignment:\n\t" + alignments.join("\n")
println "Info from GPCF:\n\t[OTRS-Ticket]"
println "Import Started:\n\t" + importStarted()
println "Import Finished:\n\t" + importFinished()
def alignmentStartedOTP = alignmentStarted()
def alignmentFinishedOTP = alignmentFinished()
if(alignmentStartedOTP || alignmentFinishedOTP) {
    println "Alignment Started:\n\t" + alignmentStartedOTP
    println "Alignment Finished:\n\t" + alignmentFinishedOTP
} else {
    def (alignmentStartedPanCan, alignmentEndedPanCan) = panCanStartedAndFinished()
    println "Alignment Started:\n\t" + alignmentStartedPanCan
    println "Alignment Finished:\n\t" + alignmentEndedPanCan
}
def (snvStarted, snvEnded) = snvStartedAndFinished()
println "SNV Started:\n\t" + snvStarted
println "SNV Finished:\n\t" + snvEnded
println "Notification:\n\t[OTRS-Ticket]"
