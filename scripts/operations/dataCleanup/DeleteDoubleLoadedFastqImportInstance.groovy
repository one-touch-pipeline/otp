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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
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

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

Path baseOutputDir = fileService.toPath(configService.getScriptOutputPath(), fileSystem).resolve('sample_swap')

FastqImportInstance.withTransaction {
    FastqImportInstance fastqImportInstance = FastqImportInstance.get(fastqImportInstanceId)
    assert fastqImportInstance
    DataFile.findAllByFastqImportInstance(fastqImportInstance).each { DataFile dataFile ->
        MetaDataEntry.findAllByDataFile(dataFile).each { MetaDataEntry entry ->
            entry.delete(flush: true)
        }
        assert MetaDataEntry.countByDataFile(dataFile) == 0
        CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllByDataFile(dataFile))?.delete(flush: true)
        SeqTrack seqTrack = dataFile.seqTrack
        deletionService.deleteProcessingFilesOfProject(seqTrack.individual.project.name, baseOutputDir, false, false, [seqTrack])

        ConsistencyStatus.findAllByDataFile(dataFile)*.delete(flush: true)

        dataFile.delete(flush: true)
        if (!seqTrack.dataFiles) {
            seqTrack.delete(flush: true)
        }
    }
    assert DataFile.countByFastqImportInstance(fastqImportInstance) == 0
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
