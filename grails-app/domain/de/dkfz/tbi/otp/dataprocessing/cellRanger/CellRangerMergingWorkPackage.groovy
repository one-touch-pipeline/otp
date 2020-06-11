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
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeIndex
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

class CellRangerMergingWorkPackage extends MergingWorkPackage {

    enum Status {
        FINAL,
        DELETED,
        UNSET,
    }

    Integer expectedCells
    Integer enforcedCells
    ReferenceGenomeIndex referenceGenomeIndex
    CellRangerConfig config
    Status status = Status.UNSET
    Date informed
    User requester

    static constraints = {
        sample(validator: { val, obj ->
            Map properties = [
                sample               : val,
                seqType              : obj.seqType,
                expectedCells        : obj.expectedCells,
                enforcedCells        : obj.enforcedCells,
                referenceGenomeIndex : obj.referenceGenomeIndex,
            ]
            List<CellRangerMergingWorkPackage> workPackages = findAllWhere(properties)
            if (workPackages.size() > 1 || workPackages && workPackages.first().id != obj.id) {
                return ["unique.combination", properties.collect { k, v -> "${k}: ${v}" }.join(", ")]
            }
        })
        expectedCells(nullable: true, validator: { val, obj ->
            if (val != null && obj.enforcedCells != null) {
                return "nand"
            }
        })
        enforcedCells(nullable: true, validator: { val, obj ->
            if (val != null && obj.expectedCells != null) {
                return "nand"
            }
        })
        referenceGenomeIndex(validator: { val, obj ->
            if (val.referenceGenome != obj.referenceGenome) {
                return "sync"
            }
        })
        config(nullable: true)
        status(validator: { val , obj ->
            if (val && val == CellRangerMergingWorkPackage.Status.FINAL) {
                CellRangerMergingWorkPackage crmwp = CollectionUtils.atMostOneElement(
                        CellRangerMergingWorkPackage.findAllBySampleAndSeqTypeAndConfigAndReferenceGenomeIndexAndStatus(
                                obj.sample,
                                obj.seqType,
                                obj.config,
                                obj.referenceGenomeIndex,
                                CellRangerMergingWorkPackage.Status.FINAL
                        )
                )
                if (crmwp && crmwp != obj) {
                    return [
                            "unique.combination",
                            "sample: ${obj.sample}, seq type: ${obj.seqType}, config: ${obj.config}, referenceGenomeIndex: ${obj.referenceGenomeIndex}, status: ${CellRangerMergingWorkPackage.Status.FINAL}"
                    ]
                }
            }
        })
        informed(nullable: true)
        requester(nullable: true)
    }

    @Override
    String toString() {
        return "CRMWP ${id}: ${toStringWithoutIdAndPipeline()} ${pipeline?.name} ${referenceGenomeIndex}"
    }
}
