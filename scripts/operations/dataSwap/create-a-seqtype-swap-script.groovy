import de.dkfz.tbi.otp.ngsdata.*

/**
 * Generation script for LaneSwaps
 *
 */


//Input area
//------------------------------

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
        eq('libraryLayout', SeqType.LIBRARYLAYOUT_PAIRED)
    }
    order('id')
}

/**
 * Overwrite in the map the value you would like to change for each seqTrack
 */
def adaptValues = { SeqTrack oldSeqTrack ->
    [
            newProjectName   : oldSeqTrack.project.name,
            newPid           : oldSeqTrack.individual.pid,
            newSampleTypeName: oldSeqTrack.sampleType.name,
            newSeqTypeName   : oldSeqTrack.seqType.name,
            newLibraryLayout : oldSeqTrack.seqType.libraryLayout,
    ]
}


//script area
//------------------------------

int counter =0
StringBuilder script = new StringBuilder()
List<String> files = []

script << """
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.config.*

import static org.springframework.util.Assert.*

DataSwapService dataSwapService = ctx.dataSwapService

StringBuilder outputStringBuilder = new StringBuilder()

final String SCRIPT_OUTPUT_DIRECTORY = "\${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

boolean linkedFilesVerified = false

boolean failOnMissingFiles = true


try {
    Individual.withTransaction {
        [
"""

seqTracks.each { seqTrack ->
    Map newValues = adaptValues(seqTrack)

    String name = "${seqTrack.individual.pid}__${seqTrack.run.name}__${seqTrack.laneId}__${seqTrack.seqType.name}__to__${newValues.newSeqTypeName}".replace('-', '_')
    String fileName = "swap_${String.valueOf(counter++).padLeft(4, '0')}_${name}"
    files << fileName

    script << """
    {
        dataSwapService.swapLane(
            [
                "oldProjectName"   : "${seqTrack.project.name}",
                "newProjectName"   : "${newValues.newProjectName}",
                "oldPid"           : "${seqTrack.individual.pid}",
                "newPid"           : "${newValues.newPid}",
                "oldSampleTypeName": "${seqTrack.sampleType.name}",
                "newSampleTypeName": "${newValues.newSampleTypeName}",
                "oldSeqTypeName"   : "${seqTrack.seqType.name}",
                "newSeqTypeName"   : "${newValues.newSeqTypeName}",
                "oldLibraryLayout" : "${seqTrack.seqType.libraryLayout}",
                "newLibraryLayout" : "${newValues.newLibraryLayout}",
                "runName"          : "${seqTrack.run.name}",
                "lane"             : [
                        "${seqTrack.laneId}",
                ]
            ],
            ["""
    seqTrack.dataFiles.each { dataFile ->
        script << "\n'${dataFile.fileName}':'',"
    }

    script << """
            ],
            '${fileName}',
            outputStringBuilder,
            failOnMissingFiles,
            SCRIPT_OUTPUT_DIRECTORY,
            linkedFilesVerified,
            )
    },
"""
}

script << """
        ].each {
            it()
        }
        assert false
    }
} finally {
    println outputStringBuilder
}

"""

script << """
//scripts to execute
/*
${DataSwapService.bashHeader + files.collect {
    "bash ${it}.sh"
}.join('\n')}
*/

"""
println script

