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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import groovy.transform.ToString

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Data dto for {@link MergingPreventionValidator}.
 */
@ToString(includeNames = true, includePackage = false)
class MergingPreventionDataDto {

    SeqType seqType
    SeqPlatform seqPlatform
    Sample sample
    AntibodyTarget antibodyTarget
    LibraryPreparationKit libraryPreparationKit
    MergingCriteria mergingCriteria
    SeqPlatformGroup seqPlatformGroupImport

    boolean filledCompletely = false

    boolean ignoreSeqPlatformGroupForMerging() {
        return mergingCriteria && mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
    }

    boolean checkLibPrepKit() {
        return mergingCriteria ? mergingCriteria.useLibPrepKit : !seqType.isWgbs()
    }

    String createMessagePrefix(boolean startLowerCase) {
        return "${startLowerCase ? 's' : 'S'}ample ${sample.displayName} with sequencing type ${seqType.displayNameWithLibraryLayout}"
    }
}
