package overviews

import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.DateTime
import de.dkfz.tbi.otp.tracking.*

/**
 * This is a script to get information for the run-time statistics @ https://sharepoint.local/Shared%20Documents/Run-Time-Statistics/.
 *
 * output:
 *
 * IlseIds
 * Runs [names]
 * Number of Runs
 * Number of Samples
 * Projects
 * SeqTypes
 * Alignment [true/false for each Project]
 * Submission received Notice [extract from OTRS-Ticket]
 * Info from GPCF [extract from OTRS-Ticket]
 * Installation Started (Import Started in run-time statistics table)
 * Installation Finished
 * Fastqc Started
 * Fastqc Finished (Import Finished in run-time statistics table)
 * Alignment Started
 * Alignment Finished
 * SNV Started
 * SNV Finished
 * Notification: [extract from OTRS-Ticket]
 */

String otrsTicketNumber= ""

OtrsTicket otrsTicket = OtrsTicket.findByTicketNumber(otrsTicketNumber)
assert otrsTicket : "No OtrsTicket found for ticketnumber ${otrsTicketNumber}"

Set<SeqTrack> seqTracks = otrsTicket.findAllSeqTracks()
ilseIdList = (seqTracks*.ilseId).unique()
List<Sample> samples = seqTracks*.sample
List<Project> projects = (samples*.project).unique()
List<SeqType> seqTypes = (seqTracks*.seqType).unique()
List<String> alignments = projects.collect { Project project ->
    return "${project} alignment: ${!(project.alignmentDeciderBeanName == 'noAlignmentDecider')}"
}

ProcessingStatus status = ctx.trackingService.getProcessingStatus(seqTracks)

String outputString = [
        "IlseIds:\n\t${ilseIdList}",
        "Runs:\n\t${seqTracks*.run.unique()}",
        "Number of Runs:\n\t${seqTracks*.run.unique().size()}",
        "Number of Samples:\n\t${samples.unique().size()}",
        "Projects:\n\t${projects}",
        "SeqTypes:\n\t${seqTypes*.displayName}",
        "Alignment:\n\t${alignments.join("\n")}",
        "Submission received notice:\n\t[OTRS-Ticket]",
        "Info from GPCF\n\t[OTRS-Ticket]",
].join("\n") + "\n"

OtrsTicket.ProcessingStep.values().each {
    String processingStep = it.toString()
    ProcessingStatus.WorkflowProcessingStatus currentStatus = status."${processingStep}ProcessingStatus"

    ["Started": (currentStatus.done != ProcessingStatus.Done.NOTHING),
     "Finished": (currentStatus.done == ProcessingStatus.Done.ALL || currentStatus == ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_WONT_DO_MORE)
    ].each { String step, boolean exp ->

        outputString += "${processingStep} ${step}:\n\t"
        if (exp) {
            outputString += new DateTime(otrsTicket."${processingStep}${step}").toString('yyyy-MM-dd HH:mm:ss')
        } else {
            outputString += status."${processingStep}ProcessingStatus"
        }
        outputString += "\n"
    }
}

outputString += "Notification:\n\t[OTRS-Ticket]"

println outputString