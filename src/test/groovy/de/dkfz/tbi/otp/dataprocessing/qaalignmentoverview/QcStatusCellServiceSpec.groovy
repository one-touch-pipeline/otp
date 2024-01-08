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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.otp.utils.MessageSourceService

class QcStatusCellServiceSpec extends Specification implements ServiceUnitTest<QcStatusCellService>, DataTest {

    @Unroll
    void "generateQcStatusCell, when execute for status #status, then generate cell with icon #icon"() {
        given:
        Map<String, ?> qcStatusMap = [
                qcStatus : status,
                qcComment: 'comment',
                qcAuthor : 'author',
                bamId    : 1,
        ]

        when:
        TableCellValue qcCell = service.generateQcStatusCell(qcStatusMap)

        then:
        qcCell.value == 'comment'
        qcCell.tooltip == "Status: ${status} \ncomment\nauthor"
        qcCell.warnColor == null
        qcCell.link == null
        qcCell.icon == icon
        qcCell.status == status.toString()
        qcCell.id == 1

        where:
        status                                                   | icon                        || naCount
        AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET   | TableCellValue.Icon.OKAY    || 0
        AbstractBamFile.QcTrafficLightStatus.QC_PASSED     | TableCellValue.Icon.OKAY    || 0
        AbstractBamFile.QcTrafficLightStatus.ACCEPTED      | TableCellValue.Icon.OKAY    || 0
        AbstractBamFile.QcTrafficLightStatus.AUTO_ACCEPTED | TableCellValue.Icon.OKAY    || 0
        AbstractBamFile.QcTrafficLightStatus.WARNING       | TableCellValue.Icon.WARNING || 0
    }

    void "generateQcStatusCell, when execute for status UNCHECKED, then generate cell with icon NA"() {
        service.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage("alignment.quality.not.available") >> 'NA'
            0 * _
        }
        AbstractBamFile.QcTrafficLightStatus status = AbstractBamFile.QcTrafficLightStatus.UNCHECKED

        Map<String, ?> qcStatusMap = [
                qcStatus : status,
                qcComment: 'comment',
                qcAuthor : 'author',
                bamId    : 1,
        ]

        when:
        TableCellValue qcCell = service.generateQcStatusCell(qcStatusMap)

        then:
        qcCell.value == 'comment'
        qcCell.tooltip == "NA"
        qcCell.warnColor == null
        qcCell.link == null
        qcCell.icon == TableCellValue.Icon.NA
        qcCell.status == status.toString()
        qcCell.id == 1
    }
}
