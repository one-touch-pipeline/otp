/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.project.dta

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.Entity

import java.text.DateFormat
import java.text.SimpleDateFormat

class DataTransfer implements Entity {

    /**
     * Which medium was used to transport the data?
     */
    enum TransferMode {
        /** a network transfer via a central Aspera(tm) server. */
        ASPERA,
        /** physical disks were posted, sneakernet survives into the next millenium! */
        HARD_DISK,
        /** Data was copied to other storage inside the local institute, but outside of OTP control. */
        INTERNAL,
    }

    @TupleConstructor
    enum Direction {
        /** this transfer is into the local institution, for import into OTP. */
        INCOMING("from"),
        /** this transfer moves data out of OTP to somewhere else. */
        OUTGOING("to"),

        /** English word to use in a sentence for a transfer in this direction */
        final String adjective
    }

    /**
     * The DTA document authorising this transfer.
     */
    DataTransferAgreement dataTransferAgreement

    Set<DataTransferDocument> dataTransferDocuments

    /** Person asking for the data to be moved */
    String requester
    /** reference number for this transfer in external helpdesk ticket system / request tracker */
    String ticketID
    /** OTP staff doing the practical bit-moving */
    User performingUser

    Direction direction

    TransferMode transferMode

    String peerPerson
    String peerAccount

    /** Date that transfer was first made possible, e.g. shipping date of hard disk, or when data was copied to transfer server. */
    Date transferDate
    /** Date that transfer was last possible (if applicable), e.g. when data was removed from transfer server. */
    Date completionDate

    String comment

    static hasMany = [
            dataTransferDocuments: DataTransferDocument,
    ]

    static belongsTo = [
            dataTransferAgreement: DataTransferAgreement,
    ]

    static mappedBy = [
            dataTransferDocuments: "dataTransfer",
    ]

    @Override
    String toString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd" , Locale.ENGLISH)

        return "Transfer by ${performingUser} ${direction.adjective} " +
                "${peerPerson}${peerAccount ? " (${peerAccount})" : ""}" +
                ", via ${transferMode};\n" +

                "(started ${ dateFormat.format(transferDate) }" +
                "${completionDate ? " until ${dateFormat.format(completionDate)}" : ", ongoing"}" +
                ");\n" +

                " requested by ${requester}" +
                "${ticketID ? " in ${ticketID}." : ""}" +

                "\nNotes: ${comment ?: "n/a"}"
    }

    static Closure constraints = {
        completionDate nullable: true
        peerAccount blank: false, nullable: true
        ticketID blank: false, nullable: true
        comment blank: false, nullable: true
    }

    static Closure mapping = {
        comment type: "text"
    }

    boolean isTicketLinkable() {
        return OtrsTicket.ticketNumberConstraint(ticketID) == null
    }

    String getTicketLink() {
        return ticketLinkable ? OtrsTicketService.buildTicketDirectLink(ticketID) : ""
    }
}
