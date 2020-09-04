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

/**
 * Script to export metadata for selected fastq files.
 *
 * The export contains all metadata imported with the fastq files. But the metadata OTP use in other
 * domains are taken from there to have the actual used information. For the fastq files the complete view-by-pid path is used.
 *
 * The selection can be done about following input areas:
 * - project name
 * - pid
 * - ilseNumber
 * - sampleIds (select full sample)
 *
 * Additional a filter can be done about:
 * - sampleType
 * - seqType (Specified about sampleTypeName, libraryLayout and single cell flag)
 *
 *
 * Please provide one value per line. Spaces around the values are trimmed away. Empty lines and lines starting with # are ignored.
 * *
 * The files are saved in a .tsv file with permission 660 and a .tsv.org file with permission 440.
 *
 * If you do not define a file name, the name is build from the input.
 *
 * The flag 'overwriteExisting' indicate, if an existing file should be replaced.
 */

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

//=============================================
// input area


String selectByProject = """
#project1
#project2

"""

String selectByIndividual = """
#pid1
#pid2

"""


String selectByIlse = """
#ilse1
#ilse2

"""


String selectBySampleIdentifier = """
#sampleIdentifier1
#sampleIdentifier2

"""


String filterBySampleType = """
#sampleType1
#sampleType2

"""

//A SeqType is defined as a combination of SeqTypeName (or alias), LibraryLayout (PAIRED, SINGLE, MATE_PAIRED) and singleCell flag (true = single cell, false = bulk).
//Example:
//EXON PAIRED false
//WGS SINGLE false
//10x_scRNA PAIRED true
String filterBySeqTypeName = """
#seqType1
#seqType2

"""

/**
 * Name of the file without extension. The extension '.tsv' is automatically added.
 * The file is created in $PATH_SCRIPTS_OUTPUT/export.
 */
String fileName = ''

/**
 * Flag to indicate, if existing files should be overwritten
 */
boolean overwriteExisting = false

//=============================================
// check input area

SeqTypeService seqTypeService = ctx.seqTypeService

static <T> List<T> parseHelper(String inputArea, String inputType, Closure<T> selection) {
    inputArea.split('\n')*.trim().findAll {
        it && !it.startsWith('#')
    }.collect {
        CollectionUtils.exactlyOneElement(selection(it), "Could not found ${inputType} '${it}")
    }
}

List<Project> projects = parseHelper(selectByProject, 'project') {
    Project.findAllByName(it)
}

List<Individual> individuals = parseHelper(selectByIndividual, 'Individual') {
    Individual.findAllByPidOrMockPidOrMockFullName(it, it, it)
}

List<IlseSubmission> ilseSubmissions = parseHelper(selectByIlse, 'IlseNumber') {
    IlseSubmission.findAllByIlseNumber(it as long)
}

List<SeqTrack> seqTracks = parseHelper(selectBySampleIdentifier, 'SampleIdentifier') {
    SeqTrack.findAllBySampleIdentifier(it)
}

List<SampleType> sampleTypes = parseHelper(filterBySampleType, 'SampleTYpe') {
    SampleType.findAllByNameIlike(it)
}

List<SeqType> seqTypes = filterBySeqTypeName.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collect {
    String[] values = it.split(' ')
    assert values.size() == 3: "A seqtype is defined by three parts"
    LibraryLayout libraryLayout = LibraryLayout.findByName(values[1])
    assert libraryLayout: "${values[1]} is no valid library layout"
    boolean singleCell = Boolean.parseBoolean(values[2])
    println singleCell

    SeqType seqType = seqTypeService.findByNameOrImportAlias(values[0], [
            libraryLayout: libraryLayout,
            singleCell   : singleCell,
    ])
    assert seqType: "Could not find seqType: ${it}"
    return seqType
}


if (!fileName) {
    fileName = [
            'export',
            projects*.name,
            individuals*.pid,
            ilseSubmissions*.ilseNumber,
            seqTracks*.sampleIdentifier.unique(),
            sampleTypes*.name,
            seqTypes*.toString(),
    ].findAll().flatten().join('_').replaceAll(' ', '_')
}

if (!projects && !individuals && !ilseSubmissions && !seqTracks) {
    println "no selection defined, stopped"
    return
}

//=============================================
// work area

Collection<DataFile> dataFiles = DataFile.createCriteria().list {
    seqTrack {
        or {
            if (projects) {
                sample {
                    individual {
                        'in'('project', projects)
                    }
                }
            }
            if (individuals) {
                sample {
                    'in'('individual', individuals)
                }
            }
            if (seqTracks) {
                'in'('id', seqTracks*.id)
            }
            if (ilseSubmissions) {
                'in'('ilseSubmission', ilseSubmissions)
            }
        }
        if (seqTypes) {
            'in'('seqType', seqTypes)
        }
        if (sampleTypes) {
            sample {
                'in'('sampleType', sampleTypes)
            }
        }
        sample {
            individual {
                project {
                    order('name')
                }
                order('pid')
            }
            sampleType {
                order('name')
            }
        }
        seqType {
            order('name')
        }
    }
    order('fileName')
}

assert dataFiles: "Could not find any Datafiles for the Criteria."


class MetaDataExport {

    LsdfFilesService lsdfFilesService
    FileService fileService

    /**
     * Creates a TSV file containing the metadata of the specified {@linkplain DataFile}s.
     * The output file has a format which is processable by the {@linkplain MetadataImportService}.
     */
    void writeMetadata(Collection<DataFile> dataFiles, Path metadataOutputFile) {
        metadataOutputFile.bytes = getMetadata(dataFiles).getBytes(StandardCharsets.UTF_8)
    }

