/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*

/**
 * Script to:
 * 1. list all samples with roddy bam files
 * 2. creates a bam import metadata file based of existing roddy bam files.
 *
 * Depending on the columns of the first data raw in the input the script switch between this two cases.
 *
 * Use case 1: list all samples with roddy bam files
 * In this mode, the script search for given pids all roddy bam files and print them out in the manner required for the other use case of the script.
 * Therefore it needs already the mapping (new project and new pid) as input.
 * Also it checks, that NewPid and OldPid are different.
 *
 * The input is a table with following columns:
 * - new project
 * - new pid
 * - old pid
 *
 * The columns are separated by tab, each value will be trimmed. The order is fixed and no header column is used. Empty rows and rows starting with # are skipped.
 *
 * The output has the structure of the input for the import file creation.
 *
 *
 * Use case 2: Create a bam import file based for the selected bam files.
 * In this case, its create for each input row an entry for bam import.
 * Also it checks, that NewPid and OldPid are different.
 *
 * The input is a table with following columns:
 * - new project
 * - new pid
 * - old pid
 * - sample type
 * - seq type identifier, supported are:
 *   - WGS for WHOLE GENOME PAIRED bulk
 *   - WES  for WHOLE EXOME PAIRED bulk
 *
 * The columns are separated by tab, each value will be trimmed. The order is fixed and no header column is used. Empty rows and rows starting with # are skipped.
 *
 * The file is generated in the provided file. Missing parent directories are created, if necessary.
 *
 * The flag 'overwriteExisting' indicates, that an existing file should be replaced.
 */


//=============================================
// input area

//column order: NEW_PROJECT NEW_PID OLD_PID SAMPLE_TYPE WGS/WES
//WGS indicated 'WHOLE_GENOME PAIRD bulk', WES indicated 'EXOME PAIRED bulk'.
String input = """

"""

/**
 * delimiter for the input matrix. default value is '\t' for a tab
 */
String inputFieldDelimiter = '\t'

/**
 * Name of the file to generate. The name must be absolute. Its only to create the bam import file.
 */
String fileName = ''

/**
 * Flag to indicate, if existing files should be overwritten. Its only to create the bam import file.
 */
boolean overwriteExisting = false


//=============================================
// work area


String outputFieldDelimiter = '\t'

class BamExportImport {

    private static class InputData {
        String newProject
        String newPid
        Individual oldIndividual
        SampleType sampleType
        SeqType seqType

        RoddyBamFile bamFile
        Map<String, String> metadata
    }

    FileService fileService

    FileSystemService fileSystemService

    String inputFieldDelimiter

    String outputFieldDelimiter

    private SeqType getSeqTypeForIdentifier(String seqTypeIdentifier) {
        switch (seqTypeIdentifier) {
            case 'WGS':
                return SeqTypeService.wholeGenomePairedSeqType
            case 'WES':
                return SeqTypeService.exomePairedSeqType
            default:
                throw new OtpRuntimeException("Unknown value: '${seqTypeIdentifier}', only 'WES' and 'WGS' supported")
        }
    }

