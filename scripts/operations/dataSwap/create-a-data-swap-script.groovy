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
import de.dkfz.tbi.otp.project.Project
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


/*********************************************************************************
 * Input area
 *
 * This block should be all that needs to be changed to generate the swap.
 ********************************************************************************/


swapLabel = 'OTRS-________________-something-descriptive'

def swapMapDelimitor = " "
def swapMap = """
#oldPid newPid
#pid oldSampleType pid newSampleType
#oldPid oldSampleType newPid newSampleType
#oldPid sampleType newPid sampleType


""".split("\n")*.trim().findAll {
    it && !it.startsWith('#')
}.collectEntries {
    def entries = it.split(swapMapDelimitor)
    if (entries.size() == 4) {
        return ["${entries[0]} ${entries[1]}", "${entries[2]} ${entries[3]}"]
    } else {
        return ["${entries[0]}", "${entries[1]}"]
    }
}

//Only need, if the project should be changed. If the new Individual already exist, it have to belong to this project.
String newProjectName = ''

/**
 * Allow to filter for specific seqTypes. If given, only SeqTracks of the given SeqTypes will be swapped.
 *
 * For example: SeqTypeService.wholeGenomePairedSeqType
 */
List<SeqType> seqTypeFilterList = [
]

/**
 * User-defined function to map old SampleType name into a new SampleType name
 *
 * This closure is passed into the swapping-logic to handle more complicated swaps
 * default: identity pass-through.
 */
Closure<SampleType> newSampleTypeClosure = { SampleType sampleType ->
    return sampleType
}

/**
 * User-defined function to map old DataFile names into new DataFile names.
 *
 * This closure is passed into the swapping-logic to handle more complicated swaps
 * default: replace old-pid with new-pid, keep rest unchanged.
 */
Closure<String> newDataFileNameClosure = { DataFile dataFile, String oldPatientName, String newPatientName ->
    String oldFileName = dataFile.fileName
    String newFileName = oldFileName.replace(oldPatientName, newPatientName)

    if (oldFileName == newFileName) {
        println "\t- data file name remains unchanged: ${newFileName}"
        return ''
    } else {
        println "\t- data file name changing from ${dataFile.fileName} to ${newFileName}"
        return newFileName
    }
}

/*********************************************************************************
 * Script area
 *
 * Here follows logic that really should be in a service.
 * It should not change depending on the swap-details.
 ********************************************************************************/

int counter = 1
List<String> files = []

Closure<Integer> newSampleSwapScript = { StringBuilder script, Project newProject,
                                         Individual oldIndividual, String newIndividualName,
                                         Sample oldSample, SampleType newSampleType ->
    String fileName = "mv_${counter++}_${oldSample.individual.pid}_${oldSample.sampleType.name}__to__${newIndividualName}_${newSampleType.displayName}"

    script << "\n\tdataSwapService.moveSample( \n" +
            "\t\t[\n" +
            "\t\toldProjectName: '${oldIndividual.project.name}',\n" +
            "\t\tnewProjectName: '${newProject.name}',\n" +
            "\t\toldPid: '${oldIndividual.pid}',\n" +
            "\t\tnewPid: '${newIndividualName}',\n" +
            "\t\toldSampleTypeName: '${oldSample.sampleType.name}',\n" +
            "\t\tnewSampleTypeName: '${newSampleType.name}',\n" +
            "\t\t],\n" +
            "\t\t[\n"
    SeqTrack.findAllBySample(oldSample, [sort: 'id']).each { SeqTrack seqTrack ->
        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
            script << "\t\t'${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividual.pid, newIndividualName)}', \n"
        }
    }
    script << "\t\t],\n\t\t'${fileName}', log, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY, verifiedLinkedFiles\n\t)\n"

    files << fileName

    return counter
}

