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
package de.dkfz.tbi.otp

/**
 * The possible state for {@link InformationReliability}
 */
enum InformationReliability {
    /**
     * Indicates, that the {@link LibraryPreparationKit} is known and present
     */
    KNOWN,
    /**
     * Indicates, that the {@link LibraryPreparationKit} is verified to be unknown
     */
    UNKNOWN_VERIFIED("UNKNOWN"),
    /**
     * Indicates, that {@link LibraryPreparationKit} is not available yet and must be asked for.
     * The value is defined for migration of old data, where the {@link LibraryPreparationKit} was not asked for.
     */
    UNKNOWN_UNVERIFIED,
    /**
     * Indicates that OTP inferred this {@link LibraryPreparationKit} from a different lane, where it was explicitly {@link #KNOWN}.
     * This {@link LibraryPreparationKit} should not be relied upon (find the original instead, don't infer based on inferences!)
     */
    INFERRED

    /**
     * The raw value of the information reliability.
     * <p>
     * This defaults to the Enum {@link #name()}, but in case of {@link #UNKNOWN_VERIFIED}
     * it returns "UNKNOWN", which is the raw value as it appears in external input files
     */
    final String rawValue

    private InformationReliability() {
        this.rawValue = name()
    }

    private InformationReliability(String value) {
        this.rawValue = value
    }
}
