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
 * For example: SeqType.wholeGenomePairedSeqType
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
        println "  data file name not change: ${newFileName}"
        return ''
    }
    println "  change data file name from ${dataFile.fileName} to ${newFileName}"
    return newFileName
}


def newSampleSwapScript = { StringBuilder script, Project newProject, Individual oldIndividual, String newIndividualName, Sample oldSample, String newSampleTypeName ->
    String fileName = 'mv_${counter++}_${oldSample.individual.pid}_${oldSample.sampleType.name}__to__${newIndividualName}_${newSampleTypeName}'
    script << "\ndataSwapService.moveSample('${oldIndividual.project.name}', '${newProject.name}', '${oldIndividual.pid}', '${newIndividualName}', '${oldSample.sampleType.name}', '${newSampleTypeName}', ["
    SeqTrack.findAllBySample(oldSample, [sort: 'id']).each { SeqTrack seqTrack ->
        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile -> script << "\n        '${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividual.pid, newIndividualName)}'," }
    }
    script << "\n], '${fileName}', outputStringBuilder, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY, linkedFilesVerified)\n"
    files << fileName
}

def createScript = { ->
    StringBuilder script = new StringBuilder()
    Set<String> createdPids = [] as Set
    Set<String> createdSamples = [] as Set
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
"""
    swapMap.each { String key, String value ->
        println "swap '${key}' to '${value}'"
        script << "\n//swap '${key}' to '${value}'"
        String oldIndividualName, oldSampleTypeName, newIndividualName, newSampleTypeName
        if (key.contains(' ')) {
            (oldIndividualName, oldSampleTypeName) = key.split(' ')
            (newIndividualName, newSampleTypeName) = value.split(' ')
        } else {
            oldIndividualName = key
            newIndividualName = value
        }
        Individual oldIndividual = Individual.findByPid(oldIndividualName)
        assert oldIndividual
        Individual newIndividual = Individual.findByPid(newIndividualName)
        Project newProject = newProjectName ? CollectionUtils.exactlyOneElement(Project.findAllByName(newProjectName)) : oldIndividual.project

        List<Sample> samples = Sample.findAllByIndividual(oldIndividual, [sort: 'id'])
        if (!newIndividual && newSampleTypeName == null && seqTypeFilterList.empty) {
            String fileName = 'mv_${counter++}_${oldIndividualName}__to__${newIndividualName}'
            script << "\ndataSwapService.moveIndividual('${oldIndividual.project.name}', '${newProject.name}', '${oldIndividualName}', '${newIndividualName}', ["
            samples.each { sample -> script << "'${sample.sampleType.name}': '${newSampleTypeClosure(sample.sampleType.name)}', " }
            script << "], ["
            samples.each { sample ->
                SeqTrack.findAllBySample(sample, [sort: 'id']).each { SeqTrack seqTrack ->
                    DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
                        script << "\n        '${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividualName, newIndividualName)}',"
                    }
                }
            }
            script << "\n], '${fileName}', outputStringBuilder, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY)\n"
            files << fileName
        } else {
            if (oldSampleTypeName || seqTypeFilterList) {
                SampleType oldSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(oldSampleTypeName))
                Sample oldSample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(oldIndividual, oldSampleType))
                if (!newIndividual && !createdPids.contains(newIndividualName)) {
                    script << """
assert new Individual(
    pid: '${newIndividualName}',
    mockPid: '${newIndividualName}',
    mockFullName: '${newIndividualName}',
    type: Individual.Type.REAL,
    project: Project.findByName('${newProject.name}'),
).save(flush: true, failOnError: true)
"""
                    createdPids << newIndividualName
                }
                SampleType newSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(newSampleTypeName))
                Sample newSample = CollectionUtils.atMostOneElement(Sample.findAllByIndividualAndSampleType(newIndividual, newSampleType))
                boolean sampleExist = newSample || createdSamples.contains("${newIndividualName} ${newSampleType.name}".toString())
                if (!sampleExist && seqTypeFilterList) {
                    script << """
assert new Sample(
    individual: CollectionUtils.exactlyOneElement(Individual.findAllByPid('${newIndividualName}')),
    sampleType: CollectionUtils.exactlyOneElement(SampleType.findAllByName('${newSampleTypeName}'))
).save(flush: true, failOnError: true)
"""
                    createdSamples << "${newIndividualName} ${newSampleTypeName}".toString()
                    sampleExist = true
                }
                if (sampleExist) {
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
        'oldProjectName'   : '${oldIndividual.project.name}',
        'newProjectName'   : '${newProject.name}',
        'oldPid'           : '${oldIndividual.pid}',
        'newPid'           : '${newIndividual.pid}',
        'oldSampleTypeName': '${oldSampleType.name}',
        'newSampleTypeName': '${newSampleType.name}',
        'oldSeqTypeName'   : '${seqTrack.seqType.name}',
        'newSeqTypeName'   : '${seqTrack.seqType.name}',
        'oldLibraryLayout' : '${seqTrack.seqType.libraryLayout}',
        'newLibraryLayout' : '${seqTrack.seqType.libraryLayout}',
        'runName'          : '${seqTrack.run.name}',
        'lane'             : ['${seqTrack.laneId}'],
], ["""
                        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
                            script << "\n        '${datafile.fileName}': '${newDataFileNameClosure(datafile, oldIndividualName, newIndividualName)}',"
                        }
                        script << "\n], '${fileName}', outputStringBuilder, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY)\n"
                        files << fileName
                    }
                } else {
                    newSampleSwapScript(script, newProject, oldIndividual, newIndividualName, oldSample, newSampleTypeName)
                    createdSamples << "${newIndividualName} ${newSampleTypeName}".toString()
                }
            } else {
                samples.each { Sample sample ->
                    newSampleSwapScript(script, newProject, oldIndividual, newIndividualName, sample, newSampleTypeClosure(sample.sampleType.name))
                    createdSamples << "${newIndividualName} ${newSampleTypeName}".toString()
                }
            }
            if (!oldSampleTypeName && seqTypeFilterList.empty) {
                script << "\nIndividual.findByPid('${oldIndividualName}').delete(flush: true)\n"
            }
        }
    }
    script << """
        assert false
    }
} finally {
    println outputStringBuilder
}

"""

    script << """
//scripts to execute
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