Closure<String> createScript = { String swapLabel ->
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
    println "/*"

    script << Snippets.databaseFixingHeader(swapLabel)

    swapMap.each { String key, String value ->
        println "swap of '${key}' to '${value}'"
        script << "\n\t//swap '${key}' to '${value}'"
        def (Individual oldIndividual, String newIndividualName, SampleType oldSampleType, SampleType newSampleType) = parseSwapMapEntry(key, value)

        Individual newIndividual = Individual.findByPid(newIndividualName)

        Project newProject = newProjectName ? CollectionUtils.exactlyOneElement(Project.findAllByName(newProjectName)) : oldIndividual.project

        List<Sample> samples = Sample.findAllByIndividual(oldIndividual, [sort: 'id'])

        // simplest case: move entire patient
        if (newSampleType == null && seqTypeFilterList.empty && // only if we're moving entire, unfiltered patients...
                (!newIndividual || newProject != oldIndividual.project ) // .. either into a shiny new patient, or into another project entirely
        ) {
            counter = renamePatient(newIndividualName, oldIndividual, newProject, samples, counter, script, newSampleTypeClosure, newDataFileNameClosure, files)
        } else { // more complex case: moving partial source, or into non-empty destination
            // moving one single sample in its entirety
            if (oldSampleType || seqTypeFilterList) {
                counter = moveOneSample(newProject,
                        oldIndividual, newIndividual, newIndividualName,
                        oldSampleType, newSampleType,
                        seqTypeFilterList,
                        newDataFileNameClosure,
                        newSampleSwapScript,
                        counter, script, files,
                        createdPids, createdSamples
                )
            } else { // moving ALL the samples for a patient (e.g. into another existing patient)
                counter = moveAllSamples(samples,
                        newProject,
                        oldIndividual, newIndividualName,
                        newSampleTypeClosure,
                        newSampleSwapScript,
                        counter, script,
                        createdSamples
                )
                if (seqTypeFilterList.empty) {
                    // if we moved _everything_ out of a patient, delete the leftover empty patient
                    script << "\n\tIndividual.findByPid('${oldIndividual.pid}').delete(flush: true)\n"
                }
            }
        }
    }
    println "*/\n"
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
        DataSwapService.BASH_HEADER + files.collect {
            "bash ${it}.sh"
        }.join('\n')
    }
