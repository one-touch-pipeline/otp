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
package de.dkfz.tbi.otp.job.jobs.dataInstallation

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellMappingFileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkFileUtils

/**
 * Create additional links for single cell with well identifier and the mapping info file
 */
@Component
@Scope("prototype")
@ResumableJob
@Slf4j
class CreateSingleCellAllLinkJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    SingleCellMappingFileService singleCellMappingFileService

    @SuppressWarnings('JavaIoPackageAccess')
    @Override
    void execute() throws Exception {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(processParameterValue))
        if (seqTrack.seqType.singleCell && seqTrack.singleCellWellLabel) {
            Realm realm = seqTrack.project.realm

            List<DataFile> dataFiles = seqTrack.dataFiles

            Map map = dataFiles.collectEntries { DataFile dataFile ->
                String source = lsdfFilesService.getFileFinalPath(dataFile)
                String link = lsdfFilesService.getWellAllFileViewByPidPath(dataFile)
                return [(new File(source)): new File(link)]
            }
            linkFileUtils.createAndValidateLinks(map, realm, seqTrack.project.unixGroup)

            dataFiles.each {
                singleCellMappingFileService.addMappingFileEntryIfMissing(it)
            }
        }
        succeed()
    }
}
