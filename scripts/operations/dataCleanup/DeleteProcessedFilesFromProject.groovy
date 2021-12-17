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
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.FileSystem
import java.nio.file.Path

// input area
//----------------------

String projectName = ""

// optional restriction on SeqTracks
// not selecting any SeqTracks results in the whole project being affected
List<SeqTrack> seqTracks = SeqTrack.withCriteria {
    sample {
        individual {
            'in'('pid', [
                    '',
            ])
            project {
                eq('name', projectName)
            }
        }
    }
    'in'('seqType', [
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.exomePairedSeqType,
            SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
            SeqTypeService.rnaSingleSeqType,
            SeqTypeService.rnaPairedSeqType,
            SeqTypeService.chipSeqPairedSeqType,
            SeqTypeService.'10xSingleCellRnaSeqType',
    ])
}

//script area
//-----------------------------

assert projectName

if (seqTracks) {
    println "Affected SeqTracks:"
    seqTracks.groupBy { it.individual }.each { Individual individual, List<SeqTrack> seqTracksPerIndividual ->
        println "  - ${individual}"
        seqTracksPerIndividual.each { SeqTrack seqTrack ->
            println "    * ${seqTrack}"
        }
    }
} else {
    println """\
    |#########################################################################
    |# No restriction on specific SeqTracks, entire project will be removed! #
    |#########################################################################
    |""".stripMargin()
}

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

Path baseOutputDir = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('sample_swap')

DeletionService deletionService = ctx.deletionService

Project.withTransaction {
    deletionService.deleteProcessingFilesOfProject(projectName, baseOutputDir, false, false, seqTracks)
    assert false: "DEBUG, remove this line to continue"
}
