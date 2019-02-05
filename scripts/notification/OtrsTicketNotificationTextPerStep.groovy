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

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

String ticketNumber = ""
String projectName = ""

//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.INSTALLATION
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.FASTQC
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.ALIGNMENT
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.SNV
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.INDEL
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.SOPHIA
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.ACESEQ
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.RUN_YAPSA

//---------------------------------

TrackingService trackingService = ctx.trackingService
CreateNotificationTextService notificationTextService = ctx.createNotificationTextService

OtrsTicket otrsTicket = exactlyOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))

Project project = Project.findByName(projectName)

println notificationTextService.notification(otrsTicket,
        trackingService.getProcessingStatus(otrsTicket.findAllSeqTracks()), processingStep, project)

''