    private InputData parseInputLine(String inputLine) {
        List<String> values = inputLine.split(inputFieldDelimiter)*.trim()
        assert values.size() == 5: "The input needs exact 5 parts"
        InputData data = new InputData([
                newProject   : values[0],
                newPid       : values[1],
                oldIndividual: CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(values[2], values[2], values[2])),
                sampleType   : SampleType.findSampleTypeByName(values[3]),
                seqType      : getSeqTypeForIdentifier(values[4])
        ])
        assert data.sampleType: "could not find sampletype with name '${values[3]}'"
        if (values[1] == values[2]) {
            println "\n${values[1]} is used as oldPid and as newPid\n"
            throw new OtpRuntimeException("${values[1]} is used as oldPid and as newPid")
        }
        return data
    }

    private List<InputData> parseInputTable(Collection<String> input) {
        return input.collect { String line ->
            parseInputLine(line)
        }
    }

    private void searchAndAddRoddyBamFile(InputData inputData) {
        List<RoddyBamFile> bamFiles = RoddyBamFile.createCriteria().list {
            workPackage {
                sample {
                    'eq'('individual', inputData.oldIndividual)
                    'eq'('sampleType', inputData.sampleType)
                }
                eq('seqType', inputData.seqType)
            }
        }.findAll { RoddyBamFile bamFile ->
            bamFile.isMostRecentBamFile()
        }
        RoddyBamFile bamFile = CollectionUtils.exactlyOneElement(bamFiles, "Could not find exactly one bame file for ${inputData.oldIndividual} ${inputData.sampleType} ${inputData}")
        assert bamFile.fileOperationStatus == RoddyBamFile.FileOperationStatus.PROCESSED: "The found bam file for ${inputData.oldIndividual} ${inputData.sampleType} ${inputData} is still in processing"
        assert !bamFile.withdrawn: "The found bam file for ${inputData.oldIndividual} ${inputData.sampleType} ${inputData} is withdrawn"
        inputData.bamFile = bamFile
    }

    private String finalInsertSizeFile(RoddyBamFile bamFile) {
        Path absoluteInsertSizePath = bamFile.finalInsertSizeFile.toPath()
        Path bamFileDirectory = bamFile.baseDirectory.toPath()
        Path relativeInsertSizePath = bamFileDirectory.relativize(absoluteInsertSizePath)
        return relativeInsertSizePath.toString()
    }

    private String qualityControlPath(RoddyBamFile bamFile) {
        return 'qualitycontrol/merged/qualitycontrol.json'
    }

    private void fetchAndAddMetadata(InputData inputData) {
        RoddyBamFile bamFile = inputData.bamFile
        inputData.metadata = [
                (BAM_FILE_PATH)          : bamFile.finalBamFile.absolutePath,
                (MD5)                    : bamFile.md5sum,
                (PROJECT)                : inputData.newProject,
                (INDIVIDUAL)             : inputData.newPid,
                (SAMPLE_TYPE)            : bamFile.sampleType.name,
                (SEQUENCING_TYPE)        : bamFile.seqType.name,
                (SEQUENCING_READ_TYPE)   : bamFile.seqType.libraryLayout.toString(),
                (LIBRARY_PREPARATION_KIT): bamFile.workPackage.libraryPreparationKit?.name,
                (COVERAGE)               : bamFile.coverage?.toString(),
                (REFERENCE_GENOME)       : bamFile.referenceGenome.name,
                (MAXIMAL_READ_LENGTH)    : bamFile.maximalReadLength,
                (INSERT_SIZE_FILE)       : finalInsertSizeFile(bamFile),
                (QUALITY_CONTROL_FILE)   : qualityControlPath(bamFile),
                OLD_PROJECT              : bamFile.individual.project.name,
                OLD_INDIVIDUAL           : bamFile.individual.pid,
                OLD_SAMPLE_TYPE          : bamFile.sampleType.name,
        ].collectEntries { key, value ->
            [(key): value?.toString()]
        }
    }

    private String createFileContent(List<InputData> inputDataList) {
        List<String> content = []
        List<BamMetadataColumn> columns = inputDataList*.metadata*.keySet().flatten().unique()
        content << columns*.toString().join(outputFieldDelimiter)

        inputDataList.each { InputData inputData ->
            Map<String, String> metadata = inputData.metadata
            content << columns.collect { column ->
                metadata[column] ?: '' //ensure that null is replaced by empty string
            }.join(outputFieldDelimiter)
        }
        return content.join('\n')
    }

    Path convertAndCheckPath(String fileName, boolean overwriteExisting) {
        assert fileName: 'No file name given, but this is required'

        FileSystem fileSystem = fileSystemService.getRemoteFileSystemOnDefaultRealm()
        Path path = fileSystem.getPath(fileName)

        assert path.absolute: '"The file name is not absolute, but that is required'

        if (Files.exists(path)) {
            if (overwriteExisting) {
                Files.delete(path)
            } else {
                throw new OtpRuntimeException("The file ${path} already exist and overwrite is set to false")
            }
        }
        return path
    }

    void writeFile(Path path, String content) {
        fileService.createFileWithContentOnDefaultRealm(path, content, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION)
    }

    Path handleInput(List<String> input, String filename, boolean overwriteExisting) {
        List<InputData> inputDataList = parseInputTable(input).each { InputData inputData ->
            searchAndAddRoddyBamFile(inputData)
            fetchAndAddMetadata(inputData)
        }
        Path path = convertAndCheckPath(filename.trim(), overwriteExisting)
        String content = createFileContent(inputDataList)
        writeFile(path, content)

        return path
    }
}

class DisplaySamples {

    private static class InputData {
        String newProject
        String newPid
        Individual oldIndividual
        List<RoddyBamFile> bamFiles
    }

    String inputFieldDelimiter

    String outputFieldDelimiter

    private final Map<SeqType, String> seqTypeMap = [
            (SeqTypeService.wholeGenomePairedSeqType): 'WGS',
            (SeqTypeService.exomePairedSeqType)      : 'WES',
    ].asImmutable()

