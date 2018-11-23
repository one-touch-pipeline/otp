import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Generation Script which allow:
 * - rename patient
 * - move all samples of one patient into another one
 * - change sample type of sample
 * - move sample to other patient
 * - merge sample into another
 *
 * This can be limited to specific seqTypes, if wished.
 *
 * If necessary, the code for create new Patient and samples are added to the script.
 *
 * In many cases there will be stay empty patients or samples.
 *
 * Limitation: If you want to merge one patient into another, they may not have the same SampleTypes (not implemented yet)
 */


//Input area
//------------------------------

def swapMap = [
        ('oldPid')              : 'newPid',
        ('pid oldSampleType')   : 'pid newSampleType',
        ('oldPid oldSampleType'): 'newPid newSampleType',
        ('oldPid sampleType')   : 'newPid sampleType',
]

//Only need, if the project should be changed. If the new Individual already exist, it have to belong to this project.
String newProjectName = ''

/**
 * Allow to filter for specific seqTypes. If given, only SeqTracks of the given SeqTypes will be swapped.
 *
 * For example: SeqTypeService.wholeGenomePairedSeqType
 */
def seqTypeFilterList = [
]

//script area
//------------------------------

int counter = 0

List<String> files = []

def newSampleTypeClosure = { String sampleType ->
    return sampleType
}

def newDataFileNameClosure = { DataFile dataFile, String oldPatientName, String newPatientName ->
    String oldFileName = dataFile.fileName
    String newFileName = oldFileName.replace(oldPatientName, newPatientName)
    if (oldFileName == newFileName) {
        println "\t- data file name remains unchanged: ${newFileName}"
        return ''
    }
    println "\t- data file name changing from ${dataFile.fileName} to ${newFileName}"
    return newFileName
}


def newSampleSwapScript = { StringBuilder script, Project newProject, Individual oldIndividual, String newIndividualName, Sample oldSample, String newSampleTypeName ->
    String fileName = "mv_${counter++}_${oldSample.individual.pid}_${oldSample.sampleType.name}__to__${newIndividualName}_${newSampleTypeName}"

    script << "\n\t\tdataSwapService.moveSample('${oldIndividual.project.name}', '${newProject.name}',\n" +
            "\t\t\t'${oldIndividual.pid}', '${newIndividualName}',\n" +
            "\t\t\t'${oldSample.sampleType.name}', '${newSampleTypeName}',\n" +
            "["
    SeqTrack.findAllBySample(oldSample, [sort: 'id']).each { SeqTrack seqTrack ->
        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
            script << "\n\t\t\t\t'${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividual.pid, newIndividualName)}',"
        }
    }
    script << "\n],\n\t\t'${fileName}', log, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY, verifiedLinkedFiles)\n"

    files << fileName
}

