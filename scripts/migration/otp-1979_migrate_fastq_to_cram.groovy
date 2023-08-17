/*
 * Copyright 2011-2022 The OTP authors
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
package migration

import groovy.sql.Sql

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataswap.ScriptBuilder
import de.dkfz.tbi.otp.filestore.BaseFolder
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import javax.sql.DataSource
import java.nio.file.Path

/**
 * TSV file with the following format:
 * id   baseFolder  uuid    cramMd5Sum newFileName
 */
String fastqToCramFile = '/home/otp/filesystem/scripts/cram/otp-1979-demo-migration/ExampleProject.tsv'

FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService
DataSource dataSource = ctx.dataSource
LsdfFilesService lsdfFilesService = ctx.lsdfFilesService
ConfigService configService = ctx.configService

Path path = fileService.toPath(new File(fastqToCramFile), fileSystemService.remoteFileSystemOnDefaultRealm)
ScriptBuilder scriptBuilder = new ScriptBuilder(configService, fileService, fileSystemService, path.parent)
ArrayList<String[]> rows = path.readLines().tail()*.split('\t')

List<String> pathsToDelete = []

RawSequenceFile.withTransaction {
    Sql sql = new Sql(dataSource)
    String sequenceCramFilePackage = SequenceCramFile.class.name
    String sqlQuery = "UPDATE raw_sequence_file SET class=:sequenceCramFilePackage WHERE id=:id"

    rows.forEach { row ->
        long id = row[0] as long
        String baseFolderPath = row[1]
        UUID uuid = UUID.fromString(row[2])
        String cramMd5Sum = row[3]
        String newFileName = row[4]

        println "migrate FastqFile with id = ${id} to SequenceCramFile"
        sql.execute(sqlQuery, [id: id, sequenceCramFilePackage: sequenceCramFilePackage])
        SequenceCramFile sequenceCramFile = SequenceCramFile.findById(id)
        if (sequenceCramFile.fileExists) {
            pathsToDelete += lsdfFilesService.getFileFinalPathAsPath(sequenceCramFile).toString()
            pathsToDelete += lsdfFilesService.getFileViewByPidPathAsPath(sequenceCramFile).toString()
        }
        sequenceCramFile.cramMd5sum = cramMd5Sum

        BaseFolder baseFolder = BaseFolder.findByPath(baseFolderPath)
        if (!baseFolder) {
            baseFolder = new BaseFolder([
                    path    : baseFolderPath,
                    writable: true,
            ]).save(flush: true)
        }

        WorkFolder workFolder = WorkFolder.findByUuidAndBaseFolder(uuid, baseFolder)
        if (!workFolder) {
            workFolder = new WorkFolder([
                    uuid      : uuid,
                    baseFolder: baseFolder,
            ]).save(flush: true)
        }

        sequenceCramFile.seqTrack.workflowArtefact.producedBy.workFolder = workFolder
        sequenceCramFile.fileName = newFileName
        sequenceCramFile.vbpFileName = newFileName
        sequenceCramFile.save(flush: true)
    }

    println "-----------------------"
    println "db migration completed!"
    println "-----------------------"

    scriptBuilder.addBashCommand("#!/bin/bash")
    pathsToDelete.each { pathToDelete ->
        scriptBuilder.addBashCommand("rm -f ${pathToDelete}")
    }
    println(scriptBuilder.build('oldFastqSequenceFileDeletionScript.sh', true))
}
