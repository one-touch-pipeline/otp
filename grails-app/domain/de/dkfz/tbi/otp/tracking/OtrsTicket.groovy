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
package de.dkfz.tbi.otp.tracking

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class OtrsTicket implements Commentable, Entity {

    @TupleConstructor
    enum ProcessingStep {
        INSTALLATION('installation', 'installed', null, null, null),
        FASTQC('FastQC', null, null, null, INSTALLATION),
        ALIGNMENT('alignment', 'aligned', null, null, INSTALLATION),
        SNV('SNV calling', 'SNV-called', 'snv', 'results', ALIGNMENT),
        INDEL('Indel calling', 'Indel-called', 'indel', 'results', ALIGNMENT),
        SOPHIA('SV calling', 'SV-called', 'sophia', 'results', ALIGNMENT),
        ACESEQ('CNV calling', 'CNV-called', 'aceseq', 'results', SOPHIA),
        RUN_YAPSA('YAPSA signature analysis', 'YAPSA-analysed', "runYapsa", "results", SNV),

        final String displayName
        /**
         * Will be used in the subject of notification e-mails: "sequencing data ${notificationSubject}"
         *
         * {@code null} means that no notification shall be sent for this step
         */
        final String notificationSubject

        final String controllerName
        final String actionName

        final ProcessingStep dependsOn

        /**
         * Converts UPPER_SNAKE_CASE "ENUM_NAMES" to camelCase "fieldNames".
         *
         * This is needed by the {@link NotificationCreator},
         * which does some stringly-typed magic to dynamically determine which field to set.
         */
        @Override
        String toString() {
            return name().toLowerCase(Locale.ENGLISH).replaceAll("(_)([a-z0-9])") { List<String> it ->
                it[2].toString().toUpperCase()
            }
        }
    }

    String ticketNumber

    /**
     * Timestamp of the earliest "Submission Received Notice" mail for an ILSe submission belonging to this ticket.
     */
    Date submissionReceivedNotice
    /**
     * Timestamp when the ticket gets created in OTRS.
     */
    Date ticketCreated

    Date installationStarted
    Date installationFinished

    Date fastqcStarted
    Date fastqcFinished

    Date alignmentStarted
    Date alignmentFinished

    Date snvStarted
    Date snvFinished

    Date indelStarted
    Date indelFinished

    Date sophiaStarted
    Date sophiaFinished

    Date aceseqStarted
    Date aceseqFinished

    Date runYapsaStarted
    Date runYapsaFinished

    boolean finalNotificationSent = false
    boolean automaticNotification = true

    String seqCenterComment

    static constraints = {
        ticketNumber(nullable: false, unique: true, validator: { val, obj ->
            return ticketNumberConstraint(val) ?: true
        })

        submissionReceivedNotice(nullable: true)
        ticketCreated(nullable: true)

        installationStarted(nullable: true)
        installationFinished(nullable: true)

        fastqcStarted(nullable: true)
        fastqcFinished(nullable: true)

        alignmentStarted(nullable: true)
        alignmentFinished(nullable: true)

        snvStarted(nullable: true)
        snvFinished(nullable: true)

        indelStarted(nullable: true)
        indelFinished(nullable: true)

        sophiaStarted(nullable: true)
        sophiaFinished(nullable: true)

        aceseqStarted(nullable: true)
        aceseqFinished(nullable: true)

        runYapsaStarted(nullable: true)
        runYapsaFinished(nullable: true)

        comment(nullable: true)
        seqCenterComment(nullable: true)
    }

    static mapping = {
        seqCenterComment type: "text"
        comment cascade: "all-delete-orphan"
    }

    static String ticketNumberConstraint(String val) {
        return val =~ /^[0-9]+$/ ? null : "does not match the required pattern"
    }

    @Override
    String toString() {
        return "OtrsTicket ${id}: #${ticketNumber}"
    }
}
