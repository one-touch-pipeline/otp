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
package operations.dataSwap


import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Generation Script which allows to separate lanes of a Sample into different SampleTypes.
 *
 * The script contains three steps.
 *   - creation of missing SampleTypes
 *   - creation of missing Samples
 *   - creation of swap script
 */


/*********************************************************************************
 * Input area
 *
 * This block should be all that needs to be changed to generate the swap.
 ********************************************************************************/

swapLabel = 'OTRS-________________-something-descriptive'

String sampleName = "pid sampleTypeName"

Map<String, String> swapMap = [
        ('sampleIdentifier'): 'newSampleType',
]


/*********************************************************************************
 * Script area
 *
 * Here follows logic that really should be in a service.
 * It should not change depending on the swap-details.
 ********************************************************************************/

int counter = 1
List<String> files = []

Closure<ScriptOutput> createSampleTypeCreationScript = { Sample sample, List<String> sampleTypeNames ->
    ScriptOutput scriptOutput = new ScriptOutput()

    scriptOutput.meta << "Create new SampleTypes:"
    List<String> namesOfExistingSampleTypes = SampleType.findAllByNameInList(sampleTypeNames)*.name

    scriptOutput.script << Snippets.databaseFixingBanner()
    scriptOutput.script << "SampleType.withTransaction {"
    sampleTypeNames.each { String name ->
        if (!namesOfExistingSampleTypes.contains(name)) {
            scriptOutput.meta << "  - ${name}"
            scriptOutput.script << Snippets.indent(Snippets.createSampleType(name), 1)
        }
    }
    scriptOutput.script << "}"

    return scriptOutput
}

Closure<ScriptOutput> createSampleCreationScript = { Sample sample, List<String> sampleTypeNames ->
    ScriptOutput scriptOutput = new ScriptOutput()

    scriptOutput.meta << "Create new Samples for given Individual:"
    List<String> sampleTypeNamesOfExistingSamples = Sample.findAllByIndividual(sample.individual)*.sampleType.name

    scriptOutput.script << Snippets.databaseFixingBanner()
    scriptOutput.script << "import de.dkfz.tbi.otp.utils.CollectionUtils\n"
    scriptOutput.script << "Sample.withTransaction {"
    sampleTypeNames.each { String name ->
        if (!sampleTypeNamesOfExistingSamples.contains(name)) {
            scriptOutput.meta << "  - ${name}"
            scriptOutput.script << Snippets.indent(Snippets.createSampleForIndividual(sample.individual, name), 2)
        }
    }
    scriptOutput.script << "}"

    return scriptOutput
}