    private InputData parseInputLine(String inputLine) {
        List<String> values = inputLine.split(inputFieldDelimiter)*.trim()
        assert values.size() == 3: "The input needs exact 3 parts"
        InputData data = new InputData([
                newProject   : values[0],
                newPid       : values[1],
                oldIndividual: CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(values[2], values[2], values[2])),
        ])
        if (values[1] == values[2]) {
            println "\n${values[1]} is used as oldPid and as newPid\n"
            throw new OtpRuntimeException("${values[1]} is used as oldPid and as newPid")
        }
        return data
    }

    private List<InputData> parseInputTable(List<String> input) {
        return input.collect { String line ->
            parseInputLine(line)
        }
    }

    private void searchAndAddRoddyBamFiles(InputData inputData) {
        List<RoddyBamFile> bamFiles = RoddyBamFile.createCriteria().list {
            workPackage {
                sample {
                    'eq'('individual', inputData.oldIndividual)
                    sampleType {
                        order('name')
                    }
                }
                'in'('seqType', seqTypeMap.keySet())
                seqType {
                    order('name')
                }
            }
        }.findAll { RoddyBamFile bamFile ->
            bamFile.isMostRecentBamFile()
        }

        if (!bamFiles) {
            println "Could not find any roddy bam file for ${inputData.oldIndividual}"
            return
        }

        inputData.bamFiles = bamFiles.findAll { RoddyBamFile bamFile ->
            if (bamFile.fileOperationStatus != RoddyBamFile.FileOperationStatus.PROCESSED) {
                println "The bam file ${bamFile} is still in processing"
                return false
            } else if (bamFile.withdrawn) {
                println "The bam file ${bamFile} is withdrawn and is skipped therefore"
                return false
            } else {
                return true
            }
        }
    }

    private String createContent(List<InputData> inputDataList) {
        return inputDataList.collectMany { InputData inputData ->
            inputData.bamFiles.collect { RoddyBamFile roddyBamFile ->
                [
                        inputData.newProject,
                        inputData.newPid,
                        roddyBamFile.individual.pid,
                        roddyBamFile.sampleType.name,
                        seqTypeMap[roddyBamFile.seqType],
                ].join(outputFieldDelimiter)
            }
        }.join('\n')
    }

    void showSamples(List<String> input) {
        List<InputData> inputDataList = parseInputTable(input).each { InputData inputData ->
            searchAndAddRoddyBamFiles(inputData)
        }
        println ''
        println createContent(inputDataList)
    }
}

class HandleInputTypes {

    FileService fileService

    FileSystemService fileSystemService

    String inputFieldDelimiter

    String outputFieldDelimiter

    private List<String> readInput(String input) {
        return input.split('\n')*.trim().findAll {
            it && !it.startsWith('#')
        }
    }

    private int checkColumnCount(List<String> cleanInput) {
        if (!cleanInput) {
            return 0
        }
        return cleanInput[0].split(inputFieldDelimiter).size()
    }

    private void handleSampleListing(Collection<String> input) {
        DisplaySamples displaySamples = new DisplaySamples([
                inputFieldDelimiter : inputFieldDelimiter,
                outputFieldDelimiter: outputFieldDelimiter,
        ])
        displaySamples.showSamples(input)
    }

    private void handleExport(List<String> input, String fileName, boolean overwriteExisting) {
        BamExportImport export = new BamExportImport([
                fileService         : fileService,
                fileSystemService   : fileSystemService,
                inputFieldDelimiter : inputFieldDelimiter,
                outputFieldDelimiter: outputFieldDelimiter,
        ])
        Path file = export.handleInput(input, fileName, overwriteExisting)
        println "Metadata exported to ${file}\n"
    }

    void handleInput(String input, String fileName, boolean overwriteExisting) {
        List<String> cleanInput = readInput(input)
        int columnCount = checkColumnCount(cleanInput)
        switch (columnCount) {
            case 0:
                println 'no content, so nothing to do'
                break
            case 3:
                println '3 columns found, show sample table'
                handleSampleListing(cleanInput)
                break
            case 5:
                println '5 columns found, export metadata'
                handleExport(cleanInput, fileName, overwriteExisting)
                break
            default:
                println "Unsupported column count: ${columnCount}. Only 3 or 5 columns can be handled"
        }
    }
}

HandleInputTypes export = new HandleInputTypes([
        fileService         : ctx.fileService,
        fileSystemService   : ctx.fileSystemService,
        inputFieldDelimiter : inputFieldDelimiter,
        outputFieldDelimiter: outputFieldDelimiter,
]).handleInput(input, fileName, overwriteExisting)

println ''
