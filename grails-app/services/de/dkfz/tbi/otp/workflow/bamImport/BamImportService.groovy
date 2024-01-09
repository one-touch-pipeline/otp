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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

@Component
@Transactional(readOnly = true)
@Slf4j
class BamImportService {

    LsdfFilesService lsdfFilesService
    WorkflowService workflowService

    final static String QUERY_WAITING_AND_ALLOWED_IMPORT_PROCESS = """
        select
            imp
        from
            ImportProcess imp
        where
            imp.workflowCreateState = 'WAITING'
            and not exists (
                select
                    imp2.id
                from
                    ImportProcess imp2
                    join imp2.externallyProcessedBamFiles bf2
                where
                    imp2.workflowCreateState = 'PROCESSING'
                    and bf2.workPackage.sample.individual.project in (
                        select
                            bf3.workPackage.sample.individual.project
                        from
                            ImportProcess imp3
                            join imp3.externallyProcessedBamFiles bf3
                        where
                            imp3 = imp
                    )
            )
        order by imp.id asc
        """

    ImportProcess waiting() {
        return CollectionUtils.atMostOneElement(
                ImportProcess.executeQuery(QUERY_WAITING_AND_ALLOWED_IMPORT_PROCESS, [:], [max: 1])
        )
    }

    @CompileDynamic
    int countInstancesInWaitingState() {
        return ImportProcess.countByWorkflowCreateState(WorkflowCreateState.WAITING)
    }

    @Transactional(readOnly = false)
    void updateState(long id, WorkflowCreateState state) {
        ImportProcess bamImportDb = ImportProcess.get(id)
        bamImportDb.workflowCreateState = state
        bamImportDb.save(flush: true)
    }
}
