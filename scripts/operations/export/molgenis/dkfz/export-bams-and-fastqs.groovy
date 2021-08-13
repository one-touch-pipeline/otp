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


import groovy.transform.TupleConstructor
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat

String projectString ="""
#project 1
#project 2

"""

// ---

List<String> projectNames = projectString.split("\n").findAll {
    if (it.trim().isEmpty() || it.trim().startsWith("#")) {
        return null
    }
    return it.trim()
}

assert projectNames: "No projects given"
List<Project> projects = Project.findAllByNameInList(projectNames)
assert projects.size() == projectNames.size(): "Not all project names could be resolved to a project"


@TupleConstructor
enum DataFileColumns {
    DATA_FILE_ID("DataFile ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.id
    }),
    SEQ_TRACK_ID("SeqTrack ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.id
    }),
    SAMPLE_ID("Sample ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.sample.id
    }),
    INDIVIDUAL_ID("Individual ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.individual.id
    }),
    PID("PID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.sample.individual.pid
    }),
    COMMON_NAME("Species Common Name", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.sample.individual.project.speciesWithStrain.species.commonName.name
    }),
    SCIENTIFIC_NAME("Species Scientific Name", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.sample.individual.project.speciesWithStrain.species.scientificName
    }),
    STRAIN_NAME("Species Strain Name", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.sample.individual.project.speciesWithStrain.strain.name
    }),
    SAMPLE_IDENTIFIER("Sample Identifier", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.sampleIdentifier
    }),
    SAMPLE_TYPE_ID("Sample Type ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.sampleType.id
    }),
    SAMPLE_TYPE("Sample Type", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.sample.sampleType.name
    }),
    FASTQ_PATH("FastQ Path", { DataFile dataFile, Map properties = [:] ->
        return (properties["lsdfFilesService"] as LsdfFilesService).getFileFinalPath(dataFile)
    }),
    MD5SUM("md5sum", { DataFile dataFile, Map properties = [:] ->
        return dataFile.md5sum
    }),
    DATE_CREATED("Date Created", { DataFile dataFile, Map properties = [:] ->
        return dataFile.dateCreated
    }),
    N_READS("Number of Reads", { DataFile dataFile, Map properties = [:] ->
        return dataFile.nReads
    }),
    SEQ_LENGTH("Sequence Length", { DataFile dataFile, Map properties = [:] ->
        return dataFile.sequenceLength
    }),
    FASTQC_ID("FastQC ID", { DataFile dataFile, Map properties = [:] ->
        return FastqcProcessedFile.findByDataFile(dataFile)?.id
    }),
    FASTQC_PATH("FastQC Path", { DataFile dataFile, Map properties = [:] ->
        return (properties["fastqcDataFilesService"] as FastqcDataFilesService).fastqcOutputFile(dataFile)
    }),
    RUN_ID("Run ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.id
    }),
    RUN_NAME("Run Name", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.name
    }),
    RUN_DATE_EXECUTED("Run Date Executed", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.dateExecuted
    }),
    SEQ_CENTER_ID("Seq Center ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.seqCenter.id
    }),
    SEQ_CENTER("Seq Center", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.seqCenter.name
    }),
    SEQ_PLATFORM_ID("Seq Platform ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.seqPlatform.id
    }),
    SEQ_PLATFORM("Seq Platform", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.seqPlatform.name
    }),
    SEQ_PLATFORM_MODEL_LABEL("Seq Platform Model Label", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.seqPlatform.seqPlatformModelLabel.name
    }),
    SEQ_KIT_LABEL("Seq Kit Label", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.run.seqPlatform.sequencingKitLabel?.name
    }),
    SEQ_TYPE_ID("Sequencing Type Id", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.seqType.id
    }),
    SEQ_TYPE("Sequencing Type Name", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.seqType.name
    }),
    SEQ_TYPE_SINGLE_CELL("Sequencing Type is Single Cell", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.seqType.singleCell
    }),
    LIB_LAYOUT("Lib Layout", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.seqType.libraryLayout.name()
    }),
    LIB_NAME("Lib Name", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.libraryName
    }),
    LIB_PREP_KIT_ID("Lib Prep Kit ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.libraryPreparationKit?.id
    }),
    LIB_PREP_KIT("Lib Prep Kit", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.libraryPreparationKit?.name
    }),
    ANTIBODY_TARGET_ID("Antibody Target ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.antibodyTarget?.id
    }),
    ANTIBODY_TARGET("Antibody Target", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.antibodyTarget?.name
    }),
    ANTIBODY("Antibody", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.antibody
    }),
    INSERT_SIZE("Insert Size", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.insertSize
    }),
    LANE_ID("Lane ID", { DataFile dataFile, Map properties = [:] ->
        return dataFile.seqTrack.laneId
    })

    String columnName
    Closure<String> value
}

