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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Script to delete a FastqImportInstance if it was loaded twice.
 */

long fastqImportInstanceId = 0

DeletionService deletionService = ctx.deletionService
ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

FileSystem fileSystem = fileSystemService.remoteFileSystem

Path baseOutputDir = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('sample_swap')

FastqImportInstance.withTransaction {
    FastqImportInstance fastqImportInstance = FastqImportInstance.get(fastqImportInstanceId)
    assert fastqImportInstance
    RawSequenceFile.findAllByFastqImportInstance(fastqImportInstance).each { RawSequenceFile rawSequenceFile ->
        MetaDataEntry.findAllBySequenceFile(rawSequenceFile).each { MetaDataEntry entry ->
            entry.delete(flush: true)
        }
        assert MetaDataEntry.countBySequenceFile(rawSequenceFile) == 0
        CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))?.delete(flush: true)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        deletionService.deleteProcessingFilesOfProject(seqTrack.individual.project.name, baseOutputDir, false, false, [seqTrack])

        rawSequenceFile.delete(flush: true)
        if (!seqTrack.sequenceFiles) {
            seqTrack.delete(flush: true)
        }
    }
    assert RawSequenceFile.countByFastqImportInstance(fastqImportInstance) == 0
    MetaDataFile.findAllByFastqImportInstance(fastqImportInstance).each {
        it.delete(flush: true)
    }
    assert MetaDataFile.countByFastqImportInstance(fastqImportInstance) == 0
    assert FastqImportInstance.get(fastqImportInstanceId) == fastqImportInstance
    fastqImportInstance.delete(flush: true)
    assert FastqImportInstance.get(fastqImportInstanceId) == null
    assert false
}
''
