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

/**
 * Prints a csv listing the number of individuals with sample pairs pairs of the given baseSeqType per project.
 * Additionally it can check for the existence of further data for a given seqType.
 *
 * A "complete dataset" is defined as a patient with disease/control pairs with additional WGBS and RNA data.
 *
 * To use this script, configure the baseSeqType, which is used to look up sample pairs, and then define the
 * seqTypes for "additional data checks" in seqTypesToQuery.
 * Each entry of the list will become its own column, with multiple entries inside an item being ANDed.
 * Be careful with the single/paired variations between seqTypes.
 */

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project;

class Global {
    // User Configuration

    // SeqTypes to use for finding DISEASE/CONTROL SamplePairs
    static String baseSeqType =
            "WHOLE_GENOME"
            // "EXOME"

    // SeqTypes to use for finding additional data
    static List<List<String>> seqTypesToQuery = [
            ["WGBS-tag"],
            ["ChIP"],
            ["RNA_paired"],
            ["WGBS-tag", "RNA"],
            ["WGBS-tag", "ChIP"],
            ["RNA_paired", "ChIP"],
            ["RNA_paired", "WGBS-tag", "ChIP"],
    ]

    static List<Project> projectsToList = Project.findAll()

    // Mapping from actual SeqType entities to the shortcut referenced above
    private static Map<String, SeqType> seqTypeShortcuts = [
            "WHOLE_GENOME": SeqTypeService.wholeGenomePairedSeqType,
            "EXOME"       : SeqTypeService.exomePairedSeqType,
            "WGBS"        : SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            "WGBS-tag"    : SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
            "ChIP"        : SeqTypeService.chipSeqPairedSeqType,
            "RNA_single"  : SeqTypeService.rnaSingleSeqType,
            "RNA_paired"  : SeqTypeService.rnaPairedSeqType,
    ]

    // ------------------------------------

    static SeqType getSeqTypeFromShortcut(String shortcut) {
        return seqTypeShortcuts[shortcut]
    }
}

List<Individual> getIndividualsWithDiseaseControlSamplePairs(Project project, SeqType seqType) {
    return SamplePair.createCriteria().list {
        projections {
            mergingWorkPackage1 {
                sample {
                    individual {
                        eq('project', project)
                    }
                    distinct('individual')
                }
                eq('seqType', seqType)
            }
        }
    } as List<Individual>
}

List<Individual> individualsWithAllSeqTypes(List<Individual> individuals, List<SeqType> seqTypes) {
    assert seqTypes: "No SeqTypes given, this should probably not happen but is equivalent to returning an empty list"

    if (individuals == []) {
        return []
    }

    String hql = """\
        |SELECT
        |    DISTINCT(indiv)
        |FROM
        |    Individual indiv
        |WHERE
        |    indiv.id IN (${individuals*.id.join(", ")}) AND
        |    ${seqTypes.size()} = (
        |    SELECT
        |        COUNT(DISTINCT st.seqType)
        |    FROM
        |        SeqTrack st
        |    WHERE
        |        st.seqType.id IN (${seqTypes*.id.join(", ")}) AND
        |        st.sample.individual.id = indiv
        |    )
        |""".stripMargin()
    return Individual.findAll(hql)
}

List<String> header = [
        "Project",
        "PIDs with ${Global.baseSeqType} sample pairs",
        "${Global.seqTypesToQuery.collect { "of those with ${it.join(" and ")}" }.join(',')}",
]

List<String> output = []

Global.projectsToList.sort { it.name }.collect { Project project ->
    List<String> row = [project.name]

    List<Individual> individualsWithDiseaseControl = getIndividualsWithDiseaseControlSamplePairs(project, Global.getSeqTypeFromShortcut(Global.baseSeqType))
    row << individualsWithDiseaseControl.size().toString()

    Global.seqTypesToQuery.each { List<String> seqTypeNames ->
        row << individualsWithAllSeqTypes(individualsWithDiseaseControl, seqTypeNames.collect { Global.getSeqTypeFromShortcut(it) }).size().toString()
    }

    output << row.join(",")
}

println "${header.join(",")}\n${output.join("\n")}"
