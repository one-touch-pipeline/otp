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
 * The swap is executed in two steps:
 *   - creation of missing Samples and SampleTypes, which are required for the swap
 *   - creation of swap script
 */


/*********************************************************************************
 * Input area
 *
 * This block should be all that needs to be changed to generate the swap.
 ********************************************************************************/

swapLabel = 'OTRS-________________-something-descriptive'

Map<String, String> swapMap = [
        ('pid sampleType sampleIdentifier'): 'newPid newSampleType',
]

/*********************************************************************************
 * Script area
 *
 * Here follows logic that really should be in a service.
 * It should not change depending on the swap-details.
 ********************************************************************************/

List<String> files = []

Closure<ScriptOutput> createSamplesAndSampleTypesCreationScript = { List<String> sampleNames ->
    ScriptOutput scriptOutput = new ScriptOutput()

    scriptOutput.meta << "Objects to be created:"

    List<SampleComponents> uniqueParsedSamples = sampleNames.unique().collect { String sampleName -> parseSampleAndGetComponents(sampleName) }

    scriptOutput.script << Snippets.databaseFixingBanner()
    scriptOutput.script << "import de.dkfz.tbi.otp.utils.CollectionUtils\n"

    scriptOutput.script << "Sample.withTransaction {"

    List<SampleType> existingSampleTypes = SampleType.createCriteria().list {
        or {
            (uniqueParsedSamples*.sampleTypeName.unique()).each {
                ilike("name", it)
            }
        }
    }

    List<String> expectedSampleTypes = uniqueParsedSamples*.sampleTypeName.unique()
    List<String> sampleTypesToBeCreated = expectedSampleTypes - existingSampleTypes*.name

    scriptOutput.meta << "  * new SampleTypes:"
    sampleTypesToBeCreated.each { String name ->
        scriptOutput.meta << "    - ${name}"
        scriptOutput.script << Snippets.indent(Snippets.createSampleType(name), 1)
        scriptOutput.containsChanges = true
    }

    scriptOutput.meta << "  * new Samples:"
    uniqueParsedSamples.each { SampleComponents components ->
        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(components.pid))

        SampleType sampleType = SampleType.findSampleTypeByName(components.sampleTypeName)
        Sample sample = Sample.findByIndividualAndSampleType(individual, sampleType)

        if (!sample) {
            scriptOutput.meta << "    - ${components.pid} ${components.sampleTypeName}"
            scriptOutput.script << Snippets.indent(Snippets.createSample(components.pid, components.sampleTypeName), 1)
            scriptOutput.containsChanges = true
        }
    }

    scriptOutput.script << "}"

    return scriptOutput
}

