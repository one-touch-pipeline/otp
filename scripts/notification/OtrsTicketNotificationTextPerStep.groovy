import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*



String ticketNumber = ''

//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.INSTALLATION
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.ALIGNMENT
//OtrsTicket.ProcessingStep processingStep = OtrsTicket.ProcessingStep.SNV



//---------------------------------

TrackingService trackingService = ctx.trackingService
CreateNotificationTextService notificationTextService = ctx.createNotificationTextService

OtrsTicket otrsTicket = exactlyOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))


println notificationTextService.notification(otrsTicket,
        trackingService.getProcessingStatus(otrsTicket.findAllSeqTracks()), processingStep)

''
