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

import de.dkfz.tbi.otp.AbstractIntegrationSpecWithoutRollbackAnnotation
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.SessionUtils
import org.springframework.beans.factory.annotation.Autowired

class FastqImportWorkflowCreatorSchedulerIntegrationSpec extends AbstractIntegrationSpecWithoutRollbackAnnotation implements DomainFactoryCore {

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
        Ticket ticket

        SessionUtils.withTransaction {
            DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)
            setupBlacklistImportSourceNotificationProcessingOption("")

            ticket = createTicket()

            RawSequenceFile rawSequenceFile = createFastqFile()
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [rawSequenceFile])

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance)

            String prefix = ticketService.getPrefixedTicketNumber(ticket)
            String expectedHeader = "Import source ready for deletion [${prefix}]"

            String expectedEnd = [
                    rawSequenceFile.fullInitialPath,
            ].collect { "rm -f ${it}" }.join("\n")

            fastqImportWorkflowCreatorScheduler.mailHelperService = Mock(MailHelperService) {
                1 * saveMail(expectedHeader) { it.endsWith(expectedEnd) }
            }
        }

        expect:
        SessionUtils.withTransaction {
            fastqImportWorkflowCreatorScheduler.sendImportSourceOperatorNotification(ticket)
            return true
        }
    }

    void "sendImportSourceOperatorNotification, does not send a mail when all paths are filtered out"() {
        given:
        Ticket ticket

        SessionUtils.withTransaction {
            DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)

            String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("${File.separator}blacklisted").value
            ticket = createTicket()

            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                    createFastqFile(initialDirectory: "${blacklisted}"),
                    createFastqFile(initialDirectory: "${blacklisted}"),
            ])
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance, filePathSource: "${blacklisted}")

            fastqImportWorkflowCreatorScheduler.mailHelperService = Mock(MailHelperService) {
                0 * saveMail(_, _, _)
            }
        }

        expect:
        SessionUtils.withTransaction {
            fastqImportWorkflowCreatorScheduler.sendImportSourceOperatorNotification(ticket)
            return true
        }
    }

    void "getPathsToDelete returns the paths of all DataFiles associated with the ticket"() {
        given:
        Ticket ticket
        List<String> expected = []

        SessionUtils.withTransaction {
            ticket = createTicket()

            List<RawSequenceFile> rawSequenceFilesA = [createFastqFile(), createFastqFile()]
            FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesA)

            List<RawSequenceFile> rawSequenceFilesB = [createFastqFile(), createFastqFile(), createFastqFile()]
            FastqImportInstance fastqImportInstanceB = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesB)

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceA)
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceB)

            expected.addAll(rawSequenceFilesA*.fullInitialPath)
            expected.addAll(rawSequenceFilesB*.fullInitialPath)
        }

        expect:
        SessionUtils.withTransaction {
            assert fastqImportWorkflowCreatorScheduler.getPathsToDelete(ticket).sort() == expected.sort()
            return true
        }
    }

    void "getPathsToDelete leaves out blacklisted paths"() {
        given:
        Ticket ticket
        List<String> expected = []

        SessionUtils.withTransaction {
            String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("${File.separator}blacklisted").value

            ticket = createTicket()

            List<RawSequenceFile> rawSequenceFilesA = [createFastqFile(), createFastqFile()]
            FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesA)

            Closure<RawSequenceFile> createBlacklistedRawSequenceFile = {
                createFastqFile(initialDirectory: "${blacklisted}${File.separator}path${File.separator}dataFile")
            }
            List<RawSequenceFile> rawSequenceFilesB = [createFastqFile()]
            List<RawSequenceFile> rawSequenceFilesBBlacklisted = [createBlacklistedRawSequenceFile(), createBlacklistedRawSequenceFile()]
            FastqImportInstance fastqImportInstanceB = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesB + rawSequenceFilesBBlacklisted)

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceA, filePathSource: "${blacklisted}${File.separator}path${File.separator}metaDataFile")
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceB)

            expected.addAll(rawSequenceFilesA*.fullInitialPath)
            expected.addAll(rawSequenceFilesB*.fullInitialPath)
        }

        expect:
        SessionUtils.withTransaction {
            assert fastqImportWorkflowCreatorScheduler.getPathsToDelete(ticket).sort() == expected.sort()
            return true
        }
    }

    void "getPathsToDelete returns empty list if all paths are blacklisted"() {
        given:
        Ticket ticket

        SessionUtils.withTransaction {
            String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("${File.separator}blacklisted").value

            ticket = createTicket()

            Closure<RawSequenceFile> createBlacklistedRawSequenceFile = {
                createFastqFile(initialDirectory: "${blacklisted}${File.separator}path${File.separator}dataFile")
            }

            FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                    createBlacklistedRawSequenceFile(),
            ])

            FastqImportInstance fastqImportInstanceB = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                    createBlacklistedRawSequenceFile(),
                    createBlacklistedRawSequenceFile(),
            ])

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceA, filePathSource: "${blacklisted}${File.separator}path${File.separator}metaDataFile")
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceB, filePathSource: "${blacklisted}${File.separator}path${File.separator}metaDataFile")
        }

        expect:
        SessionUtils.withTransaction {
            assert fastqImportWorkflowCreatorScheduler.getPathsToDelete(ticket).sort() == []
            return true
        }
    }
}
