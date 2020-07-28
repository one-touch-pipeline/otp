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

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Generation script for LaneSwaps
 */


//Input area
//------------------------------

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
        eq('libraryLayout', LibraryLayout.PAIRED)
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


//script area
//------------------------------

int counter = 1
StringBuilder script = new StringBuilder()
List<String> all_swaps = []

script << """
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.config.*

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

import static org.springframework.util.Assert.*

DataSwapService dataSwapService = ctx.dataSwapService

StringBuilder log = new StringBuilder()

// create a container dir for all output of this swap;
// group-editable so non-server users can also work with it
String swapLabel = "${swapLabel}"
final Path SCRIPT_OUTPUT_DIRECTORY = ctx.configService.getScriptOutputPath().toPath().resolve('sample_swap').resolve(swapLabel)
ctx.fileService.createDirectoryRecursively(SCRIPT_OUTPUT_DIRECTORY)
ctx.fileService.setPermission(SCRIPT_OUTPUT_DIRECTORY, ctx.fileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

boolean linkedFilesVerified = false

boolean failOnMissingFiles = true


try {
    Individual.withTransaction {[
"""

seqTracks.each { seqTrack ->
    Map newValues = adaptValues(seqTrack)

    String swapName = "${seqTrack.individual.pid}__${seqTrack.run.name}__${seqTrack.laneId}__${seqTrack.seqType.name}__to__${newValues.newSeqTypeName}".replace('-', '_')
    String swapOrderedName = "swap_${String.valueOf(counter++).padLeft(4, '0')}_${swapName}"
    all_swaps << swapOrderedName

    script << """
    {
        dataSwapService.swapLane([
                "oldProjectName"   : "${seqTrack.project.name}",
                "newProjectName"   : "${newValues.newProjectName}",
                "oldPid"           : "${seqTrack.individual.pid}",
                "newPid"           : "${newValues.newPid}",
                "oldSampleTypeName": "${seqTrack.sampleType.name}",
                "newSampleTypeName": "${newValues.newSampleTypeName}",
                "oldSeqTypeName"   : "${seqTrack.seqType.name}",
                "newSeqTypeName"   : "${newValues.newSeqTypeName}",
                "oldSingleCell"    : "${seqTrack.seqType.singleCell}",
                "newSingleCell"    : "${newValues.newSingleCell}",
                "oldLibraryLayout" : "${seqTrack.seqType.libraryLayout}",
                "newLibraryLayout" : "${newValues.newLibraryLayout}",
                "runName"          : "${seqTrack.run.name}",
                "lane"             : [ "${seqTrack.laneId}", ],
            ],
            ["""
    seqTrack.dataFiles.each { dataFile ->
        script << "\n                '${dataFile.fileName}':'',"
    }

    script << """
            ],
            '${swapOrderedName}',
            log,
            failOnMissingFiles,
            SCRIPT_OUTPUT_DIRECTORY,
            linkedFilesVerified,
        )
    },
"""
}

script << """
    ].each { it() }
    assert false : "transaction intentionally failed to rollback transaction"
    }
} finally {
    println log
}

"""

script << """
//scripts to execute
/*
${DataSwapService.BASH_HEADER + all_swaps.collect {
    "bash ${it}.sh"
}.join('\n')}
*/

"""
println script

