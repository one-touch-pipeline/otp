/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.otp.utils.MessageSourceService

/**
 * service to create the cell for the {@link AbstractMergedBamFile.QcTrafficLightStatus}
 */
@Transactional(readOnly = true)
class QcStatusCellService {

    MessageSourceService messageSourceService

    /**
     * Generate the QC Status info in a DataTable cell format.
     *
     * @param qcStatusMap containing the infos to display
     * @return TableCellValue for the DataTable
     */
    TableCellValue generateQcStatusCell(Map<String, ?> qcStatusMap) {
        TableCellValue.Icon icon = [
                (AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED)  : TableCellValue.Icon.WARNING,
                (AbstractMergedBamFile.QcTrafficLightStatus.WARNING)  : TableCellValue.Icon.WARNING,
                (AbstractMergedBamFile.QcTrafficLightStatus.REJECTED) : TableCellValue.Icon.ERROR,
                (AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED): TableCellValue.Icon.NA,
        ].getOrDefault(qcStatusMap.qcStatus, TableCellValue.Icon.OKAY)

        String tooltip
        if (qcStatusMap.qcStatus == AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED) {
            tooltip = messageSourceService.createMessage("alignment.quality.not.available")
        } else {
            String comment = qcStatusMap.qcComment ? "\n${qcStatusMap.qcComment}\n${qcStatusMap.qcAuthor}" : ""
            tooltip = "Status: ${(qcStatusMap.qcStatus)} ${comment}"
        }

        return new TableCellValue([
                value    : qcStatusMap.qcComment ? qcStatusMap.qcComment : "",
                tooltip  : tooltip,
                warnColor: null,
                link     : null,
                icon     : icon,
                status   : qcStatusMap.qcStatus.toString(),
                id       : (long) qcStatusMap.bamId,
        ])
    }
}
