/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.utils.LinkEntry

import java.nio.file.Path

@Slf4j
abstract class AbstractRoddyPrepareJob extends AbstractPrepareJob {
    @Autowired
    AbstractBamFileServiceFactoryService abstractBamFileServiceFactoryService

    Collection<LinkEntry> createBamAndBaiLinkEntries(AbstractBamFile bamFile, Path workDirectory) {
        /**
         * Other file names need to be used, because bam files that were not created with Roddy might have a file name that newer Roddy versions
         * can't handle when running analyses.
         */
        String bamFileName = "${bamFile.sampleType.dirName}_${bamFile.individual.pid}_merged.mdup.bam"
        String baiFileName = "${bamFileName}.bai"

        Path targetFileBam = abstractBamFileServiceFactoryService.getService(bamFile).getPathForFurtherProcessing(bamFile)
        Path targetFileBai = targetFileBam.resolveSibling(bamFile.baiFileName)

        return [
                new LinkEntry([link: workDirectory.resolve(bamFileName), target: targetFileBam]),
                new LinkEntry([link: workDirectory.resolve(baiFileName), target: targetFileBai]),
        ]
    }
}
