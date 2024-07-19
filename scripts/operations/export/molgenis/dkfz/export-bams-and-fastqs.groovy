/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TimeFormats

import java.nio.file.Path

//-------------
// input area

/**
 * Provide projects, seperated by newline.
 * empty lines and lines starting with '#' are ignored.
 */
String projectString = """
#project 1
#project 2

"""

//-------------
// work area

ProjectService projectService = ctx.projectService
FileService fileService = ctx.fileService

List<Project> projects = projectString.split("\n")*.trim().findAll {
    it && !it.startsWith("#")
}.collect {
    CollectionUtils.exactlyOneElement(Project.findAllByName(it), "No Project with name '${it}' exist")
}

assert projects: "No projects given"

@TupleConstructor
enum RawSequenceFileColumns {
    DATA_FILE_ID("DataFile ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.id
    }),
    SEQ_TRACK_ID("SeqTrack ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.id
    }),
    SAMPLE_ID("Sample ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.sample.id
    }),
    INDIVIDUAL_ID("Individual ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.individual.id
    }),
    PID("PID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.sample.individual.pid
    }),
    COMMON_NAME("Species Common Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.individual.species ? rawSequenceFile.individual.species.species.speciesCommonName.name : ""
    }),
    SCIENTIFIC_NAME("Species Scientific Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.individual.species ? rawSequenceFile.individual.species.species.scientificName : ""
    }),
    STRAIN_NAME("Species Strain Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.individual.species ? rawSequenceFile.individual.species.strain.name : ""
    }),
    MIXED_IN_COMMON_NAME("Mixed-in Species Common Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return (rawSequenceFile.sample.mixedInSpecies ?: [] as List<SpeciesWithStrain>)*.species*.speciesCommonName*.name.join(',')
    }),
    MIXED_IN_SCIENTIFIC_NAME("Mixed-in Species Scientific Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return (rawSequenceFile.sample.mixedInSpecies ?: [] as List<SpeciesWithStrain>)*.species*.scientificName.join(',')
    }),
    MIXED_IN_STRAIN_NAME("Mixed-in Species Strain Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return (rawSequenceFile.sample.mixedInSpecies ?: [] as List<SpeciesWithStrain>)*.strain*.name.join(',')
    }),
    SAMPLE_IDENTIFIER("Sample Identifier", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.sampleIdentifier
    }),
    SAMPLE_TYPE_ID("Sample Type ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.sampleType.id
    }),
    SAMPLE_TYPE("Sample Type", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.sample.sampleType.name
    }),
    FASTQ_PATH("FastQ Path", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return (properties["rawSequenceDataWorkFileService"] as RawSequenceDataWorkFileService).getFilePath(rawSequenceFile)?.toString()
    }),
    MD5SUM("md5sum", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.fastqMd5sum
    }),
    DATE_CREATED("Date Created", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.dateCreated
    }),
    N_READS("Number of Reads", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.nReads
    }),
    SEQ_LENGTH("Sequence Length", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.sequenceLength
    }),
    FASTQC_ID("FastQC ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))?.id
    }),
    FASTQC_PATH("FastQC Path", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))
        return fastqcProcessedFile ? (properties["fastqcDataFilesService"] as FastqcDataFilesService).fastqcOutputPath(fastqcProcessedFile).toString() : ""
    }),
    RUN_ID("Run ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.id
    }),
    RUN_NAME("Run Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.name
    }),
    RUN_DATE_EXECUTED("Run Date Executed", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.dateExecuted
    }),
    SEQ_CENTER_ID("Seq Center ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.seqCenter.id
    }),
    SEQ_CENTER("Seq Center", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.seqCenter.name
    }),
    SEQ_PLATFORM_ID("Seq Platform ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.seqPlatform.id
    }),
    SEQ_PLATFORM("Seq Platform", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.seqPlatform.name
    }),
    SEQ_PLATFORM_MODEL_LABEL("Seq Platform Model Label", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.seqPlatform.seqPlatformModelLabel.name
    }),
    SEQ_KIT_LABEL("Seq Kit Label", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.run.seqPlatform.sequencingKitLabel?.name
    }),
    SEQ_TYPE_ID("Sequencing Type Id", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.seqType.id
    }),
    SEQ_TYPE("Sequencing Type Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.seqType.name
    }),
    SEQ_TYPE_SINGLE_CELL("Sequencing Type is Single Cell", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.seqType.singleCell
    }),
    LIB_LAYOUT("Lib Layout", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.seqType.libraryLayout.name()
    }),
    LIB_NAME("Lib Name", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.libraryName
    }),
    LIB_PREP_KIT_ID("Lib Prep Kit ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.libraryPreparationKit?.id
    }),
    LIB_PREP_KIT("Lib Prep Kit", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.libraryPreparationKit?.name
    }),
    ANTIBODY_TARGET_ID("Antibody Target ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.antibodyTarget?.id
    }),
    ANTIBODY_TARGET("Antibody Target", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.antibodyTarget?.name
    }),
    ANTIBODY("Antibody", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.antibody
    }),
    INSERT_SIZE("Insert Size", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.insertSize
    }),
    LANE_ID("Lane ID", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.laneId
    }),
    INDIVIDUAL_COMMENT("Individual comment", { RawSequenceFile rawSequenceFile, Map properties = [:] ->
        return rawSequenceFile.seqTrack.individual.comment?.comment
    })

    String columnName
    Closure<String> value
}