int counter = 1
Closure<ScriptOutput> createSwapScript = { String swapLabel ->
    ScriptOutput scriptOutput = new ScriptOutput()

    scriptOutput.script << Snippets.databaseFixingHeader(swapLabel)

    swapMap.each { String from, String to ->
        ParsedSwapMapEntry parsedEntry = ParsedSwapMapEntry.parse(from, to)

        scriptOutput.meta << "${parsedEntry.oldSample} [${parsedEntry.identifier}] --> ${parsedEntry.newSample}"

        Map<SeqTrack, List<DataFile>> dataFilesPerSeqTrack = getDataFilesBySampleIdentifierString(parsedEntry.identifier).groupBy { it.seqTrack }
        dataFilesPerSeqTrack.eachWithIndex { SeqTrack seqTrack, List<DataFile> dataFiles, int index ->
            String fileName = "mv_${counter++}_${parsedEntry.oldSampleString}_ST_${index}_${seqTrack.laneId}__to__${parsedEntry.newSampleString}"

            scriptOutput.meta << "  * ${seqTrack.laneId}  ${seqTrack.seqType}"

            dataFiles.each { DataFile dataFile ->
                scriptOutput.meta << "    - ${dataFile}"
            }

            scriptOutput.script << Snippets.indent(Snippets.swapLane(seqTrack, fileName, parsedEntry.newSample), 2)

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
    DataSwapService.BASH_HEADER + files.collect {
        "bash ${it}.sh"
    }.join('\n')
}
*/
"""

    return scriptOutput
}

/*********************************************************************************
 * Script execution
 * This is the actual part of the script which is executed when calling it.
 ********************************************************************************/

ScriptOutput samplesAndTypesScriptOutput = createSamplesAndSampleTypesCreationScript(swapMap.values() as List<String>)
if (samplesAndTypesScriptOutput.containsChanges) {
    samplesAndTypesScriptOutput.printToStdout()
    return
}

createSwapScript(swapLabel).printToStdout()


/*********************************************************************************
 * Utility Functions and Classes
 ********************************************************************************/

private List<DataFile> getDataFilesBySampleIdentifierString(String sampleIdentifier) {
    return DataFile.withCriteria {
        seqTrack {
            eq('sampleIdentifier', sampleIdentifier)
        }
    }
}

private SampleComponents parseSampleAndGetComponents(String sampleName) {
    return new SampleComponents(sampleName.split(" "))
}

class ScriptOutput {
    List<String> meta = []
    List<String> script = []
    boolean containsChanges = false

    String getStdoutReady() {
        return Snippets.encloseInMetaDescription(meta.join("\n")) + "\n" + (script.join("\n"))
    }

    void printToStdout() {
        println getStdoutReady()
    }
}

class SampleComponents {
    String pid
    String sampleTypeName

    SampleComponents(String[] componentList) {
        this.pid = componentList[0]
        this.sampleTypeName = componentList[1]
    }

    boolean equals(SampleComponents other) {
        return pid == other.pid && sampleTypeName == other.sampleTypeName
    }

    @Override
    String toString() {
        return "components: ${pid} ${sampleTypeName}"
    }
}

class ParsedSwapMapEntry {
    Sample oldSample
    String identifier
    Sample newSample

    ParsedSwapMapEntry(Sample oldSample, String identifier, Sample newSample) {
        this.oldSample = oldSample
        this.identifier = identifier
        this.newSample = newSample
    }

    static ParsedSwapMapEntry parse(String from, String to) {
        def (oldPid, oldSampleTypeName, identifier) = from.split(" ")
        def (newPid, newSampleTypeName) = to.split(" ")
        return new ParsedSwapMapEntry(
                getSampleFromPidAndSampleTypeName(oldPid, oldSampleTypeName),
                identifier,
                getSampleFromPidAndSampleTypeName(newPid, newSampleTypeName),
        )
    }

    String getOldSampleString() {
        getSampleStringForFilename(oldSample)
    }

    String getNewSampleString() {
        getSampleStringForFilename(newSample)
    }

    private static String getSampleStringForFilename(Sample sample) {
        return "${sample.individual.pid}_${sample.sampleType.name}"
    }

    private static Sample getSampleFromPidAndSampleTypeName(String pid, String sampleTypeName) {
        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(pid))

        SampleType sampleType = SampleType.findSampleTypeByName(sampleTypeName)
        assert sampleType: "SampleType '${sampleTypeName}' could not be found"

        Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))

        return sample
    }
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

    static String createSample(String pid, String sampleTypeName) {
        return """\
assert new Sample(
\tindividual: CollectionUtils.exactlyOneElement(Individual.findAllByPid('${pid}')),
\tsampleType: SampleType.findSampleTypeByName('${sampleTypeName}')
).save(flush: true, failOnError: true) : \"Error creating new Sample '${pid} ${sampleTypeName}'\""""
    }

    static String createSampleType(String name) {
        return "assert new SampleType(name: '${name}').save(flush: true, failOnError: true) : \"Error creating new SampleType '${name}'\""
    }

    static String swapLane(SeqTrack seqTrack, String fileName, Sample newSample) {
        StringBuilder snippet = new StringBuilder()
        snippet << """\
dataSwapService.swapLane(
\t[
\t\t'oldProjectName'   : '${seqTrack.sample.individual.project.name}',
\t\t'newProjectName'   : '${seqTrack.sample.individual.project.name}',
\t\t'oldPid'           : '${seqTrack.sample.individual.pid}',
\t\t'newPid'           : '${newSample.individual.pid}',
\t\t'oldSampleTypeName': '${seqTrack.sample.sampleType.name}',
\t\t'newSampleTypeName': '${newSample.sampleType.name}',
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

        snippet << "\t],\n\t'${fileName}',\n" +
                "\tlog, failOnMissingFiles, SCRIPT_OUTPUT_DIRECTORY\n)\n"

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