    String getMetadata(Collection<DataFile> dataFiles) {
        MetaDataKey.list()
        Collection<Map<String, String>> allProperties = dataFiles.collect { getMetadata(it) }
        List<String> headers = allProperties*.keySet().sum() as List<String>
        StringBuilder s = new StringBuilder(headers.join('\t')).append('\n')
        for (Map<String, String> properties : allProperties) {
            s << headers.collect { String header ->
                properties.get(header) ?: ''
            }.join('\t')
            s << '\n'
        }
        return s.toString()
    }

    Map<String, String> getMetadata(DataFile dataFile) {
        Map<String, String> properties = [:]

        MetaDataEntry.findAllByDataFile(dataFile).each {
            properties.put(it.key.name, it.value)
        }

        Closure put = { MetaDataColumn column, String value ->
            if (value != null) {
                properties.put(column.toString(), value)
            }
        }

        put(FASTQ_FILE, lsdfFilesService.getFileViewByPidPath(dataFile).replaceAll('//+', '/'))
        put(MD5, dataFile.md5sum)
        put(READ, dataFile.mateNumber?.toString())
        put(WITHDRAWN, dataFile.fileWithdrawn ? '1' : null)
        put(WITHDRAWN_DATE, dataFile.withdrawnDate?.format("yyyy-MM-dd"))

        Run run = dataFile.run
        put(RUN_ID, run.name)
        put(RUN_DATE, run.dateExecuted?.format("yyyy-MM-dd"))
        put(CENTER_NAME, run.seqCenter.name)
        put(INSTRUMENT_PLATFORM, run.seqPlatform.name)
        put(INSTRUMENT_MODEL, run.seqPlatform.seqPlatformModelLabel?.name)
        put(SEQUENCING_KIT, run.seqPlatform.sequencingKitLabel?.name)

        SeqTrack seqTrack = dataFile.seqTrack
        String[] laneId = seqTrack.laneId.split('_', 2)
        put(LANE_NO, laneId[0])
        put(INDEX, laneId.length > 1 ? laneId[1] : null)
        String seqType = seqTrack.seqType.name
        if (seqType.endsWith(SeqType.TAGMENTATION_SUFFIX)) {
            put(SEQUENCING_TYPE, seqType.substring(0, seqType.length() - SeqType.TAGMENTATION_SUFFIX.length()))
            put(TAGMENTATION_BASED_LIBRARY, '1')
        } else {
            put(SEQUENCING_TYPE, seqType)
        }
        put(SEQUENCING_READ_TYPE, seqTrack.seqType.libraryLayout.toString())
        properties.put('OTP_PID', seqTrack.individual.pid)
        properties.put('OTP_PID_ALIAS', seqTrack.individual.mockPid)
        properties.put('OTP_PID_DISPLAYED_IDENTIFIER', seqTrack.individual.mockFullName)
        properties.put('OTP_SAMPLE_TYPE', seqTrack.sampleType.name)
        properties.put('OTP_WITHDRAWN_COMMENT', dataFile.withdrawnComment?.trim()?.replace("\t", ", ")?.replace("\n", "; "))
        put(SAMPLE_NAME, seqTrack.sampleIdentifier)
        put(FASTQ_GENERATOR, preferredOrLongest(
                properties.get(FASTQ_GENERATOR.toString()), SoftwareToolIdentifier.findAllBySoftwareTool(seqTrack.pipelineVersion)*.name))
        put(FRAGMENT_SIZE, String.valueOf(seqTrack.insertSize))
        put(LIB_PREP_KIT, seqTrack.libraryPreparationKit?.name)
        put(ILSE_NO, seqTrack.ilseSubmission?.ilseNumber?.toString())
        put(PROJECT, seqTrack.project.name)
        put(CUSTOMER_LIBRARY, seqTrack.libraryName)

        if (seqTrack.seqType.hasAntibodyTarget) {
            put(ANTIBODY_TARGET, seqTrack.antibodyTarget.name)
            put(ANTIBODY, seqTrack.antibody)
        }

        return properties
    }

    static String preferredOrLongest(String preferred, Collection<String> all) {
        return all.contains(preferred) ? preferred : all.max { it.length() }
    }

    Path handleCreationOfMetadataFile(Collection<DataFile> dataFiles, String fileName, boolean overwriteExisting) {
        Path outputDir = new File(ConfigService.getInstance().getScriptOutputPath(), "metadata-export").toPath()
        Path outputFile = outputDir.resolve("${fileName}.tsv")
        Path outputFileOrg = outputDir.resolve("${fileName}.tsv.org")

        if (!overwriteExisting) {
            assert !Files.exists(outputFile): "Outputfile ${outputFile} already exists"
            assert !Files.exists(outputFile): "Original outputfile ${outputFileOrg} already exists"
        }

        fileService.createDirectoryRecursively(outputDir)

        writeMetadata(dataFiles, outputFile)
        fileService.setPermission(outputFile, [
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
        ].toSet().asImmutable())

        Files.copy(outputFile, outputFileOrg, StandardCopyOption.REPLACE_EXISTING)
        fileService.setPermission(outputFileOrg, FileService.DEFAULT_FILE_PERMISSION)
        return outputFile
    }
}

MetaDataExport metaDataExport = new MetaDataExport([
        lsdfFilesService: ctx.lsdfFilesService,
        fileService     : ctx.fileService,
])


Path file = metaDataExport.handleCreationOfMetadataFile(dataFiles, fileName, overwriteExisting)

println "Metadata exported to ${file}"
