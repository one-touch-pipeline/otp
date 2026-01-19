/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class CellRangerPrepareJobSpec extends Specification {
    CellRangerPrepareJob cellRangerPrepareJob
    CellRangerService cellRangerService
    NotificationCreator notificationCreator

    void setup() {
        this.cellRangerService = Mock(CellRangerService)
        this.notificationCreator = Mock(NotificationCreator)
        this.cellRangerPrepareJob = new CellRangerPrepareJob(cellRangerService, notificationCreator)
    }

    CellRangerPrepareJob overrideCellRangerJob(SingleCellBamFile bamFile) {
        return new CellRangerPrepareJob(cellRangerService, notificationCreator) {
            @Override
            SingleCellBamFile getBamFile(WorkflowStep workflowStep) {
                return bamFile
            }
        }
    }

    void "test generateMapForLinking should return empty list"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)

        when:
        Collection<LinkEntry> result = cellRangerPrepareJob.generateMapForLinking(workflowStep)

        then:
        result == []
    }

    void "test buildWorkDirectoryPath should return null"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)

        when:
        Path result = cellRangerPrepareJob.buildWorkDirectoryPath(workflowStep)

        then:
        result == null
    }

    void "test doFurtherPreparation calls service methods"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)
        SingleCellBamFile bamFile = Mock(SingleCellBamFile)
        CellRangerMergingWorkPackage workPackage = Mock(CellRangerMergingWorkPackage)
        bamFile.mergingWorkPackage >> workPackage
        cellRangerPrepareJob = overrideCellRangerJob(bamFile)

        when:
        cellRangerPrepareJob.doFurtherPreparation(workflowStep)

        then:
        1 * cellRangerService.createInputDirectoryStructure(bamFile)
        1 * notificationCreator.setStartedForSeqTracks(bamFile.seqTracks, Ticket.ProcessingStep.ALIGNMENT)
    }

    void "test doFurtherPreparation sets needsProcessing to false and saves work package in correct order"() {
        given:
        WorkflowStep workflowStep = Mock(WorkflowStep)
        SingleCellBamFile bamFile = Mock(SingleCellBamFile)
        CellRangerMergingWorkPackage workPackage = Mock(CellRangerMergingWorkPackage)
        bamFile.mergingWorkPackage >> workPackage
        cellRangerPrepareJob = overrideCellRangerJob(bamFile)

        when:
        cellRangerPrepareJob.doFurtherPreparation(workflowStep)

        then:
        1 * workPackage.setNeedsProcessing(false)
        then:
        1 * workPackage.save(flush: true)
    }
}