*/
"""
    println "\n"
    return script as String
}

println createScript(swapLabel)


/****************************************************************
 * refactoring milestone:
 * END chaotic closures mixed with in-line script
 * BEGIN organised helper methods
 ****************************************************************/

private Tuple parseSwapMapEntry(String from, String to) {
    String oldIndividualName, newIndividualName, oldSampleTypeName, newSampleTypeName

    // figure out who/what we're swapping (decode the swapMap inputs into database objects)
    if (from.contains(' ')) {
        (oldIndividualName, oldSampleTypeName) = from.split(' +')
        (newIndividualName, newSampleTypeName) = to.split(' +')
    } else {
        oldIndividualName = from
        newIndividualName = to
    }

    Individual oldIndividual = Individual.findByPid(oldIndividualName)
    assert oldIndividual: "probable error in input: couldn't find Individual \"${oldIndividualName}\" in database"

    SampleType oldSampleType
    if (oldSampleTypeName) {
        oldSampleType = SampleType.findByName(oldSampleTypeName)
        assert oldSampleType: "probable error in input: couldn't find old SampleType \"${oldSampleTypeName}\" in database"
    }
    SampleType newSampleType
    if (newSampleTypeName) {
        newSampleType = SampleType.findByName(newSampleTypeName)
        assert newSampleType: "probable error in input: couldn't find new SampleType \"${newSampleTypeName}\" in database"
    }

    return new Tuple(oldIndividual, newIndividualName, oldSampleType, newSampleType)
}

private int moveAllSamples(List<Sample> samples,
                           Project newProject,
                           Individual oldIndividual, String newIndividualName,
                           Closure<SampleType> newSampleTypeClosure,
                           Closure newSampleSwapScript,
                           int counter,
                           StringBuilder script,
                           Set<String> createdSamples) {
    samples.each { Sample sample ->
        counter = newSampleSwapScript(script, newProject, oldIndividual, newIndividualName, sample, newSampleTypeClosure(sample.sampleType))
        createdSamples << "${newIndividualName} ${newSampleTypeClosure(sample.sampleType)}".toString()
    }

    return counter
}

private int moveOneSample(Project newProject,
                          Individual oldIndividual, Individual newIndividual, String newIndividualName,
                          SampleType oldSampleType, SampleType newSampleType,
                          List<SeqType> seqTypeFilterList,
                          Closure<String> newDataFileNameClosure,
                          Closure newSampleSwapScript,
                          int counter,
                          StringBuilder script, List<String> files,
                          Set<String> createdPids, Set<String> createdSamples
) {
    Sample oldSample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(oldIndividual, oldSampleType),
            "couldn't find old sample for \"${oldIndividual.displayName} ${oldSampleType.displayName}\"")

    // create our recipient patient, if needed
    boolean individualExists = newIndividual || createdPids.contains(newIndividualName)
    if (!individualExists) {
        script << Snippets.createIndividual(newIndividualName, newProject.name)
        createdPids << newIndividualName
    }

    Sample newSample = CollectionUtils.atMostOneElement(Sample.findAllByIndividualAndSampleType(newIndividual, newSampleType),
            "couldn't find new sample \"${newIndividualName} ${newSampleType.displayName}\"")
    // create recipient Sample if needed
    boolean sampleExists = newSample || createdSamples.contains("${newIndividualName} ${newSampleType.displayName}".toString())
    if (!sampleExists && seqTypeFilterList) {
        script << Snippets.createSample(newIndividualName, newSampleType.displayName)
        createdSamples << "${newIndividualName} ${newSampleType.displayName}".toString()
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
            String fileName = "mv_${counter++}_${oldIndividual.pid}_${oldSampleType.name}_${seqTrack.run.name}_${seqTrack.laneId}__to__${newIndividualName}_${newSampleType.displayName}"
            script << Snippets.swapLane(seqTrack, fileName, newDataFileNameClosure,
                    newProject,
                    oldIndividual, newIndividualName,
                    oldSampleType, newSampleType)
            files << fileName
        }
    } else {
        counter = newSampleSwapScript(script, newProject, oldIndividual, newIndividualName, oldSample, newSampleType)
        createdSamples << "${newIndividualName} ${newSampleType.displayName}".toString()
    }

    return counter
}


private int renamePatient(String newIndividualName, Individual oldIndividual,
                          Project newProject,
                          List<Sample> samples, int counter, StringBuilder script,
                          Closure<SampleType> newSampleTypeClosure, Closure<String> newDataFileNameClosure,
                          List<String> files
) {
    String fileName = "mv_${counter++}_${oldIndividual.pid}__to__${newIndividualName}"

    script << "\n\tdataSwapService.moveIndividual(\n" +
            "\t\t[\n" +
            "\t\t'oldProjectName' :'${oldIndividual.project.name}',\n" +
            "\t\t'newProjectName' : '${newProject.name}',\n" +
            "\t\t'oldPid' : '${oldIndividual.pid}',\n" +
            "\t\t'newPid' : '${newIndividualName}',\n" +
            "\t\t],\n" +
            "\t\t[\n"
    samples.each { sample ->
        script << "\t\t'${sample.sampleType.name}': '${newSampleTypeClosure(sample.sampleType).name}', \n"
    }
    script << "\t\t],"

    script << "\n\t\t[\n"
    samples.each { sample ->
        SeqTrack.findAllBySample(sample, [sort: 'id']).each { SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
                script << "\t\t'${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividual.pid, newIndividualName)}',\n"
            }
        }
    }
    script << "\t\t],\n"
    script << "\t\t'${fileName}', log, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY, verifiedLinkedFiles\n" +
            "\t)\n"
    files << fileName

    return counter
}



class Snippets {
    static String databaseFixingHeader(String swapLabel) {
        return """
