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
package de.dkfz.tbi.otp.alignment

import de.dkfz.tbi.otp.WorkflowTestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractAlignmentWorkflowTest extends WorkflowTestCase {
    LsdfFilesService lsdfFilesService

    void linkFastqFiles(SeqTrack seqTrack, List<File> testFastqFiles) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        assert seqTrack.seqType.libraryLayout.mateCount == dataFiles.size()

        Map<File, File> sourceLinkMap = [:]
        dataFiles.eachWithIndex { dataFile, index ->
            File sourceFastqFile = testFastqFiles[index]
            assert sourceFastqFile.exists()
            dataFile.fileSize = sourceFastqFile.length()
            dataFile.save()
            File linkFastqFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            sourceLinkMap.put(sourceFastqFile, linkFastqFile)
            File linkViewByPidFastqFile = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
            sourceLinkMap.put(linkFastqFile, linkViewByPidFastqFile)
        }
        createDirectories(sourceLinkMap.values()*.parentFile.unique())
        linkFileUtils.createAndValidateLinks(sourceLinkMap, realm)
    }

    void setUpRefGenomeDir(MergingWorkPackage workPackage, File refGenDir) {
        File linkRefGenDir = referenceGenomeService.referenceGenomeDirectory(workPackage.referenceGenome, false)
        linkFileUtils.createAndValidateLinks([(refGenDir): linkRefGenDir], realm)
    }
}
