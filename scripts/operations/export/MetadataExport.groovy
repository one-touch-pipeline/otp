/*
 * Copyright 2011-2026 The OTP authors
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
 * Script to export metadata for selected fastq files
 *
 * The export contains all metadata imported with the fastq files. But the updated metadata that OTP uses in other
 * domains is taken from the database to have the correct information. For the fastq files the complete view-by-pid path is used.
 *
 * The selection for the export can be done with the following inputs:
 * - project name
 * - pid
 * - ilseNumber
 * - sampleIds
 * - md5Sum of the datafile (select complete OTP lane, for paired also the other read)
 * - or with an input table defining the seqTracks. Each line needs to be able to find at least one seqTrack. Be careful when combining this with the other filters.
 *   The table should have the following 5-6 columns:
 *   - pid
 *   - sample type
 *   - seqType name or alias (for example WGS, WES, RNA, ...)
 *   - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 *   - single cell flag: true = single cell, false = bulk
 *   - sampleName: optional column, if present, the seqTrack has to have this sample name
 *
 * Additionally, a filter can be created for:
 * - sampleType: Only seqTracks of this sampleType are shown.
 *   If used together with the input table all sample types used in the table should be included, otherwise the other seqTracks will not be in the exported file.
 * - seqType (Specified about sampleTypeName, libraryLayout and single cell flag).
 *   If used together with the input table all seqTypes used there should be included, otherwise the other seqTracks will not be in the exported file.
 *
 * Please provide one value per line. Spaces around the values will be trimmed away. Empty lines and lines starting with # will be ignored.
 *
 * The file will be generated at the provided filepath with the permissions 660. Missing parent directories will be created, if necessary.
 * A copy of the file with the permissions 440 will be created using the file name and adding the suffix '.org'
 *
 * The flag 'overwriteExisting' indicates, if an existing file should be replaced.
 *
 * The flag 'exportColumns' indicates, which columns should be exported. This flag can be set to ALL, WHITE_LISTED_COLUMNS, or REIMPORT_COLUMNS.
 * The option WHITE_LISTED_COLUMNS exports only the columns listed in the processing option METADATA_WHITELIST_COLUMNS.
 * The option REIMPORT_COLUMNS exports only the columns necessary for the reimport.
 * Otherwise, all data will be exported.
 */

import de.dkfz.tbi.otp.egaSubmission.EgaSubmissionService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TimeFormats

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

// =============================================
// input area

/**
 * List of projects, one per line
 */
String selectByProject = """
#project1
#project2

"""

/**
 * List of PIDs, one per line
 */
String selectByIndividual = """
#pid1
#pid2

"""

/**
 * List of ilseIDs, one per line
 */
String selectByIlse = """
#ilse1
#ilse2

"""

/**
 * List of sample names, one per line
 */
String selectBySampleName = """
#sampleName1
#sampleName2

"""

/**
 * List of md5sums, one per line.
 * For paired data the corresponding reads will also be fetched.
 */
String selectByMd5Sum = """
#Md5Sum1
#Md5Sum2

"""

/**
 * List of sample types, one per line
 */
String filterBySampleType = """
#sampleType1
#sampleType2

"""

/**
 * A SeqType is defined as a combination of:
 * - SeqTypeName (or alias)
 * - LibraryLayout (PAIRED, SINGLE, MATE_PAIRED)
 * - singleCell flag (true = single cell, false = bulk).
 *
 * The columns can be separated by space, comma, semicolon or tab. Multiple separators will be merged together.
 */
String filterBySeqTypeName = """
#EXON PAIRED false
#WGS SINGLE false
#10x_scRNA PAIRED true

"""

/**
 * Multi selector using:
 * - PID
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...)
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 *
 * The columns can be separated by space, comma, semicolon or tab. Multiple separators will be merged together.
 */
String multiColumnInput = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
#pid5,control,RNA,SINGLE,true,sampleName2

