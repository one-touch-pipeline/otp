/*
 * Copyright 2011-2022 The OTP authors
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

package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional(readOnly = true)
class FastqImportInstanceService {

    FastqImportInstance waiting() {
        return CollectionUtils.atMostOneElement(
                FastqImportInstance.findAllByState(FastqImportInstance.WorkFlowTriggerState.WAITING, [max: 1, sort: 'id', order: 'asc'])
        )
    }

    @Transactional(readOnly = false)
    void updateState(FastqImportInstance fastqImportInstance, FastqImportInstance.WorkFlowTriggerState state) {
        fastqImportInstance.state = state
        fastqImportInstance.save(flush: true)
    }

    /**
     * Change in progress instance back to wait. It is for the case, OTP is shutdown during an object is processed.
     * Should only be called during startup
     */
    @Transactional(readOnly = false)
    void changeProcessToWait() {
        FastqImportInstance.findAllByState(FastqImportInstance.WorkFlowTriggerState.PROCESSING).each { FastqImportInstance fastqImportInstance ->
            log.info("Change import ${fastqImportInstance.otrsTicket.ticketNumber} from ${FastqImportInstance.WorkFlowTriggerState.PROCESSING} back " +
                    "to ${FastqImportInstance.WorkFlowTriggerState.WAITING}")
            updateState(fastqImportInstance, FastqImportInstance.WorkFlowTriggerState.WAITING)
        }
    }
}