@TupleConstructor
enum BamColumns {
    BAM_ID("Bam ID", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.id
    }),
    ALIGNMENT_DIR("Bam Path", { AbstractBamFile bamFile, Map properties = [:] ->
        return (properties["abstractBamFileService"] as AbstractBamFileService).getBaseDirectory(bamFile).resolve(bamFile.bamFileName)
    }),
    DATE_CREATED("dateCreated", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.dateCreated
    }),
    QC_STATUS("QC Status", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.qcTrafficLightStatus.name()
    }),
    WITHDRAWN("withdrawn", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.withdrawn
    }),
    COVERAGE("coverage", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.coverage
    }),
    PERCENT_MAPPED_READS("% mapped reads", { AbstractBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.percentMappedReads
    }),
    PERCENT_DUPLICATES("% duplicates", { AbstractBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.percentDuplicates
    }),
    PERCENT_PAIRED("% properly paired", { AbstractBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.percentProperlyPaired
    }),
    INSERT_SIZE_MEDIAN("insert size median", { AbstractBamFile bamFile, Map properties = [:] ->
        return MolgenisExporter.getRoddyMergedBamQaAll(bamFile)?.insertSizeMedian
    }),
    REF_GEN_ID("Reference Genome ID", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.referenceGenome?.id
    }),
    REF_GEN("Reference Genome", { AbstractBamFile bamFile, Map properties = [:] ->
        return bamFile.referenceGenome?.name
    }),
    CONTAINED_LANES("Contained Lanes (as SeqTrack IDs)", { AbstractBamFile bamFile, Map properties = [:] ->
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
        return header.collect {
            String value = map[it] as String
            String preparedValue = value == null ? '' : value.replaceAll("\"", "\"\"").replaceAll("[\n\r\t]", " ")
            "\"${preparedValue}\""
        }.join(COLUMN_SEPARATOR)
    }
}

class MolgenisRawSequenceFile extends MolgenisEntity {

    List<String> header = RawSequenceFileColumns.values()*.columnName + MetaDataKey.list()*.name.sort()

    static Map properties = [:]

    MolgenisRawSequenceFile(Map<String, String> map = [:]) {
        super(map)
    }

    static MolgenisRawSequenceFile export(RawSequenceFile rawSequenceFile) {
        return new MolgenisRawSequenceFile(RawSequenceFileColumns.values().collectEntries {
            [(it.columnName): it.value(rawSequenceFile, properties)]
        } + MetaDataEntry.findAllBySequenceFile(rawSequenceFile).collectEntries {
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

    static MolgenisBam export(AbstractBamFile bamFile) {
        return new MolgenisBam(BamColumns.values().collectEntries {
            [(it.columnName): it.value(bamFile, properties)]
        })
    }
}

class MolgenisExporter {

    ApplicationContext ctx

    String exportRawSequenceFiles(List<RawSequenceFile> rawSequenceFiles) {
        MolgenisRawSequenceFile.properties["rawSequenceDataWorkFileService"] = ctx.rawSequenceDataWorkFileService
        MolgenisRawSequenceFile.properties["fastqcDataFilesService"] = ctx.fastqcDataFilesService
        return ([new MolgenisRawSequenceFile().headerAsCsv] + rawSequenceFiles.collect { RawSequenceFile df -> MolgenisRawSequenceFile.export(df).toCsvLine() }).join("\n")
    }

    String exportBams(List<AbstractBamFile> bams) {
        MolgenisBam.properties["abstractBamFileService"] = ctx.abstractBamFileService
        return ([new MolgenisBam().headerAsCsv] + bams.collect { AbstractBamFile bam -> MolgenisBam.export(bam).toCsvLine() }).join("\n")
    }

    static RoddyMergedBamQa getRoddyMergedBamQaAll(AbstractBamFile bamFile) {
        List<RoddyMergedBamQa> qas = RoddyMergedBamQa.withCriteria {
            eq("abstractBamFile", bamFile)
            eq("chromosome", RoddyQualityAssessment.ALL)
        }
        return CollectionUtils.atMostOneElement(qas)
    }
}

String timestamp = TimeFormats.DATE_TIME_DASHES.getFormattedDate(new Date())

projects.each { Project project ->
    List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.withCriteria {
        seqTrack {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    } as List<RawSequenceFile>

    List<AbstractBamFile> bams = AbstractBamFile.withCriteria {
        workPackage {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    } as List<AbstractBamFile>

    Path projectPath = projectService.getProjectDirectory(project)
    Path outputDirectory = projectPath.resolve("exports").resolve("molgenis").resolve(timestamp)

    fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputDirectory, project.unixGroup)

    MolgenisExporter exporter = new MolgenisExporter(ctx: ctx)

    println "Writing to: ${outputDirectory}"
    [
            ["data-files", exporter.exportRawSequenceFiles(rawSequenceFiles)],
            ["bams", exporter.exportBams(bams)],
    ].each {
        Path path = outputDirectory.resolve("${it[0]}.csv")
        println "    - ${path}"
        fileService.createFileWithContent(path, it[1])
    }
}

''