"""

/**
 * Name of the file to generate. The name must be an absolute path.
 */
String fileName = ''

/**
 * Flag to indicate, if existing files should be overwritten
 */
boolean overwriteExisting = false

/**
 * Flag to indicate which columns to export. The white-listed columns are provided in the processing options [option name: METADATA_WHITELIST_COLUMNS]
 */
ExportColumnsEnum exportColumns = ExportColumnsEnum.ALL // export all columns
// ExportColumnsEnum exportColumns = ExportColumnsEnum.WHITE_LISTED_COLUMNS // export only the white listed columns
// ExportColumnsEnum exportColumns = ExportColumnsEnum.REIMPORT_COLUMNS // export only the columns necessary for the reimport

enum ExportColumnsEnum {
    ALL, WHITE_LISTED_COLUMNS, REIMPORT_COLUMNS
}

/**
 * If you chose to export the metadata for a reimport, please also add the name of the new project it should be imported to.
 */
String newProjectSelector = ""

/**
 * Flag to indicate whether withdrawn data should be exported or not
 */
boolean exportWithdrawn = false

// =============================================
// check input area

SeqTypeService seqTypeService = ctx.seqTypeService
EgaSubmissionService egaSubmissionService = ctx.egaSubmissionService

static <T> List<T> parseHelper(String inputArea, String inputType, Closure<T> selection) {
    inputArea.split('\n')*.trim().findAll {
        it && !it.startsWith('#')
    }.collect {
        CollectionUtils.exactlyOneElement(selection(it), "Could not find ${inputType} '${it}'")
    }
}

List<Project> projects = parseHelper(selectByProject, 'project') {
    Project.findAllByName(it)
}

Project reimportProject = Project.findByName(newProjectSelector.trim())
if (exportColumns == ExportColumnsEnum.REIMPORT_COLUMNS && !reimportProject) {
    throw new AssertionError("Could not find any projects with the name ${newProjectSelector}, this is required for the reimport.")
}

List<Individual> individuals = parseHelper(selectByIndividual, 'individual') {
    Individual.findAllByPid(it)
}

List<IlseSubmission> ilseSubmissions = parseHelper(selectByIlse, 'ilseNumber') {
    IlseSubmission.findAllByIlseNumber(it as long)
}

List<SeqTrack> seqTracksSampleIdentifier = selectBySampleName.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collectMany {
    List<SeqTrack> seqTracks = SeqTrack.findAllBySampleIdentifier(it)

    if (!seqTracks) {
        throw new AssertionError("Could not find any OTP lanes with the sample name ${it}")
    }
    return seqTracks
}

List<SeqTrack> seqTracksPerMd5sum = selectByMd5Sum.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collectMany {
    List<RawSequenceFile> rawSequenceFiles = exportWithdrawn ? RawSequenceFile.findAllByFastqMd5sum(it) : RawSequenceFile.findAllByFastqMd5sumAndFileWithdrawn(it, exportWithdrawn)

    if (!rawSequenceFiles) {
        throw new AssertionError("Could not find any datafiles with the md5sum ${it}")
    }
    return rawSequenceFiles*.seqTrack
}

List<SampleType> sampleTypes = parseHelper(filterBySampleType, 'sampleType') {
    SampleType.findAllByName(it)
}

List<SeqType> seqTypes = filterBySeqTypeName.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collect {
    String[] values = it.split('[ ,;\t]+')
    int valueSize = values.size()
    assert valueSize == 3: "A seqType is defined by three parts"
    SequencingReadType libraryLayout = SequencingReadType.getByName(values[1])
    assert libraryLayout: "${values[1]} is not a valid sequencing read type"
    boolean singleCell = Boolean.parseBoolean(values[2])

    SeqType seqType = seqTypeService.findByNameOrImportAlias(values[0], [
            libraryLayout: libraryLayout,
            singleCell   : singleCell,
    ])
    assert seqType: "Could not find seqType: ${it}"
    return seqType
}

List<SeqTrack> seqTrackPerMultiImport = multiColumnInput.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collectMany { String line ->
    List<String> values = line.split('[ ,;\t]+')*.trim()
    int valueSize = values.size()
    assert valueSize in [5, 6]: "A multi input is defined by 5 or 6 columns"
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(values[0]),
            "Could not find any individuals with the name ${values[0]}")
    SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(values[1]),
            "Could not find any sampleTypes with the name ${values[1]}")

    SequencingReadType libraryLayout = SequencingReadType.getByName(values[3])
    assert libraryLayout: "${values[3]} is not a valid sequencingReadType"
    boolean singleCell = Boolean.parseBoolean(values[4])

    SeqType seqType = seqTypeService.findByNameOrImportAlias(values[2], [
            libraryLayout: libraryLayout,
            singleCell   : singleCell,
    ])
    assert seqType: "Could not find seqType with: ${values[2]} ${values[3]} ${values[4]}"

    List<SeqTrack> seqTracks = SeqTrack.withCriteria {
        sample {
            eq('individual', individual)
            eq('sampleType', sampleType)
        }
        eq('seqType', seqType)
        if (values.size() == 6) {
            eq('sampleIdentifier', values[5])
        }
    }
    assert seqTracks: "Could not find any seqtracks for ${values.join(' ')}"
    return seqTracks
}

if (!projects && !individuals && !ilseSubmissions && !seqTracksSampleIdentifier && !seqTracksPerMd5sum && !seqTrackPerMultiImport) {
    println "No selection defined, export stopped"
    return
}

if (seqTrackPerMultiImport && sampleTypes && !sampleTypes.containsAll(seqTrackPerMultiImport*.sampleType.unique())) {
    println "Attention: Your sampleTypes filter does not contain all sample types used in your table input. " +
            "Therefore some of the seqTracks will not be exported."
}

if (seqTrackPerMultiImport && seqTypes && !seqTypes.containsAll(seqTrackPerMultiImport*.seqType.unique())) {
    println "Attention: Your seqTypes filter does not contain all seqTypes used in your table input. " +
            "Therefore some of the seqTracks will not be exported."
}

List<Project> projectsEgaSubmissionInProgress = egaSubmissionService.findProjectsWithUploadInProgress(projects)

if (projectsEgaSubmissionInProgress) {
    println "Attention: Some data of the following projects could change, since the EGA submission is still in the upload process:"
    println projectsEgaSubmissionInProgress.name.join('\n')
    println "\n"
}


// =============================================
// work area

Collection<RawSequenceFile> rawSequenceFiles = RawSequenceFile.createCriteria().list {
    if (!exportWithdrawn) {
        eq('fileWithdrawn', false)
    }
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
            if (seqTracksSampleIdentifier) {
                'in'('id', seqTracksSampleIdentifier*.id)
            }
            if (seqTracksPerMd5sum) {
                'in'('id', seqTracksPerMd5sum*.id)
            }
            if (ilseSubmissions) {
                'in'('ilseSubmission', ilseSubmissions)
            }
            if (seqTrackPerMultiImport) {
                'in'('id', seqTrackPerMultiImport*.id)
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

if (rawSequenceFiles) {
    println "Found ${rawSequenceFiles.size()} lanes"
} else {
    throw new OtpRuntimeException("Could not find any datafiles for the criteria.")
}

class MetaDataExport {

    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    FileService fileService
    FileSystemService fileSystemService
    ProcessingOptionService processingOptionService

    /**
     * Creates a TSV file containing the metadata of the specified {@linkplain RawSequenceFile}s.
     * The output file has a format which is processable by the {@linkplain MetadataImportService}.
     */
    void writeMetadata(Collection<RawSequenceFile> rawSequenceFiles, Path metadataOutputFile, ExportColumnsEnum exportColumns, Project reimportProject) {
        metadataOutputFile.bytes = getMetadata(rawSequenceFiles, exportColumns, reimportProject).getBytes(StandardCharsets.UTF_8)
    }

    String getMetadata(Collection<RawSequenceFile> rawSequenceFiles, ExportColumnsEnum exportColumns, Project reimportProject) {
        MetaDataKey.list()
        boolean reimport = exportColumns == ExportColumnsEnum.REIMPORT_COLUMNS
        Collection<Map<String, String>> allProperties = rawSequenceFiles.collect { getMetadata(it, reimport, reimportProject, exportColumns) }

        List<String> allColumnHeaders = MetaDataColumn.values()*.name() + allProperties*.keySet().flatten().sort().unique()
        List<String> headers = determineHeaders(exportColumns, allColumnHeaders).unique()

        StringBuilder metadataString = new StringBuilder(headers.join('\t')).append('\n')
        allProperties.each { properties ->
            metadataString << headers.collect { header -> properties[header] ?: '' }.join('\t')
            metadataString << '\n'
        }
        return metadataString.toString()
    }

    private List<String> determineHeaders(ExportColumnsEnum exportColumns, List<String> allColumnHeaders) {
        if (exportColumns == ExportColumnsEnum.WHITE_LISTED_COLUMNS) {
            String whitelistColumns = processingOptionService.findOptionAsString(ProcessingOption.OptionName.METADATA_WHITELIST_COLUMNS)
            return whitelistColumns.split('[,;\t]+')*.trim().findAll { column -> allColumnHeaders.contains(column) }
        } else if (exportColumns == ExportColumnsEnum.REIMPORT_COLUMNS) {
            // this could also be made into a ProcessingOption if wanted
            List<String> removedColumns = ["SWAPPED", "WITHDRAWN", "WITHDRAWN_DATE", "WITHDRAWN_COMMENT", "FILE_EXISTS", "CUSTOMER_TAGS"]
            List<String> headers = allColumnHeaders - removedColumns
            // discussion with operators: column BASE_MATERIAL should always exist, even if empty (otp-2322)
            if (!headers.contains("BASE_MATERIAL")) headers << "BASE_MATERIAL"
            return headers.unique()
        }
        return allColumnHeaders.unique()
    }

    Map<String, String> getMetadata(RawSequenceFile rawSequenceFile, Boolean reimport, Project reimportProject, ExportColumnsEnum exportColumns) {
        Map<String, String> metadataValues = [:]
        MetaDataEntry.findAllBySequenceFile(rawSequenceFile).each {
            String value = (it.value == "N.A.") ? "" : it.value
            metadataValues.put(it.key.name, value)
        }

        Closure put = { MetaDataColumn column, String value ->
            if (value != null) {
                metadataValues.put(column.toString(), value)
            }
        }

        Path fastqPath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        String fastqValue = fastqPath?.toString()?.replaceAll('//+', '/')
        if (exportColumns == ExportColumnsEnum.WHITE_LISTED_COLUMNS && fastqValue) {
            fastqValue = Paths.get(fastqValue).fileName.toString()
        }
        put(FASTQ_FILE, fastqValue)
        put(MD5, rawSequenceFile.fastqMd5sum)
        put(READ, (rawSequenceFile.indexFile ? 'I' : '') + rawSequenceFile.mateNumber?.toString())

        SeqTrack seqTrack = rawSequenceFile.seqTrack
        put(SAMPLE_TYPE, seqTrack.sampleType.name)

        List<SpeciesWithStrain> speciesList = []
        if (rawSequenceFile.individual.species) {
            speciesList.add(rawSequenceFile.individual.species)
            speciesList.addAll(rawSequenceFile.sample.mixedInSpecies?.unique() ?: [])
        }

        if (!reimport) {
            put(WITHDRAWN, rawSequenceFile.fileWithdrawn ? '1' : null)
            put(WITHDRAWN_DATE, TimeFormats.DATE.getFormattedDate(rawSequenceFile.withdrawnDate))
            put(WITHDRAWN_COMMENT, rawSequenceFile.withdrawnComment?.trim()?.replace("\t", ", ")?.replace("\n", "; "))

            // export, if the fastq file is available or is a dead link. It use the cached flag in the database.
            put(FILE_EXISTS, rawSequenceFile.fileExists.toString())
            put(SWAPPED, seqTrack.swapped.toString())

            put(PATIENT_ID, seqTrack.individual.pid)
            put(PROJECT, seqTrack.project.name)
            put(SPECIES, speciesList ? speciesList*.importAlias*.first().join(' + ') : '')
        } else {
            String oldPid = seqTrack.individual.pid
            String newPrefixPid = oldPid.startsWith(seqTrack.project.individualPrefix) ?
                    oldPid.replaceFirst(seqTrack.project.individualPrefix, reimportProject.individualPrefix) : (reimportProject.individualPrefix + oldPid)
            put(PATIENT_ID, newPrefixPid)
            put(SAMPLE_TYPE, seqTrack.sampleType.name)
            put(PROJECT, reimportProject.name)
            put(SPECIES, speciesList ? speciesList*.displayName.join(' + ') : '')
        }

        Run run = rawSequenceFile.run
        put(RUN_ID, run.name)
        put(RUN_DATE, TimeFormats.DATE.getFormattedDate(run.dateExecuted))
        put(CENTER_NAME, run.seqCenter.name)
        put(INSTRUMENT_PLATFORM, run.seqPlatform.name)
        put(INSTRUMENT_MODEL, run.seqPlatform.seqPlatformModelLabel?.name)
        put(SEQUENCING_KIT, run.seqPlatform.sequencingKitLabel?.name)

        String[] laneId = seqTrack.laneId.split('_', 2)
        put(LANE_NO, laneId[0])
        put(INDEX, laneId.length > 1 ? laneId[1] : null)
        put(SEQUENCING_TYPE, seqTrack.seqType.name)
        put(SEQUENCING_READ_TYPE, seqTrack.seqType.libraryLayout.toString())

        put(SAMPLE_NAME, seqTrack.sampleIdentifier)
        put(FASTQ_GENERATOR, preferredOrLongest(
                metadataValues.get(FASTQ_GENERATOR.toString()),
                SoftwareToolIdentifier.findAllBySoftwareTool(seqTrack.pipelineVersion)*.name
        ))
        put(FRAGMENT_SIZE, String.valueOf(seqTrack.insertSize))
        put(LIB_PREP_KIT, seqTrack.libraryPreparationKit?.name)
        put(ILSE_NO, seqTrack.ilseSubmission?.ilseNumber?.toString())
        put(TAGMENTATION_LIBRARY, seqTrack.libraryName)

        if (seqTrack.seqType.hasAntibodyTarget) {
            put(ANTIBODY_TARGET, seqTrack.antibodyTarget.name)
            put(ANTIBODY, seqTrack.antibody)
        }

        return metadataValues
    }

    static String preferredOrLongest(String preferred, Collection<String> all) {
        return all.contains(preferred) ? preferred : all.max { it.length() }
    }

    Path handleCreationOfMetadataFile(
            Collection<RawSequenceFile> rawSequenceFiles,
            String fileName, boolean overwriteExisting,
            ExportColumnsEnum exportColumns,
            Project reimportProject
    ) {
        assert fileName: 'Error: No file name given'
        assert !fileName.contains(' '): 'Error: File name contains spaces'

        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path outputFile = fileSystem.getPath(fileName)

        assert outputFile.absolute: 'Error: The file path is not absolute'

        if (Files.exists(outputFile)) {
            if (overwriteExisting) {
                Files.delete(outputFile)
            } else {
                throw new OtpRuntimeException("The file ${outputFile} already exists and overwrite is set to false")
            }
        }

        Path outputFileOrg = fileSystem.getPath(fileName + '.org')

        if (!overwriteExisting) {
            assert !Files.exists(outputFile): "Outputfile ${outputFile} already exists"
            assert !Files.exists(outputFile): "Original outputfile ${outputFileOrg} already exists"
        }

        String unixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputFile.parent, unixGroup)

        writeMetadata(rawSequenceFiles, outputFile, exportColumns, reimportProject)
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
        rawSequenceDataWorkFileService: ctx.rawSequenceDataWorkFileService,
        fileService                   : ctx.fileService,
        fileSystemService             : ctx.fileSystemService,
        processingOptionService       : ctx.processingOptionService,
])

Path file = metaDataExport.handleCreationOfMetadataFile(rawSequenceFiles, fileName, overwriteExisting, exportColumns, reimportProject)

println "Metadata exported to ${file}"
