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

/*
Creates a table over all processed data for each SeqTrack, Pipeline and Project.

It lists the amount of processed objects per tool version and colors the entries based on their timeliness.
It also displays the total available amount of processable objects.

The overview lists alignment and analysis pipelines in one table. The numbers reflect MergingWorkPackages and SamplePairs respectively.
*/

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.SeqType

import java.util.regex.Matcher

// Input Area

// Enter a list of project names, empty list means all projects
List<String> selectedProjects = [

]

// Pipelines to ignore in the overview
List<Pipeline.Name> ignoredPipelines = [
        Pipeline.Name.DEFAULT_OTP,
        Pipeline.Name.OTP_SNV,
        Pipeline.Name.EXTERNALLY_PROCESSED,
]

class Globals {
    static String UNVERSIONED_PREFIX = "Unversioned"

    // Mapping of versions
    static Map<VersionState, List<String>> versionMapping = [

            (VersionState.UP_TO_DATE): [
                    // PanCan
                    "AlignmentAndQCWorkflows:1.2.73-1",
                    "AlignmentAndQCWorkflows:1.2.73-2",

                    // RNA
                    "RNAseqWorkflow:1.3.0",

                    // CellRanger
                    "cellranger/3.0.1",
                    "cellranger/3.1.0",
                    "cellranger/4.0.0",

                    // ACEseq
                    "ACEseqWorkflow:1.2.8-1",
                    "ACEseqWorkflow:1.2.8-2",
                    "ACEseqWorkflow:1.2.8-3",
                    "ACEseqWorkflow:1.2.8-4",

                    // Sophia
                    "SophiaWorkflow:1.2.17",
                    "SophiaWorkflow:2.2.0",
                    "SophiaWorkflow:2.2.1",

                    // SNV
                    "SNVCallingWorkflow:1.2.166-1",

                    // Indel
                    "IndelCallingWorkflow:1.2.177",
                    "IndelCallingWorkflow:1.0.177-2",
                    "IndelCallingWorkflow:1.2.177-4",
                    "IndelCallingWorkflow:2.0.0",

                    // Yapsa
                    "yapsa-devel/b765fa8",

                    // QC Workflow
                    "QualityControlWorkflows:1.2.182",
            ],
            (VersionState.OUTDATED): [
                    // PanCan
                    "AlignmentAndQCWorkflows:1.1.51",
                    "AlignmentAndQCWorkflows:1.2.51-1",
                    "AlignmentAndQCWorkflows:1.1.73",

                    // RNA
                    "RNAseqWorkflow:1.0.8",
                    "RNAseqWorkflow:1.0.10",
                    "RNAseqWorkflow:1.0.11",
                    "RNAseqWorkflow:1.0.12",
                    "RNAseqWorkflow:1.0.22",
                    "RNAseqWorkflow:1.0.22-1",
                    "RNAseqWorkflow:1.2.22-6",

                    // ACEseq

                    // Sophia
                    "SophiaWorkflow:1.0.15",
                    "SophiaWorkflow:1.0.16",
                    "SophiaWorkflow:1.2.16",
                    "SophiaWorkflow:2.0.1",
                    "SophiaWorkflow:2.0.2",

                    // SNV
                    "SNVCallingWorkflow:1.0.166-1",

                    // Indel
                    "IndelCallingWorkflow:1.0.167",
                    "IndelCallingWorkflow:1.0.176-1",
                    "IndelCallingWorkflow:1.0.176-7",
                    "IndelCallingWorkflow:1.0.176-8",
                    "IndelCallingWorkflow:1.0.176-9",
                    "IndelCallingWorkflow:1.0.176-10",

                    // Yapsa
                    "yapsa-devel/80f748e",

                    // QC Workflow
                    "QualityControlWorkflows:1.0.177",
                    "QualityControlWorkflows:1.0.178",
                    "QualityControlWorkflows:1.0.182",
                    "QualityControlWorkflows:1.0.182-1",
            ],
            (VersionState.DEPRECATED): [
                    // OTP SNV
                    "1.0.166",
            ],
            (VersionState.UNIMPORTANT): [
                    "Unversioned ProcessedMergedBamFile",
                    "Unversioned ExternallyProcessedMergedBamFile",
            ],
            (VersionState.UNKNOWN): [
            ]
    ]
}

