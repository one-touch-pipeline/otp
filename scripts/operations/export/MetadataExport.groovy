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
 * - sampleIds
 * - md5Sum of datafile (select complete otp lane, for paired also the other read)
 * - about a input table defining seqTracks. Each line needs to be find at least one seqTrack. Be careful with combining with filters.
 *   The table has following 5-6 columns:
 *   - pid
 *   - sample type
 *   - seqType name or alias (for example WGS, WES, RNA, ...)
 *   - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 *   - single cell flag: true = single cell, false = bulk
 *   - sampleName: optional column, if present, the seqTrack has to have this sample name
 *
 * Additional a filter can be done about:
 * - sampleType: Only seqTracks of this sampleType is shown.
 *   If used together with input table all sample types used there should be included, otherwise the seqTracks there will be lost.
 * - seqType (Specified about sampleTypeName, libraryLayout and single cell flag).
 *   If used together with input table all seqTypes used there should be included, otherwise the seqTracks there will be lost.
 *
 * Please provide one value per line. Spaces around the values are trimmed away. Empty lines and lines starting with # are ignored.
 *
 * The file is generated in the provided file with permission 660. Missing parent directories are created, if necessary.
 * A copy of the file with permission 440 is created using the file name and adding the suffix '.org'
 *
 * The flag 'overwriteExisting' indicate, if an existing file should be replaced.
 */

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.REALM_DEFAULT_VALUE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

//=============================================
// input area

/**
 * List of project, one per line
 */
String selectByProject = """
#project1
#project2

"""

/**
 * List of pids, one per line
 */
String selectByIndividual = """
#pid1
#pid2

"""

/**
 * List of ilse, one per line
 */
String selectByIlse = """
#ilse1
#ilse2

"""


/**
 * List of sample types, one per line
 */
String selectBySampleName = """
#sampleName1
#sampleName2

"""


/**
 * List of md5sums, one per line.
 * For paired data always the corresponding read are also fetched.
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
 * The columns can be separated by space, comma, semicolon or tab. Multiple separators are merged together.
 */
String filterBySeqTypeName = """
#EXON PAIRED false
#WGS SINGLE false
#10x_scRNA PAIRED true

"""


/**
 * Multi selector using:
 * - pid
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 *
 * The columns can be separated by space, comma, semicolon or tab. Multiple separators are merged together.
 */
String multiColumnInput = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
#pid5,control,RNA,SINGLE,true,sampleName2

