/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.ProcessingStatus
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

// input area
/*
Please provide the Ticket number and the project you want to inform.
In case you want to have all processing information please set all = true.
In case you want to inform for one specific processing step comment in the required ProcessingStep
Above the notification text also the email addresses of the project members are provided.
*/
String ticketNumber = ""
String projectName = ""
boolean all = false

//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.INSTALLATION
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.FASTQC
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.ALIGNMENT
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.SNV
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.INDEL
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.SOPHIA
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.ACESEQ
//Ticket.ProcessingStep processingStep = Ticket.ProcessingStep.RUN_YAPSA

// ---------------------------------

// work area
NotificationCreator notificationCreator = ctx.notificationCreator
CreateNotificationTextService notificationTextService = ctx.createNotificationTextService
UserProjectRoleService userProjectRoleService = ctx.userProjectRoleService

Ticket ticket = exactlyOneElement(Ticket.findAllByTicketNumber(ticketNumber))

Project project = CollectionUtils.atMostOneElement(Project.findAllByName(projectName))

println "Emails:"
println userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project]).sort().join(',')
println ""

try {
    if (all) {

        ProcessingStatus status = ctx.notificationCreator.getProcessingStatus(ctx.ticketService.findAllSeqTracks(ticket))
        println ctx.createNotificationTextService.installationNotification(status).trim()
        println "\n-----------------------\n"
        println ctx.createNotificationTextService.alignmentNotification(status).trim()
        println "\n-----------------------\n"
        println ctx.createNotificationTextService.snvNotification(status).trim()
        println "\n-----------------------\n"
        println ctx.createNotificationTextService.indelNotification(status).trim()
        println "\n-----------------------\n"
        println ctx.createNotificationTextService.sophiaNotification(status).trim()
        println "\n-----------------------\n"
        println ctx.createNotificationTextService.aceseqNotification(status).trim()

    } else {
        println notificationTextService.notification(ticket,
                notificationCreator.getProcessingStatus(ctx.ticketService.findAllSeqTracks(ticket)), processingStep, project)
    }
}catch (Exception e) {
    println "Please provide the processingStep you want to notify or set all = true in case you want to notify about all steps"
}

''
