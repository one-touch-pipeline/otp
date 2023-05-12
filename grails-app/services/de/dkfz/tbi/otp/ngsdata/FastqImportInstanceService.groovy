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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional(readOnly = true)
class FastqImportInstanceService {

    final static String QUERY_WAITING_AND_ALLOWED_FASTQ_IMPORT_INSTANCE = """
        select
            fii
        from
            FastqImportInstance fii
        where
            fii.state = 'WAITING'
            and not exists (
                select
                    fii2.id
                from
                    FastqImportInstance fii2
                    join fii2.dataFiles df2
                where
                    fii2.state = 'PROCESSING'
                    and df2.seqTrack.sample.individual.project in (
                        select
                            df3.seqTrack.sample.individual.project
                        from
                            FastqImportInstance fii3
                            join fii3.dataFiles df3
                        where
                            fii3 = fii
                    )
            )
        order by fii.id asc
        """

    FastqImportInstance waiting() {
        return CollectionUtils.atMostOneElement(
                FastqImportInstance.executeQuery(QUERY_WAITING_AND_ALLOWED_FASTQ_IMPORT_INSTANCE, [:], [max: 1])
        )
    }

    int countInstancesInWaitingState() {
        return FastqImportInstance.countByState(FastqImportInstance.WorkflowCreateState.WAITING)
    }

    @Transactional(readOnly = false)
    void updateState(FastqImportInstance fastqImportInstance, FastqImportInstance.WorkflowCreateState state) {
        FastqImportInstance fastqImportInstanceReFetch = FastqImportInstance.get(fastqImportInstance.id)
        fastqImportInstanceReFetch.state = state
        fastqImportInstanceReFetch.save(flush: true)
    }

    /**
     * Change all FastqImportInstances with state processing to state wait. This is meant for the case, OTP is shutdown during an object is processed.
     * Should only be called during startup
     */
    @Transactional(readOnly = false)
    void changeProcessToWait() {
        FastqImportInstance.findAllByState(FastqImportInstance.WorkflowCreateState.PROCESSING).each { FastqImportInstance fastqImportInstance ->
            log.info("Change import ${fastqImportInstance.otrsTicket.ticketNumber} from ${FastqImportInstance.WorkflowCreateState.PROCESSING} back " +
                    "to ${FastqImportInstance.WorkflowCreateState.WAITING}")
            updateState(fastqImportInstance, FastqImportInstance.WorkflowCreateState.WAITING)
        }
    }
}
