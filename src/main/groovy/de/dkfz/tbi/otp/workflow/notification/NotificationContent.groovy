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
package de.dkfz.tbi.otp.workflow.notification

import groovy.transform.Immutable

/**
 * Immutable holder for the content sections of a single workflow's notification, built in one step via
 * {@link WorkflowNotification#buildContent} from a single projection of the workflow runs.
 */
@Immutable
class NotificationContent {

    /** the texts representing the data of the workflow runs, used in the mails */
    Set<String> notificationTexts

    /** the URLs of the GUI where the corresponding data is shown, including project and, where applicable, seqType */
    Set<String> guiUrls

    /**
     * the patterns where the files are located on the file system. PID and seqType are kept as variables
     * ({@code ${PID}}, {@code ${SEQUENCING_TYPE_DIR}}, ...) in the returned values.
     */
    Set<String> filePatterns
}