@TupleConstructor
enum BamColumns {
    BAM_ID("Bam ID", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.id
    }),
    ALIGNMENT_DIR("Bam Path", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return new File(bamFile.baseDirectory, bamFile.bamFileName)
    }),
    DATE_CREATED("dateCreated", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.dateCreated
    }),
    QC_STATUS("QC Status", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.qcTrafficLightStatus.name()
    }),
    WITHDRAWN("withdrawn", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.withdrawn
    }),
    COVERAGE("coverage", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.coverage
    }),
    PERCENT_MAPPED_READS("% mapped reads", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.percentMappedReads
    }),
    PERCENT_DUPLICATES("% duplicates", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.percentDuplicates
    }),
    PERCENT_PAIRED("% properly paired", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.percentProperlyPaired
    }),
    INSERT_SIZE_MEDIAN("insert size median", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.insertSizeMedian
    }),
    REF_GEN_ID("Reference Genome ID", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.referenceGenome?.id
    }),
    REF_GEN("Reference Genome", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return bamFile.referenceGenome?.name
    }),
    CONTAINED_LANES("Contained Lanes (as SeqTrack IDs)", { AbstractMergedBamFile bamFile, Map properties = [:] ->
        return "${bamFile.containedSeqTracks*.id.join(";")}"
    })

    String columnName
    Closure<String> value
}


abstract class MolgenisEntity {

    final static String COLUMN_SEPARATOR = ","

    protected Map<String, String> map

    MolgenisEntity(Map<String, String> map) {
        this.map = map.withDefault { null }
    }

    abstract List<String> getHeader()

    String getHeaderAsCsv() {
        return header.collect { "\"${(it as String)?.replaceAll("\"", "\"\"")}\"" }.join(COLUMN_SEPARATOR)
    }

    String toCsvLine() {
        return header.collect { "\"${(map[it] as String)?.replaceAll("\"", "\"\"")}\"" }.join(COLUMN_SEPARATOR)
    }
}

class MolgenisDataFile extends MolgenisEntity {

    List<String> header = DataFileColumns.values()*.columnName + MetaDataKey.list()*.name.sort()

    static Map properties = [:]

    MolgenisDataFile(Map<String, String> map = [:]) {
        super(map)
    }

    static MolgenisDataFile export(DataFile dataFile) {
        return new MolgenisDataFile(DataFileColumns.values().collectEntries {
            [(it.columnName): it.value(dataFile, properties)]
        } + MetaDataEntry.findAllByDataFile(dataFile).collectEntries {
            [(it.key.name): it.value]
        })
    }
}

class MolgenisBam extends MolgenisEntity {

    List<String> header = BamColumns.values()*.columnName

    static Map properties = [:]

    MolgenisBam(Map<String, String> map = [:]) {
        super(map)
    }

    static MolgenisBam export(AbstractMergedBamFile bamFile) {
        return new MolgenisBam(BamColumns.values().collectEntries {
            [(it.columnName): it.value(bamFile, properties)]
        })
    }
}

class MolgenisExporter {

    ApplicationContext ctx

    String exportDataFiles(List<DataFile> dataFiles) {
        MolgenisDataFile.properties["lsdfFilesService"] = ctx.lsdfFilesService
        MolgenisDataFile.properties["fastqcDataFilesService"] = ctx.fastqcDataFilesService
        return ([new MolgenisDataFile().headerAsCsv] + dataFiles.collect { DataFile df -> MolgenisDataFile.export(df).toCsvLine() }).join("\n")
    }

    String exportBams(List<AbstractMergedBamFile> bams) {
        return ([new MolgenisBam().headerAsCsv] + bams.collect { AbstractMergedBamFile bam -> MolgenisBam.export(bam).toCsvLine() }).join("\n")
    }

    static RoddyMergedBamQa getRoddyMergedBamQaAll(AbstractMergedBamFile bamFile) {
        List<RoddyMergedBamQa> qas = RoddyMergedBamQa.withCriteria {
            qualityAssessmentMergedPass {
                eq("abstractMergedBamFile", bamFile)
            }
            eq("chromosome", RoddyQualityAssessment.ALL)
        }
        return CollectionUtils.atMostOneElement(qas)
    }
}

String timestamp = TimeFormats.DATE_TIME_DASHES.getFormattedDate(new Date())
Realm realm = ctx.configService.defaultRealm

projects.each { Project project ->
    List<DataFile> dataFiles = DataFile.withCriteria {
        seqTrack {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    }  as List<DataFile>

    List<AbstractMergedBamFile> bams = AbstractMergedBamFile.withCriteria {
        workPackage {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    } as List<AbstractMergedBamFile>

    final Path outputDirectory = ctx.fileService.toPath(ctx.configService.getScriptOutputPath(), ctx.fileSystemService.getRemoteFileSystemOnDefaultRealm()).resolve("export").resolve("molgenis").resolve("${timestamp}-${project.name}")
    ctx.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputDirectory, realm)
    ctx.fileService.setPermission(outputDirectory, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

    MolgenisExporter exporter = new MolgenisExporter(ctx: ctx)

    println "Writing to: ${outputDirectory}"
    [
            ["data-files", exporter.exportDataFiles(dataFiles)],
            ["bams", exporter.exportBams(bams)],
    ].each {
        Path path = outputDirectory.resolve("${it[0]}.csv")
        println "    - ${path}"
        Files.write(path, it[1].getBytes())
    }
}

''
