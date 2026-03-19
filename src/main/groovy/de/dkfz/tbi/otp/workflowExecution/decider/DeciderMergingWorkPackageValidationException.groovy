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
package de.dkfz.tbi.otp.workflowExecution.decider

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentDeciderGroup

/**
 * Exception thrown when a MergingWorkPackage validation fails in the decider.
 * This occurs when an existing MergingWorkPackage has different properties
 * than what is expected from the current SeqTrack being processed.
 */
class DeciderMergingWorkPackageValidationException extends DeciderException {

    final Map<String, Entity> nonMatchingProperties
    final AlignmentDeciderGroup group
    final MergingWorkPackage workPackage
    final boolean sendsUnalignableSeqTrackEmail

    DeciderMergingWorkPackageValidationException(String message,
                                                 Map<String, Entity> nonMatchingProperties,
                                                 AlignmentDeciderGroup group,
                                                 MergingWorkPackage workPackage,
                                                 boolean sendsUnalignableSeqTrackEmail = false) {
        super(message)
        this.nonMatchingProperties = nonMatchingProperties
        this.group = group
        this.workPackage = workPackage
        this.sendsUnalignableSeqTrackEmail = sendsUnalignableSeqTrackEmail
    }

    /**
     * Convenience constructor that builds a standard message
     */
    DeciderMergingWorkPackageValidationException(Map<String, Entity> nonMatchingProperties,
                                                 AlignmentDeciderGroup group,
                                                 MergingWorkPackage workPackage,
                                                 boolean sendsUnalignableSeqTrackEmail = false) {
        this(buildMessage(nonMatchingProperties, group, workPackage),
                nonMatchingProperties, group, workPackage, sendsUnalignableSeqTrackEmail)
    }

    private static String buildMessage(Map<String, Entity> nonMatchingProperties, AlignmentDeciderGroup group, MergingWorkPackage workPackage) {
        String nonMatchingString = nonMatchingProperties.collect { String key, Entity value ->
            [
                    key,
                    "- workPackage: ${workPackage[key]}",
                    "- seqTrack:    ${value}",
            ].join('\n')
        }.join('\n')

        return "existing MergingWorkPackage and Lanes do not match for ${group}\n${nonMatchingString}"
    }
}