def createScript = { ->
    // buffers for all the changes we are preparing
    StringBuilder script = new StringBuilder()
    Set<String> createdPids = [] as Set
    Set<String> createdSamples = [] as Set

    // prepare the console script that fixes the DB-side of things
    println """
/****************************************************************
 * META DESCRIPTION
 *
 * What will change?
 ****************************************************************/"""

    script << """
/****************************************************************
 * DATABASE FIXING
 *
 * OTP console script to move the database-side of things
 ****************************************************************/

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.config.*

import static org.springframework.util.Assert.*

DataSwapService dataSwapService = ctx.dataSwapService
StringBuilder log = new StringBuilder()
final String SCRIPT_OUTPUT_DIRECTORY = "\${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

/** did we manually check yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
boolean verifiedLinkedFiles = false

/** are missing fastq files an error? (usually yes, since we must redo most analyses after a swap) */
boolean failOnMissingFiles = true

try {
    Individual.withTransaction {
"""
    swapMap.each { String key, String value ->
        println "swap of '${key}' to '${value}'"
        script << "\n\t\t//swap '${key}' to '${value}'"
        String oldIndividualName, oldSampleTypeName, newIndividualName, newSampleTypeName

        // figure out who/what we're swapping (decode the swapMap inputs into database objects)
        if (key.contains(' ')) {
            (oldIndividualName, oldSampleTypeName) = key.split(' +')
            (newIndividualName, newSampleTypeName) = value.split(' +')
        } else {
            oldIndividualName = key
            newIndividualName = value
        }
        Individual oldIndividual = Individual.findByPid(oldIndividualName)
        assert oldIndividual
        Individual newIndividual = Individual.findByPid(newIndividualName)
        Project newProject = newProjectName ? CollectionUtils.exactlyOneElement(Project.findAllByName(newProjectName)) : oldIndividual.project

        List<Sample> samples = Sample.findAllByIndividual(oldIndividual, [sort: 'id'])
        // simplest case: entire patient should be renamed
        if (!newIndividual && newSampleTypeName == null && seqTypeFilterList.empty) {
            String fileName = "mv_${counter++}_${oldIndividualName}__to__${newIndividualName}"
            script << "\ndataSwapService.moveIndividual('${oldIndividual.project.name}', '${newProject.name}', '${oldIndividualName}', '${newIndividualName}', ["
            samples.each { sample -> script << "'${sample.sampleType.name}': '${newSampleTypeClosure(sample.sampleType.name)}', " }
            script << "], ["
            samples.each { sample ->
                SeqTrack.findAllBySample(sample, [sort: 'id']).each { SeqTrack seqTrack ->
                    DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
                        script << "\n\t\t'${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividualName, newIndividualName)}',"
                    }
                }
            }
            script << "\n], '${fileName}', log, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY)\n"
            files << fileName
        } else {
            // moving one single sample in its entirety
            if (oldSampleTypeName || seqTypeFilterList) {
                SampleType oldSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(oldSampleTypeName))
                Sample oldSample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(oldIndividual, oldSampleType))

                // create our recipient patient, if needed
                boolean individualExists = newIndividual || createdPids.contains(newIndividualName)
                if (!individualExists) {
                    script << """
assert new Individual(
\tpid: '${newIndividualName}',
\tmockPid: '${newIndividualName}',
\tmockFullName: '${newIndividualName}',
\ttype: Individual.Type.REAL,
\tproject: Project.findByName('${newProject.name}'),
).save(flush: true, failOnError: true) : "Error creating new Individual '${newIndividualName}'"
"""
                    createdPids << newIndividualName
                }


                SampleType newSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(newSampleTypeName))
                Sample newSample = CollectionUtils.atMostOneElement(Sample.findAllByIndividualAndSampleType(newIndividual, newSampleType))
                // create recipient Sample if needed
                boolean sampleExists = newSample || createdSamples.contains("${newIndividualName} ${newSampleType.name}".toString())
                if (!sampleExists && seqTypeFilterList) {
                    script << """
assert new Sample(
\tindividual: CollectionUtils.exactlyOneElement(Individual.findAllByPid('${newIndividualName}')),
\tsampleType: CollectionUtils.exactlyOneElement(SampleType.findAllByName('${newSampleTypeName}'))
).save(flush: true, failOnError: true) : "Error creating new Sample '${newIndividualName} ${newSampleTypeName}'"
"""
                    createdSamples << "${newIndividualName} ${newSampleTypeName}".toString()
                    sampleExists = true
                }
                if (sampleExists) {
                    List<SeqTrack> oldSeqTracks = SeqTrack.findAllBySample(oldSample, [sort: 'id'])
                    if (seqTypeFilterList) {
                        oldSeqTracks = oldSeqTracks.findAll {
                            seqTypeFilterList.contains(it.seqType)
                        }
                    }
                    oldSeqTracks.each { SeqTrack seqTrack ->
                        String fileName = "mv_${counter++}_${oldIndividual.pid}_${oldSampleType.name}_${seqTrack.run.name}_${seqTrack.laneId}__to__${newIndividualName}_${newSampleTypeName}"
                        script << """
dataSwapService.swapLane([
\t\t'oldProjectName'   : '${oldIndividual.project.name}',
\t\t'newProjectName'   : '${newProject.name}',
\t\t'oldPid'           : '${oldIndividual.pid}',
\t\t'newPid'           : '${newIndividual.pid}',
\t\t'oldSampleTypeName': '${oldSampleType.name}',
\t\t'newSampleTypeName': '${newSampleType.name}',
\t\t'oldSeqTypeName'   : '${seqTrack.seqType.name}',
\t\t'newSeqTypeName'   : '${seqTrack.seqType.name}',
\t\t'oldSingleCell'    : '${seqTrack.seqType.singleCell}',
\t\t'newSingleCell'    : '${seqTrack.seqType.singleCell}',
\t\t'oldLibraryLayout' : '${seqTrack.seqType.libraryLayout}',
\t\t'newLibraryLayout' : '${seqTrack.seqType.libraryLayout}',
\t\t'runName'          : '${seqTrack.run.name}',
\t\t'lane'             : ['${seqTrack.laneId}'],
], ["""
                        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
                            script << "\n\t\t'${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividualName, newIndividualName)}',"
                        }
                        script << "\n], '${fileName}', log, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY)\n"
                        files << fileName
                    }
                } else {
                    newSampleSwapScript(script, newProject, oldIndividual, newIndividualName, oldSample, newSampleTypeName)
                    createdSamples << "${newIndividualName} ${newSampleTypeName}".toString()
                }
            } else { // moving ALL the samples for a patient
                samples.each { Sample sample ->
                    newSampleSwapScript(script, newProject, oldIndividual, newIndividualName, sample, newSampleTypeClosure(sample.sampleType.name))
                    createdSamples << "${newIndividualName} ${newSampleTypeName}".toString()
                }
            }

            // if we moved _everything_ out of a patient, delete the leftover empty patient
            if (!oldSampleTypeName && seqTypeFilterList.empty) {
                script << "\nIndividual.findByPid('${oldIndividualName}').delete(flush: true)\n"
            }
        }
    }
    script << """
\t\tassert false : "DEBUG: transaction intentionally failed to rollback changes"
\t}
} finally {
\tprintln log
}

"""

    script << """
/****************************************************************
 * FILESYSTEM FIXING
 *
 * meta-Bash script; calls all generated bash-scripts to fix
 * the filesystem-side of things.
 *
 * execute this after the database-side of things has been updated
 ****************************************************************/

/*
${
        DataSwapService.bashHeader + files.collect {
            "bash ${it}.sh"
        }.join('\n')
    }
*/
"""
    println "\n"
    return script as String
}

println createScript()
