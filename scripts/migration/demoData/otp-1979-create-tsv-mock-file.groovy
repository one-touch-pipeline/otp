/*
 * Copyright 2011-2023 The OTP authors
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
import de.dkfz.tbi.otp.filestore.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path

/** Script to create a mocked tsv File for testing the database migration script in issue otp-1979
 * on the example database
 */

LsdfFilesService lsdfFilesService = ctx.lsdfFilesService
FilestoreService filestoreService = ctx.filestoreService
FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService
ConfigService configService = ctx.configService

String pathName = "otp-1979-demo-migration"
Path baseDir = fileSystemService.remoteFileSystemOnDefaultRealm.getPath(configService.scriptOutputPath.toString()).resolve('cram').resolve(pathName)
println "Write to Folder: ${baseDir}"

List<SeqType> seqTypes = [
        SeqTypeService.wholeGenomePairedSeqType,
        SeqTypeService.exomePairedSeqType,
]

String header = ([
        'id',
        'baseFolder',
        'uuid',
        'cramMd5Sum',
        'newFileName'
].join('\t'))

BaseFolder baseFolder = BaseFolder.last() ?: new BaseFolder([
        path: '/home/otp/filesystem/otp_data_001'
])

Project.findAllByName('ExampleProject').sort {
    it.name
}.each { Project project ->
    List<FastqFile> fastqFiles = FastqFile.withCriteria {
        seqTrack {
            sample {
                individual {
                    eq('project', project)
                }
            }
            'in'('seqType', seqTypes)
        }
    }
    Path tableFile = baseDir.resolve(project.name.replaceAll('[^a-zA-Z0-9_]', '-') + '.tsv')
    String table = [
            header,
            fastqFiles.groupBy {
                it.seqTrack
            }.collect { SeqTrack seqTrack, List<FastqFile> fastqFilesPerSeqTrack ->
                UUID uuid = UUID.randomUUID()
                WorkFolder workFolder = new WorkFolder([
                        baseFolder: baseFolder,
                        uuid      : uuid,
                ])
                Path basePath = filestoreService.getWorkFolderPath(workFolder)
                fastqFilesPerSeqTrack.collect { FastqFile fastqFile ->
                    String cramName = fastqFile.fileName.
                            replace('.fastq.gz', '.ucram').
                            replace('.txt.gz', '.ucram').
                            replace('.fq.gz', '.ucram')
                    assert cramName.endsWith('.ucram')
                    [
                            fastqFile.id,
                            baseFolder.path,
                            uuid,
                            fastqFile.id.toString().md5(),
                            cramName
                    ].join('\t')
                }.sort().join('\n')
            }.join('\n'),
    ].join('\n')
    println 'Write in file: ' + tableFile
    println(table)
    fileService.createFileWithContentOnDefaultRealm(tableFile, table, FileService.DEFAULT_FILE_PERMISSION, true)
}

'end'