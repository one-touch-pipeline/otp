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
package de.dkfz.tbi.otp.job.jobs.dataInstallation

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkFileUtils

@Component
@Scope("prototype")
@ResumableJob
@Slf4j
class CreateViewByPidJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    LinkFileUtils linkFileUtils


    @Override
    void execute() throws Exception {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        Realm realm = seqTrack.project.realm

        Map map = seqTrack.dataFiles.collectEntries { DataFile dataFile ->
            linkDataFile(dataFile)
        }
        linkFileUtils.createAndValidateLinks(map, realm, seqTrack.project.unixGroup)

        seqTrack.dataFiles*.fileLinked = true
        seqTrack.dataFiles*.dateLastChecked = new Date()
        seqTrack.dataFiles*.save()
        seqTrack.dataInstallationState = SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState = SeqTrack.DataProcessingState.NOT_STARTED
        assert (seqTrack.save())
        succeed()
    }

    private Map<File, File> linkDataFile(DataFile file) {
        String source = lsdfFilesService.getFileFinalPath(file)
        String link = lsdfFilesService.getFileViewByPidPath(file)

        assert source: "No source file could be found"
        assert link: "No link file could be found"

        return [(new File(source)): new File(link)]
    }
}