// *** *** *** *** *** *** *** *** ***

List<AbstractMergedBamFile> getAbstractMergedBamFilesOfProject(Project project) {
    return AbstractMergedBamFile.createCriteria().list {
        workPackage {
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
        eq("withdrawn", false)
        eq("fileOperationStatus", AbstractMergedBamFile.FileOperationStatus.PROCESSED)
    }
}

List<BamFilePairAnalysis> getBamFilePairAnalysesOfProject(Project project) {
    return BamFilePairAnalysis.createCriteria().list {
        samplePair {
            mergingWorkPackage1 {
                sample {
                    individual {
                        eq("project", project)
                    }
                }
            }
        }
        eq("withdrawn", false)
        eq("processingState", AnalysisProcessingStates.FINISHED)
    }
}


// Get all pipelines (minus the ignored ones) in a sorted order (ALIGNMENT + rest alphabetically)
List<Pipeline> usedPipelines = ignoredPipelines ? Pipeline.findAllByNameNotInList(ignoredPipelines) : Pipeline.list()
Map<Pipeline.Type, List<Pipeline>> pipelinesPerType = usedPipelines.groupBy { it.type }
List<Pipeline.Type> typesInOrder = ([Pipeline.Type.ALIGNMENT] + Pipeline.Type.values()).flatten().unique()
List<Pipeline> selectedAndSortedPipelines = typesInOrder.collect { pipelinesPerType[(it)].sort { it.name } }.flatten()


Map<Project, Map<Pipeline, Map<SeqType, Map<String, List>>>> table = [:]

List<Project> projects
if (selectedProjects) {
    projects = Project.findAllByNameInList(selectedProjects)
} else {
    projects = Project.list().sort { it.name.toLowerCase() }
}

// Fill 'table' map, containing processed objects per project, pipeline, seqType and version
projects.each { Project project ->
    getAbstractMergedBamFilesOfProject(project).groupBy { it.mergingWorkPackage.pipeline }.each { Pipeline pipeline, List<AbstractMergedBamFile> ambfsOfPipeline ->
        if (pipeline in selectedAndSortedPipelines) {
            ambfsOfPipeline.groupBy { it.mergingWorkPackage.seqType }.each { SeqType seqType, List<AbstractMergedBamFile> ambfsOfSeqType ->
                ambfsOfSeqType.groupBy { getVersion(saveGetAlignmentConfig(it), it.class.simpleName) }.each { String version, List<AbstractMergedBamFile> ambfsOfVersion ->
                    setValueOfMap(table, project, pipeline, seqType, version, ambfsOfVersion)
                }
            }
        }
    }

    getBamFilePairAnalysesOfProject(project).groupBy { it.config.pipeline }.each { Pipeline pipeline, List<BamFilePairAnalysis> bfpasOfPipeline ->
        if (pipeline in selectedAndSortedPipelines) {
            bfpasOfPipeline.groupBy { it.config.seqType }.each { SeqType seqType, List<BamFilePairAnalysis> bfpasOfSeqType ->
                bfpasOfSeqType.groupBy { getVersion(it.config, it.class.simpleName) }.each { String version, List<BamFilePairAnalysis> bfpasOfVersion ->
                    setValueOfMap(table, project, pipeline, seqType, version, bfpasOfVersion)
                }
            }
        }
    }
}

Map<Pipeline, List<SeqType>> seqTypesPerPipeline = [:]
selectedAndSortedPipelines.each { seqTypesPerPipeline[(it)] = it.seqTypes }

// Determine unmapped versions
List<String> allVersions = table.keySet().collect { Project project ->
    table[project].keySet().collect { Pipeline pipeline ->
        table[project][pipeline].keySet().collect { SeqType seqType ->
            table[project][pipeline][seqType].keySet()
        }
    }
}.flatten().unique()

List<String> allUnmappedVersions = allVersions.findAll { String version ->
    !Globals.versionMapping.keySet().any { VersionState key ->
        Globals.versionMapping[key].contains(version)
    }
}

Globals.versionMapping[VersionState.UNKNOWN].addAll(allUnmappedVersions)


List<String> output = []

/*
 Construct the header
 first line contains the Pipeline, spanning as many columns as it has SeqTypes
 second line contains the SeqTypes
*/

List<String> pipelineHeader = ["<tr>", "<th></th>"]
List<String> seqTypeHeader = ["<tr>", "<th>Project</th>"]

selectedAndSortedPipelines.each { Pipeline pipeline ->
    List<SeqType> seqTypes = seqTypesPerPipeline[pipeline]
    pipelineHeader << "<th colspan=\"${seqTypes.size()}\">${pipeline.name}</th>"
    if (!seqTypes) {
        seqTypeHeader << "<th></th>"
    }
    seqTypes.each { SeqType seqType ->
        seqTypeHeader << "<th>${getFormattedSeqTypeString(seqType)}</th>"
    }
}

pipelineHeader << "</tr>"
seqTypeHeader << "</tr>"

String header = ["<thead>", pipelineHeader.join("\n"), seqTypeHeader.join("\n"), "</thead>"].join("\n")


output << "<tbody>"
table.keySet().each { Project project ->
    output << "<tr>"
    output << "<td id=\"${project.name}\">${project}</td>"
    selectedAndSortedPipelines.each { Pipeline pipeline ->
        List<SeqType> seqTypes = seqTypesPerPipeline[pipeline]
        if (!seqTypes) {
            output << "<td></td>"
        }
        seqTypes.collect { SeqType seqType ->
            Map<String, List> versions = [:]
            try {
                versions = table[(project)][(pipeline)][(seqType)]
            } catch (NullPointerException e) { }
            String versionListing = versions.collect { String version, List<ProcessParameterObject> processParameterObjectList ->
                Integer numberOfUniqueProcessableObjects = getProcessableObjects(processParameterObjectList).unique().size()
                "<span class=\"${getUpToDateClass(version).name()}\">${parseVersionFromString(version)} : ${numberOfUniqueProcessableObjects}</span>"
            }.join("<br>")

            Integer processObjectsCount
            if (pipeline.type == Pipeline.Type.ALIGNMENT) {
                processObjectsCount = getCountOfAbstractMergingWorkPackageByProjectAndSeqType(project, seqType)
            } else {
                processObjectsCount = getCountOfSamplePairsByProjectAndSeqType(project, seqType)
            }
            output << [
                    "<td>",
                    versionListing ? "<strong>Avail.: ${processObjectsCount}</strong><br>" : "",
                    versionListing,
                    "</td>"
            ].join("")
        }
    }
    output << "</tr>"
}
output << "</tbody>"


List<Object> getProcessableObjects(List<ProcessParameterObject> objs) {
    if (objs.first() instanceof BamFilePairAnalysis) {
        return objs*.samplePair
    } else if (objs.first() instanceof AbstractBamFile) {
        return objs*.getMergingWorkPackage()
    } else {
        throw new UnsupportedOperationException("Could not extract processable object from ${objs.id}")
    }
}

int getCountOfAbstractMergingWorkPackageByProjectAndSeqType(Project project, SeqType seqType) {
    return AbstractMergingWorkPackage.createCriteria().get {
        projections {
            count()
        }
        eq("seqType", seqType)
        sample {
            individual {
                eq("project", project)
            }
        }
    }
}

int getCountOfSamplePairsByProjectAndSeqType(Project project, SeqType seqType) {
    return SamplePair.createCriteria().get {
        projections {
            count()
        }
        mergingWorkPackage1 {
            eq("seqType", seqType)
            sample {
                individual {
                    eq("project", project)
                }
            }
        }
    }
}

VersionState getUpToDateClass(String version) {
    for (versionState in VersionState.getInOrderOfImportance()) {
        if (Globals.versionMapping[versionState]?.contains(version)) {
            return versionState
        }
    }
    return VersionState.UNKNOWN
}

ConfigPerProjectAndSeqType saveGetAlignmentConfig(AbstractMergedBamFile bamFile) {
    try {
        return bamFile.getAlignmentConfig()
    }  catch (MissingPropertyException e) {
        return null
    }
}

String getFormattedSeqTypeString(SeqType seqType) {
    return seqType.toString().split(" ").join("<br>")
}

String parseVersionFromString(String versionString) {
    if (versionString.startsWith(Globals.UNVERSIONED_PREFIX)) {
        return versionString
    }
    if (versionString =~ ~/:(.+?)$/) {
        return Matcher.lastMatcher[0][1]
    }
    if (versionString =~ ~/(?:yapsa-devel|cellranger)\/(.+)$/) {
        return Matcher.lastMatcher[0][1]
    }
    if (versionString =~ ~/^[\.0-9]+$/) {
        return versionString
    }
    println("WARNING: could not parse a version from: \"${versionString}\"")
    return versionString
}

String getVersion(ConfigPerProjectAndSeqType config, String identifier) {
    return config?.programVersion ?: "${Globals.UNVERSIONED_PREFIX} ${identifier}"
}

@TupleConstructor
enum VersionState {
    DEPRECATED(1, "#ffff66"),
    UP_TO_DATE(2, "#66ff66"),
    OUTDATED(3, "#ff6666"),
    UNIMPORTANT(4, "#9966ff"),
    UNKNOWN(5, "#6666ff")

    int importanceIndex
    String color

    static List<VersionState> getInOrderOfImportance() {
        return values().sort { it.importanceIndex }
    }

    @Override
    String toString() {
        return this.name()
    }
}

void setValueOfMap(Map map, Project project, Pipeline pipeline, SeqType seqType, String version, Object value) {
    if (map[(project)] == null) {
        map[(project)] = [:]
    }
    if (map[(project)][(pipeline)] == null) {
        map[(project)][(pipeline)] = [:]
    }
    if (map[(project)][(pipeline)][(seqType)] == null) {
        map[(project)][(pipeline)][(seqType)] = [:]
    }
    if (map[(project)][(pipeline)][(seqType)][(version)] == null) {
        map[(project)][(pipeline)][(seqType)][(version)] = [:]
    }
    map[(project)][(pipeline)][(seqType)][(version)] = value
}


// Build the HTML report
List<String> combinedOutput = []
combinedOutput << """
<html>
<head>"""

// CSS
combinedOutput << """
<style>
table, tr, th, td {
    border: 1px solid black;
    white-space: nowrap;
}
tr:hover {
    background-color: #FFFF66
}"""

combinedOutput << VersionState.getInOrderOfImportance().collect { VersionState versionState ->
    return "span.${versionState.name()} {\n    background-color: ${versionState.color}\n}"
}.join("\n")

combinedOutput << """</style>
</head>
<body>
"""

// Legend Table
combinedOutput << "<table>"

combinedOutput << VersionState.values().collect { VersionState versionState ->
    return """<tr>
<td>${"<span class=\""+ versionState + "\">" + versionState + "</span>"}</td>
<td>${(Globals.versionMapping[versionState] ?: [""]).join("<br>")}</td>
</tr>
"""
}.join("")

combinedOutput << "</table><br>"

// Main Table
combinedOutput << "<table>"
combinedOutput << header
combinedOutput << output.join("\n")
combinedOutput << """</table>
</body>
</html>
"""

println(" ++++ copy from here on ++++")
println(combinedOutput.join("\n"))

''
