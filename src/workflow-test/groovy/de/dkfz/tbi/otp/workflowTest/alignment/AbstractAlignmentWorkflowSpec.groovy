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
package de.dkfz.tbi.otp.workflowTest.alignment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflowTest.AbstractDecidedWorkflowSpec

import java.nio.file.Files
import java.nio.file.Path

/**
 * Provides some helper needed in all alignment workflows
 */
abstract class AbstractAlignmentWorkflowSpec extends AbstractDecidedWorkflowSpec {

    ReferenceGenomeService referenceGenomeService

    protected void linkFastqFiles(SeqTrack seqTrack, List<Path> testFastqFiles) {
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllBySeqTrack(seqTrack)
        assert seqTrack.seqType.libraryLayout.mateCount == rawSequenceFiles.size()

        rawSequenceFiles.sort {
            it.mateNumber
        }.eachWithIndex { rawSequenceFile, index ->
            Path sourceFastqFile = testFastqFiles[index]
            assert Files.exists(sourceFastqFile)
            assert Files.isReadable(sourceFastqFile)
            rawSequenceFile.fileSize = Files.size(sourceFastqFile)
            rawSequenceFile.save(flush: true)
            Path linkFastqFile = lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile)
            Path linkViewByPid = lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile)
            fileService.createLink(linkFastqFile, sourceFastqFile)
            fileService.createLink(linkViewByPid, linkFastqFile)
        }
    }

    /**
     * link the reference genome directory into the test structure
     */
    void linkReferenceGenomeDirectoryToReference(ReferenceGenome referenceGenome) {
        Path target = referenceDataDirectory.resolve("reference-genomes").resolve(referenceGenome.path)
        Path link = remoteFileSystem.getPath(referenceGenomeService.referenceGenomeDirectory(referenceGenome, false).absolutePath)
        fileService.createLink(link, target)
    }

    /**
     * link the adapter directory into the test structure
     */
    void linkAdapterDirectoryToReference(LibraryPreparationKit libraryPreparationKit) {
        Path target = referenceDataDirectory.resolve("adapters")
        Path link = remoteFileSystem.getPath(libraryPreparationKit.adapterFile).parent
        fileService.createLink(link, target)
    }
}
