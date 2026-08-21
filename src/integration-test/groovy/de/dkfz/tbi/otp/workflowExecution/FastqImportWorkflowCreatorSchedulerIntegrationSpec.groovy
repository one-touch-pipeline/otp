/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService

@Rollback
@Integration
class FastqImportWorkflowCreatorSchedulerIntegrationSpec extends Specification implements DomainFactoryCore {

    FastqImportWorkflowCreatorScheduler fastqImportWorkflowCreatorScheduler

    @Autowired
    TicketService ticketService

    final static String PREFIX = "TICKET_PREFIX"

    ProcessingOption setupBlacklistImportSourceNotificationProcessingOption(String blacklist) {
        return DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.BLACKLIST_IMPORT_SOURCE_NOTIFICATION,
                type: null,
                value: blacklist,
        )
    }

    void "sendImportSourceOperatorNotification, no blacklisted paths"() {
        given:
        DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)
        setupBlacklistImportSourceNotificationProcessingOption("")

        Ticket ticket = createTicket()

        RawSequenceFile rawSequenceFile = createFastqFile()
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [rawSequenceFile])

        String expectedHeader = "Import source ready for deletion [${ticketService.getPrefixedTicketNumber(ticket)}]"
        String expectedEnd = "rm -f ${rawSequenceFile.fullInitialPath}"

        fastqImportWorkflowCreatorScheduler.mailHelperService = Mock(MailHelperService)

        when:
        fastqImportWorkflowCreatorScheduler.sendImportSourceOperatorNotification(fastqImportInstance)

        then:
        1 * fastqImportWorkflowCreatorScheduler.mailHelperService.saveMail(expectedHeader) { it.endsWith(expectedEnd) }
    }

    void "sendImportSourceOperatorNotification, sends only the paths of the given import, not those of other imports of the same ticket"() {
        given:
        DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)
        setupBlacklistImportSourceNotificationProcessingOption("")

        Ticket ticket = createTicket()

        List<RawSequenceFile> rawSequenceFilesA = [createFastqFile(), createFastqFile()]
        FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesA)

        List<RawSequenceFile> rawSequenceFilesB = [createFastqFile()]
        createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesB)

        String expectedHeader = "Import source ready for deletion [${ticketService.getPrefixedTicketNumber(ticket)}]"
        List<String> pathsA = rawSequenceFilesA*.fullInitialPath
        List<String> pathsB = rawSequenceFilesB*.fullInitialPath

        fastqImportWorkflowCreatorScheduler.mailHelperService = Mock(MailHelperService)

        when:
        fastqImportWorkflowCreatorScheduler.sendImportSourceOperatorNotification(fastqImportInstanceA)

        then:
        1 * fastqImportWorkflowCreatorScheduler.mailHelperService.saveMail(expectedHeader) { String content ->
            pathsA.every { content.contains("rm -f ${it}" as String) } && pathsB.every { !content.contains(it) }
        }
    }

    void "sendImportSourceOperatorNotification, does not send a mail when all paths are filtered out"() {
        given:
        DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)

        String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("${File.separator}blacklisted").value

        Ticket ticket = createTicket()

        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                createFastqFile(initialDirectory: "${blacklisted}"),
                createFastqFile(initialDirectory: "${blacklisted}"),
        ])

        fastqImportWorkflowCreatorScheduler.mailHelperService = Mock(MailHelperService)

        when:
        fastqImportWorkflowCreatorScheduler.sendImportSourceOperatorNotification(fastqImportInstance)

        then:
        0 * fastqImportWorkflowCreatorScheduler.mailHelperService.saveMail(_, _)
    }

    void "getPathsToDelete returns the sequence file paths of the given import"() {
        given:
        Ticket ticket = createTicket()

        List<RawSequenceFile> rawSequenceFiles = [createFastqFile(), createFastqFile()]
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFiles)

        expect:
        fastqImportWorkflowCreatorScheduler.getPathsToDelete(fastqImportInstance).sort() == rawSequenceFiles*.fullInitialPath.sort()
    }

    void "getPathsToDelete returns only the given import's paths, not those of other imports of the same ticket"() {
        given:
        Ticket ticket = createTicket()

        List<RawSequenceFile> rawSequenceFilesA = [createFastqFile(), createFastqFile()]
        FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesA)

        List<RawSequenceFile> rawSequenceFilesB = [createFastqFile(), createFastqFile(), createFastqFile()]
        createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesB)

        expect:
        fastqImportWorkflowCreatorScheduler.getPathsToDelete(fastqImportInstanceA).sort() == rawSequenceFilesA*.fullInitialPath.sort()
    }

    void "getPathsToDelete leaves out blacklisted paths"() {
        given:
        String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("${File.separator}blacklisted").value

        Ticket ticket = createTicket()

        Closure<RawSequenceFile> createBlacklistedRawSequenceFile = {
            createFastqFile(initialDirectory: "${blacklisted}${File.separator}path${File.separator}dataFile")
        }
        List<RawSequenceFile> allowedRawSequenceFiles = [createFastqFile(), createFastqFile()]
        List<RawSequenceFile> blacklistedRawSequenceFiles = [createBlacklistedRawSequenceFile(), createBlacklistedRawSequenceFile()]
        FastqImportInstance fastqImportInstance = createFastqImportInstance(
                ticket: ticket, sequenceFiles: allowedRawSequenceFiles + blacklistedRawSequenceFiles)

        expect:
        fastqImportWorkflowCreatorScheduler.getPathsToDelete(fastqImportInstance).sort() == allowedRawSequenceFiles*.fullInitialPath.sort()
    }

    void "getPathsToDelete returns empty list if all paths are blacklisted"() {
        given:
        String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("${File.separator}blacklisted").value

        Ticket ticket = createTicket()

        Closure<RawSequenceFile> createBlacklistedRawSequenceFile = {
            createFastqFile(initialDirectory: "${blacklisted}${File.separator}path${File.separator}dataFile")
        }
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                createBlacklistedRawSequenceFile(),
                createBlacklistedRawSequenceFile(),
        ])

        expect:
        fastqImportWorkflowCreatorScheduler.getPathsToDelete(fastqImportInstance) == []
    }
}