/****************************************************************
 * DATABASE FIXING
 *
 * OTP console script to move the database-side of things
 ****************************************************************/

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
DataSwapService dataSwapService = ctx.dataSwapService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

StringBuilder log = new StringBuilder()

// create a container dir for all output of this swap;
// group-editable so non-server users can also work with it
String swapLabel = "${swapLabel}"
final Path SCRIPT_OUTPUT_DIRECTORY = fileService.toPath(configService.getScriptOutputPath(), fileSystem).resolve('sample_swap').resolve(swapLabel)
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(SCRIPT_OUTPUT_DIRECTORY, realm)
fileService.setPermission(SCRIPT_OUTPUT_DIRECTORY, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

/** did we manually check yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
boolean verifiedLinkedFiles = false

/** are missing fastq files an error? (usually yes, since we must redo most analyses after a swap) */
boolean failOnMissingFiles = true

try {
    Individual.withTransaction {
"""
    }

    static String createIndividual(String newIndividualName, String newProjectName) {
        return """
assert new Individual(
\tpid: '${newIndividualName}',
\tmockPid: '${newIndividualName}',
\tmockFullName: '${newIndividualName}',
\ttype: Individual.Type.REAL,
\tproject: Project.findByName('${newProjectName}'),
).save(flush: true, failOnError: true) : "Error creating new Individual '${newIndividualName}'"
"""
    }

    static String createSample(String newIndividualName, String newSampleTypeName) {
        return """
assert new Sample(
\tindividual: CollectionUtils.exactlyOneElement(Individual.findAllByPid('${newIndividualName}')),
\tsampleType: CollectionUtils.exactlyOneElement(SampleType.findAllByName('${newSampleTypeName}'))
).save(flush: true, failOnError: true) : "Error creating new Sample '${newIndividualName} ${newSampleTypeName}'"
"""
    }


    static String swapLane(SeqTrack seqTrack, String fileName, Closure<String> newDataFileNameClosure,
                           Project newProject,
                           Individual oldIndividual, String newIndividual,
                           SampleType oldSampleType, SampleType newSampleType) {
        StringBuilder snippet = new StringBuilder()
        snippet << """
\tdataSwapService.swapLane([
\t\t'oldProjectName'   : ['${oldIndividual.project.name}'],
\t\t'newProjectName'   : ['${newProject.name}'],
\t\t'oldPid'           : ['${oldIndividual.pid}'],
\t\t'newPid'           : ['${newIndividual}'],
\t\t'oldSampleTypeName': ['${oldSampleType.name}'],
\t\t'newSampleTypeName': ['${newSampleType.name}'],
\t\t'oldSeqTypeName'   : ['${seqTrack.seqType.name}'],
\t\t'newSeqTypeName'   : ['${seqTrack.seqType.name}'],
\t\t'oldSingleCell'    : ['${seqTrack.seqType.singleCell}'],
\t\t'newSingleCell'    : ['${seqTrack.seqType.singleCell}'],
\t\t'oldLibraryLayout' : ['${seqTrack.seqType.libraryLayout}'],
\t\t'newLibraryLayout' : ['${seqTrack.seqType.libraryLayout}'],
\t\t'runName'          : ['${seqTrack.run.name}'],
\t\t'lane'             : ['${seqTrack.laneId}'],
\t\t'sampleNeedsToBeCreated': ['false'],
\t\t], [
"""
        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
            snippet << "\t\t\t'${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividual.pid, newIndividual)}',\n"
        }
        snippet << "\t\t],\n\t\t'${fileName}',\n" +
                "\t\tlog, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY\n\t)\n"

        return snippet.toString()
    }
}
