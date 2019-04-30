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

package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.Realm

@Component("importExternallyMergedBamStartJob")
@Scope("singleton")
@Slf4j
class ImportExternallyMergedBamStartJob extends AbstractStartJobImpl {

    @Scheduled(fixedDelay = 60000L)
    @Override
    void execute() {
        Realm.withNewSession {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
                return
            }

            ImportProcess.withTransaction {
                ImportProcess importProcess = ImportProcess.findByState(ImportProcess.State.NOT_STARTED)
                if (importProcess) {
                    importProcess.state = ImportProcess.State.STARTED
                    assert importProcess.save(flush: true)
                    createProcess(importProcess)
                    log.debug "Creating process for import ${importProcess}"
                }
            }
        }
    }
}