"""


/**
 * Name of the file to generate. The name must be absolute.
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
        CollectionUtils.exactlyOneElement(selection(it), "Could not found ${inputType} '${it}'")
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

List<SeqTrack> seqTracksSampleIdentifier = selectBySampleName.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collectMany {
    List<SeqTrack> seqTracks = SeqTrack.findAllBySampleIdentifier(it)

    if (!seqTracks) {
        throw new AssertionError("Could not find any OTP lane with the sample name ${it}")
    }
    return seqTracks
}

List<SeqTrack> seqTracksPerMd5sum = selectByMd5Sum.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collectMany {
    List<DataFile> dataFiles = DataFile.findAllByMd5sum(it)

    if (!dataFiles) {
        throw new AssertionError("Could not find any datafile with the md5sum ${it}")
    }
    return dataFiles*.seqTrack
}

List<SampleType> sampleTypes = parseHelper(filterBySampleType, 'SampleTYpe') {
    SampleType.findAllByNameIlike(it)
}

List<SeqType> seqTypes = filterBySeqTypeName.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.collect {
    String[] values = it.split('[ ,;\t]+')
    int valueSize = values.size()
    assert valueSize == 3: "A seqtype is defined by three parts"
    SequencingReadType libraryLayout = SequencingReadType.findByName(values[1])
    assert libraryLayout: "${values[1]} is no valid sequencing read type"
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
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(values[0], values[0], values[0]),
            "Could not find one individual with name ${values[0]}")
    SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByNameIlike(values[1]),
            "Could not find one sampleType with name ${values[1]}")

    SequencingReadType libraryLayout = SequencingReadType.findByName(values[3])
    assert libraryLayout: "${values[3]} is no valid sequencingReadType"
    boolean singleCell = Boolean.parseBoolean(values[4])

    SeqType seqType = seqTypeService.findByNameOrImportAlias(values[2], [
            libraryLayout: libraryLayout,
            singleCell   : singleCell,
    ])
    assert seqType: "Could not find seqType with : ${values[2]} ${values[3]} ${values[4]}"

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
    println "no selection defined, stopped"
    return
}

if (seqTrackPerMultiImport && sampleTypes && !sampleTypes.containsAll(seqTrackPerMultiImport*.sampleType.unique())) {
    println "Attention: your sampleTypes filter do not contain all sample types used in your table input. " +
            "Therefore some of the seqTracks there will removed"
}

if (seqTrackPerMultiImport && seqTypes && !seqTypes.containsAll(seqTrackPerMultiImport*.seqType.unique())) {
    println "Attention: your seqTypes filter do not contain all seqTypes used in your table input. " +
            "Therefore some of the seqTracks there will removed"
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

if (dataFiles) {
    println "Found ${dataFiles.size()} lanes"
} else {
    throw new OtpRuntimeException("Could not find any Datafiles for the Criteria.")
}


class MetaDataExport {

    LsdfFilesService lsdfFilesService
    FileService fileService
    FileSystemService fileSystemService

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
        List<String> headers = (MetaDataColumn.values()*.name() + (allProperties*.keySet().sum() as List<String>).sort()).unique()
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

        put(FASTQ_FILE, lsdfFilesService.getFileFinalPath(dataFile).replaceAll('//+', '/'))
        put(MD5, dataFile.md5sum)
        put(READ, (dataFile.indexFile ? 'I' : '') + dataFile.mateNumber?.toString())
        put(WITHDRAWN, dataFile.fileWithdrawn ? '1' : null)
        put(WITHDRAWN_DATE, dataFile.withdrawnDate?.format("yyyy-MM-dd"))
        put(WITHDRAWN_COMMENT, dataFile.withdrawnComment?.trim()?.replace("\t", ", ")?.replace("\n", "; "))

        //export, if the fastq file is available or is a dead link. It use the cached flag in the database.
        put(FILE_EXISTS, dataFile.fileExists.toString())

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
        put(SEQUENCING_TYPE, seqType)
        put(SEQUENCING_READ_TYPE, seqTrack.seqType.libraryLayout.toString())
        properties.put('OTP_PID', seqTrack.individual.pid)
        properties.put('OTP_PID_ALIAS', seqTrack.individual.mockPid)
        properties.put('OTP_PID_DISPLAYED_IDENTIFIER', seqTrack.individual.mockFullName)
        properties.put('OTP_SAMPLE_TYPE', seqTrack.sampleType.name)
        put(SAMPLE_NAME, seqTrack.sampleIdentifier)
        put(FASTQ_GENERATOR, preferredOrLongest(
                properties.get(FASTQ_GENERATOR.toString()), SoftwareToolIdentifier.findAllBySoftwareTool(seqTrack.pipelineVersion)*.name))
        put(FRAGMENT_SIZE, String.valueOf(seqTrack.insertSize))
        put(LIB_PREP_KIT, seqTrack.libraryPreparationKit?.name)
        put(ILSE_NO, seqTrack.ilseSubmission?.ilseNumber?.toString())
        put(PROJECT, seqTrack.project.name)
        put(TAGMENTATION_LIBRARY, seqTrack.libraryName)

        if (seqTrack.seqType.hasAntibodyTarget) {
            put(ANTIBODY_TARGET, seqTrack.antibodyTarget.name)
            put(ANTIBODY, seqTrack.antibody)
        }

        put(SWAPPED, seqTrack.swapped.toString())

        return properties
    }

    static String preferredOrLongest(String preferred, Collection<String> all) {
        return all.contains(preferred) ? preferred : all.max { it.length() }
    }

    Path handleCreationOfMetadataFile(Collection<DataFile> dataFiles, String fileName, boolean overwriteExisting) {
        assert fileName: 'No file name given, but this is required'
        assert !fileName.contains(' '): 'File name contains spaces, which is not allowed'

        FileSystem fileSystem = fileSystemService.getRemoteFileSystemOnDefaultRealm()
        Path outputFile = fileSystem.getPath(fileName)

        assert outputFile.absolute: '"The file name is not absolute, but that is required'

        if (Files.exists(outputFile)) {
            if (overwriteExisting) {
                Files.delete(outputFile)
            } else {
                throw new OtpRuntimeException("The file ${outputFile} already exist and overwrite is set to false")
            }
        }

        Path outputFileOrg = fileSystem.getPath(fileName + '.org')

        if (!overwriteExisting) {
            assert !Files.exists(outputFile): "Outputfile ${outputFile} already exists"
            assert !Files.exists(outputFile): "Original outputfile ${outputFileOrg} already exists"
        }

        String realmName = fileSystemService.processingOptionService.findOptionAsString(REALM_DEFAULT_VALUE)
        Realm realm = Realm.findByName(realmName)

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputFile.parent, realm)

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
        lsdfFilesService : ctx.lsdfFilesService,
        fileService      : ctx.fileService,
        fileSystemService: ctx.fileSystemService,
])


Path file = metaDataExport.handleCreationOfMetadataFile(dataFiles, fileName, overwriteExisting)

println "Metadata exported to ${file}"