Closure<ScriptOutput> createSwapScript = { Sample sample, String swapLabel ->
    ScriptOutput scriptOutput = new ScriptOutput()
    MetaDataKey sampleIdKey = MetaDataKey.findByName("SAMPLE_ID")

    scriptOutput.script << Snippets.databaseFixingHeader(swapLabel)

    swapMap.each { String sampleIdentifier, String newSampleTypeName ->
        SampleType newSampleType = SampleType.findByName(newSampleTypeName)

        Map<SeqTrack, List<DataFile>> dataFilesPerSeqTrack = MetaDataEntry.findAllByKeyAndValue(sampleIdKey, sampleIdentifier)*.dataFile.groupBy { it.seqTrack }
        int seqTrackCounter = 1
        dataFilesPerSeqTrack.each { SeqTrack seqTrack, List<DataFile> dataFiles ->
            String fileName = "mv_${counter++}_${sample.individual.pid}_ST_${seqTrackCounter++}__${sample.sampleType.name}__to__${newSampleTypeName}"
            scriptOutput.meta << "Lane ${seqTrack.id}: ${seqTrack.sample.individual.pid} ${seqTrack.seqType}"
            scriptOutput.meta << "  ${seqTrack.sample.sampleType.name} -->  ${newSampleTypeName}"
            dataFiles.each { DataFile dataFile ->
                scriptOutput.meta << "  * ${dataFile}, ${sampleIdentifier}"
            }

            scriptOutput.script << Snippets.indent(Snippets.swapSampleTypeOfLane(seqTrack, fileName, newSampleType), 2)

            files << fileName
        }
    }

    scriptOutput.script << """\n
\t\tassert false : "DEBUG: transaction intentionally failed to rollback changes"
\t}
} finally {
\tprintln log
}

"""

    scriptOutput.script << """
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

    return scriptOutput
}


Sample sample = parseSample(sampleName)

List<String> uniqueSampleTypeNames = swapMap.values().toList().unique()
List<SampleType> allSampleTypes = SampleType.findAllByNameInList(uniqueSampleTypeNames)

boolean allSampleTypesCreated = (allSampleTypes.size() == uniqueSampleTypeNames.size())
if (!allSampleTypesCreated) {
    createSampleTypeCreationScript(sample, uniqueSampleTypeNames).printToStdout()
    return
}

boolean allSamplesCreated = Sample.findAllByIndividualAndSampleTypeInList(sample.individual, allSampleTypes)
if (!allSamplesCreated) {
    createSampleCreationScript(sample, uniqueSampleTypeNames).printToStdout()
    return
}

createSwapScript(sample, swapLabel).printToStdout()


class ScriptOutput {
    List<String> meta = []
    List<String> script = []

    String getStdoutReady() {
        return Snippets.encloseInMetaDescription(meta.join("\n")) + "\n" + (script.join("\n"))
    }

    void printToStdout() {
        println getStdoutReady()
    }
}


/*********************************************************************************
 * Utility Functions
 ********************************************************************************/

private Sample parseSample(String sampleName) {
    String[] split = sampleName.split(" ")
    assert split.size() == 2 : "Sample information could not be parsed, expected format: \"pid sampleTypeName\""
    return Sample.findByIndividualAndSampleType(
            CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0])),
            CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1])),
    )
}

class Snippets {

    static String indent(String target, int indentationLevel = 0, String indentationChar = "\t") {
        return target.replaceAll(/(?m)^/, indentationChar * indentationLevel)
    }

    static String databaseFixingBanner() {
        return """\
/****************************************************************
 * DATABASE FIXING
 *
 * OTP console script to move the database-side of things
 ****************************************************************/
"""
    }

    static String databaseFixingHeader(String swapLabel) {
        return """
${databaseFixingBanner()}
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

/** did we manually check yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
boolean verifiedLinkedFiles = false

/** are missing fastq files an error? (usually yes, since we must redo most analyses after a swap) */
boolean failOnMissingFiles = true

try {
\tIndividual.withTransaction {
"""
    }

    static String createSampleForIndividual(Individual individual, String newSampleTypeName) {
        return """
assert new Sample(
\tindividual: CollectionUtils.exactlyOneElement(Individual.findAllByPid('${individual.pid}')),
\tsampleType: CollectionUtils.exactlyOneElement(SampleType.findAllByName('${newSampleTypeName}'))
).save(flush: true, failOnError: true) : "Error creating new Sample '\${individual.pid} ${newSampleTypeName}'"
"""
    }

    static String createSampleType(String name) {
        return "assert new SampleType(name: '${name}').save(flush: true, failOnError: true) : \"Error creating new SampleType '${name}'\""
    }

    static String swapSampleTypeOfLane(SeqTrack seqTrack, String fileName, SampleType newSampleType) {
        StringBuilder snippet = new StringBuilder()
        snippet << """
dataSwapService.swapLane(
\t[
\t\t'oldProjectName'   : '${seqTrack.sample.individual.project.name}',
\t\t'newProjectName'   : '${seqTrack.sample.individual.project.name}',
\t\t'oldPid'           : '${seqTrack.sample.individual.pid}',
\t\t'newPid'           : '${seqTrack.sample.individual.pid}',
\t\t'oldSampleTypeName': '${seqTrack.sample.sampleType.name}',
\t\t'newSampleTypeName': '${newSampleType.name}',
\t\t'oldSeqTypeName'   : '${seqTrack.seqType.name}',
\t\t'newSeqTypeName'   : '${seqTrack.seqType.name}',
\t\t'oldSingleCell'    : '${seqTrack.seqType.singleCell}',
\t\t'newSingleCell'    : '${seqTrack.seqType.singleCell}',
\t\t'oldLibraryLayout' : '${seqTrack.seqType.libraryLayout}',
\t\t'newLibraryLayout' : '${seqTrack.seqType.libraryLayout}',
\t\t'runName'          : '${seqTrack.run.name}',
\t\t'lane'             : ['${seqTrack.laneId}'],
\t], [
"""
        DataFile.findAllBySeqTrack(seqTrack, [sort: 'id']).each { datafile ->
            snippet << "\t\t'${datafile.fileName}': '',\n"
        }

        snippet << "\t],\n\t\t'${fileName}',\n" +
                "\t\tlog, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY\n)\n"

        return snippet.toString()
    }

    static String encloseInMetaDescription(String enclosedContent) {
        return """
/****************************************************************
 * META DESCRIPTION
 *
 * What will change?
 ****************************************************************/
/*
${enclosedContent}
*/
"""
    }
}