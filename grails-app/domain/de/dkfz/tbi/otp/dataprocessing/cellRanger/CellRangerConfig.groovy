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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeIndex
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * @deprecated class is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
 */
@Deprecated
@ManagedEntity
class CellRangerConfig extends ConfigPerProjectAndSeqType implements AlignmentConfig {

    boolean autoExec
    ReferenceGenomeIndex referenceGenomeIndex
    Integer expectedCells
    Integer enforcedCells

    static constraints = {
        obsoleteDate validator: { val, obj ->
            if (!val) {
                CellRangerConfig cellRangerConfig = atMostOneElement(CellRangerConfig.findAllWhere(
                        project: obj.project,
                        seqType: obj.seqType,
                        pipeline: obj.pipeline,
                        obsoleteDate: null,
                ))
                return !cellRangerConfig || cellRangerConfig == obj
            }
        }
        autoExec validator: { val, obj ->
            if (!val && (obj.referenceGenomeIndex || obj.expectedCells || obj.enforcedCells)) {
                return false
            }
        }
        referenceGenomeIndex nullable: true
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
    }

    @Override
    @Deprecated
    AlignmentInfo getAlignmentInformation() {
        return new SingleCellAlignmentInfo(alignmentProgram: "cellranger", alignmentParameter: "", programVersion: programVersion)
    }
}
