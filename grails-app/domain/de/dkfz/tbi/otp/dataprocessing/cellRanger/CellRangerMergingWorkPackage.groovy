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

package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.utils.CollectionUtils

class CellRangerMergingWorkPackage extends MergingWorkPackage {

    int expectedCells
    Integer enforcedCells
    CellRangerConfig config

    static constraints = {
        sample(validator: { val, obj ->
            CellRangerMergingWorkPackage cellRangerMergingWorkPackage = CollectionUtils.atMostOneElement(
                    CellRangerMergingWorkPackage.findAllBySampleAndSeqTypeAndExpectedCellsAndEnforcedCells(
                            val, obj.seqType, obj.expectedCells, obj.enforcedCells),
                    "More than one MWP exists for sample ${val}, " +
                            "seqType ${obj.seqType}, " +
                            "expectedCells ${obj.expectedCells} and enforcedCells ${obj.enforcedCells}")
            if (cellRangerMergingWorkPackage && cellRangerMergingWorkPackage.id != obj.id) {
                return "The CellRangerMergingWorkPackage must be unique for one sample and seqType, expectedCells and enforcedCells"
            }
        })
        enforcedCells(nullable: true)
        config(nullable: true)
    }

    @Override
    String toString() {
        return "CRMWP ${id}: ${toStringWithoutIdAndPipeline()} ${pipeline?.name}"
    }
}
