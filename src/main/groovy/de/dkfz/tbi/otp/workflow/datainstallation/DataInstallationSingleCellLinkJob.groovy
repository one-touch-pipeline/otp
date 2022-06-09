/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.datainstallation

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellMappingFileService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.WellDirectory
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
@CompileStatic
class DataInstallationSingleCellLinkJob extends AbstractLinkJob implements DataInstallationShared {

    @Autowired
    SingleCellMappingFileService singleCellMappingFileService

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        if (seqTrack.seqType.singleCell && seqTrack.singleCellWellLabel) {
            return seqTrack.dataFiles.collect { DataFile dataFile ->
                Path target = lsdfFilesService.getFileFinalPathAsPath(dataFile)
                Path link = lsdfFilesService.getFileViewByPidPathAsPath(dataFile, WellDirectory.ALL_WELL)
                return new LinkEntry(target: target, link: link)
            }
        }
        return []
    }

    @Override
    protected void doFurtherWork(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        if (seqTrack.seqType.singleCell && seqTrack.singleCellWellLabel) {
            logService.addSimpleLogEntry(workflowStep, "Add all datafiles to single cell mapping file")
            seqTrack.dataFiles.each { DataFile dataFile ->
                singleCellMappingFileService.addMappingFileEntryIfMissing(dataFile)
            }
        }
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) { }
}
