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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.gorm.transactions.NotTransactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.LinkFilesToFinalDestinationService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.SessionUtils

/**
 * This is the last job of the PanCan workflow.
 * Within this job the merged bam file, the corresponding index file, the QA-folder and the roddyExecutionStore folder
 * are linked from the working processing folder in the project folder.
 * After linking, tmp roddy files and not used files in older work directories are deleted.
 */
@CompileDynamic
@Component
@Scope("prototype")
@Slf4j
class LinkWgbsAlignmentFilesJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    @Autowired
    ConfigService configService

    @NotTransactional
    @Override
    void execute() throws Exception {
        SessionUtils.withNewSession {
            final RoddyBamFile roddyBamFile = processParameterObject

            Realm realm = roddyBamFile.project.realm
            assert realm: "Realm should not be null"

            linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

            succeed()
        }
    }
}
