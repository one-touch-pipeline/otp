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
 * Projected data of a single alignment output {@link de.dkfz.tbi.otp.dataprocessing.AbstractBamFile}, holding
 * exactly the scalar fields needed to build the GUI URLs and the merged alignment file patterns.
 *
 * The alignment notification text is derived from the input seq tracks (see {@link SampleNotificationRow}), so
 * this row is the denormalized counterpart carrying the project and seq type data of the produced BAM file.
 */
@Immutable
class AlignmentBamNotificationRow {

    Long projectId

    String projectName

    Long seqTypeId

    String seqTypeDirName

    boolean hasAntibodyTarget

    String libraryLayoutDirName
}
