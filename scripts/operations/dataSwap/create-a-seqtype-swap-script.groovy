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
import de.dkfz.tbi.otp.dataswap.AbstractDataSwapService
import de.dkfz.tbi.otp.dataswap.ScriptBuilder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Paths

ConfigService configService = ctx.configService
FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService


/**
 * Generation script for LaneSwaps
 */

// Input area
// ------------------------------

swapLabel = 'OTRS-________________-something-descriptive'

/***
 * The query to get the seqtracks for the swap.
 * Please adapt as needed.
 */
List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
    sample {
        individual {
            project {
                eq('name', 'SomeProject')
            }
            'in'('pid', [
                    'pid1',
                    'pid2',
            ])
        }
        sampleType {
            'in'('name', [
                    'sampleType1',
                    'sampleType2',
            ])
        }
    }
    seqType {
        'in'('name', [
                'seqType1',
                'seqType2',
        ])
        eq('libraryLayout', SequencingReadType.PAIRED)
        eq('singleCell', 'TRUE OR FALSE')
    }
    order('id')
}

/**
 * This closure determines the new properties of each seqtrack. It is executed for each seqtrack individually.
 * Overwrite in the map the value you would like to change for each seqTrack
 */
def adaptValues = { SeqTrack oldSeqTrack ->
    [
            newProjectName   : oldSeqTrack.project.name,
            newPid           : oldSeqTrack.individual.pid,
            newSampleTypeName: oldSeqTrack.sampleType.name,
            newSeqTypeName   : oldSeqTrack.seqType.name,
            newSingleCell    : oldSeqTrack.seqType.singleCell,
            newLibraryLayout : oldSeqTrack.seqType.libraryLayout,
    ]
}

// script area
// ------------------------------

int counter = 1
ScriptBuilder builder = new ScriptBuilder(configService, fileService, fileSystemService, Paths.get('sample_swap', swapLabel))
List<String> all_swaps = []

builder.addGroovyCommand("""
          import de.dkfz.tbi.otp.dataswap.LaneSwapService
          import de.dkfz.tbi.otp.dataswap.Swap
          import de.dkfz.tbi.otp.dataswap.parameters.LaneSwapParameters
          import de.dkfz.tbi.otp.ngsdata.*
          import de.dkfz.tbi.otp.dataprocessing.*
          import de.dkfz.tbi.otp.utils.*
          import de.dkfz.tbi.otp.config.*
          import de.dkfz.tbi.otp.infrastructure.FileService
          import de.dkfz.tbi.otp.job.processing.FileSystemService
          
          import java.nio.file.*
          import java.nio.file.attribute.PosixFilePermission
          import java.nio.file.attribute.PosixFilePermissions
          
          import static org.springframework.util.Assert.*
          
          ConfigService configService = ctx.configService
          FileSystemService fileSystemService = ctx.fileSystemService
          FileService fileService = ctx.fileService
          LaneSwapService laneSwapService = ctx.laneSwapService
          
          FileSystem fileSystem = fileSystemService.remoteFileSystem
          
          StringBuilder log = new StringBuilder()
          
          // create a container dir for all output of this swap;
          // group-editable so non-server users can also work with it
          String swapLabel = "${swapLabel}"
          final Path SCRIPT_OUTPUT_DIRECTORY = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('sample_swap').resolve(swapLabel)
          fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(SCRIPT_OUTPUT_DIRECTORY)
          fileService.setPermission(SCRIPT_OUTPUT_DIRECTORY, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)
          
          /** did we manually check yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
          boolean linkedFilesVerified = false
          
          /** are missing fastq files an error? (usually yes, since we must redo most analyses after a swap) */
          boolean failOnMissingFiles = true
          
          
          try {
              Individual.withTransaction {[
          """.stripIndent())

seqTracks.each { seqTrack ->
    Map newValues = adaptValues(seqTrack)

    String swapName = "${seqTrack.individual.pid}__${seqTrack.run.name}__${seqTrack.laneId}__${seqTrack.seqType.name}__to__${newValues.newSeqTypeName}".replace('-', '_')
    String swapOrderedName = "swap_${String.valueOf(counter++).padLeft(4, '0')}_${swapName}"
    all_swaps << swapOrderedName

    builder.addGroovyCommand("\n{\n" +
            "\n\tlaneSwapService.swap( \n" +
            "\t\tnew LaneSwapParameters(\n" +
            "\t\tprojectNameSwap: new Swap('${seqTrack.project.name}', '${newValues.newProjectName}'),\n" +
            "\t\tpidSwap: new Swap('${seqTrack.individual.pid}', '${newValues.newPid}'),\n" +
            "\t\tsampleTypeSwap: new Swap('${seqTrack.sampleType.name}', '${newValues.newSampleTypeName}'),\n" +
            "\t\tseqTypeSwap: new Swap('${seqTrack.seqType.name}', '${newValues.newSeqTypeName}'),\n" +
            "\t\tsingleCellSwap: new Swap('${seqTrack.seqType.singleCell}', '${newValues.newSingleCell}'),\n" +
            "\t\tsequencingReadTypeSwap: new Swap('${seqTrack.seqType.libraryLayout}', '${newValues.newLibraryLayout}'),\n" +
            "\t\trunName: '${seqTrack.run.name}',\n" +
            "\t\tlanes: ['${seqTrack.laneId}',],\n" +
            "\t\tsampleNeedsToBeCreated: false,\n" +
            "\t\trawSequenceFileSwaps        : [\n")

    seqTrack.sequenceFiles.each { rawSequenceFile ->
        builder.addGroovyCommand( "\t\t\tnew Swap('${rawSequenceFile.fileName}', ''),\n")
    }

    builder.addGroovyCommand("\t\t],\n" +
            "\t\t'bashScriptName': '${swapOrderedName}',\n" +
            "\t\t'log': log,\n" +
            "\t\t'failOnMissingFiles': failOnMissingFiles,\n" +
            "\t\t'scriptOutputDirectory': SCRIPT_OUTPUT_DIRECTORY,\n" +
            "\t\t'linkedFilesVerified': linkedFilesVerified,\n" +
            "\t))\n},\n")
}

builder.addGroovyCommand("""|
          |].each { it() }
          |    assert false : "transaction intentionally failed to rollback transaction"
          |    }
          |} finally {
          |    println log
          |}
          |
          |""".stripMargin())

builder.addBashCommand(AbstractDataSwapService.BASH_HEADER + all_swaps.collect { "bash ${it}.sh" }.join('\n'))

println builder.build("seqtype-swap-${swapLabel}.sh")
