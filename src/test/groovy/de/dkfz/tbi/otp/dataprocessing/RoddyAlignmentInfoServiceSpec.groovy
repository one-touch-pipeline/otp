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
package de.dkfz.tbi.otp.dataprocessing

import com.fasterxml.jackson.core.JsonProcessingException
import grails.testing.gorm.DataTest
import groovy.json.JsonOutput
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

class RoddyAlignmentInfoServiceSpec extends Specification implements DataTest, IsRoddy {

    private static final String PROGRAM_VERSION = "1.2.73-204"

    private static final Map<String, String> BWA_VALUES = [
            BWA_VERSION                      : "1.0",
            BWA_MEM_OPTIONS                  : "alnOpt",
            BWA_ACCELERATED_VERSION          : "2.0",
            BWA_MEM_CONVEY_ADDITIONAL_OPTIONS: "conveyOpt",
    ].asImmutable()

    private static final Map<String, String> MERGE_VALUES = [
            BIOBAMBAM_VERSION                    : "3.0",
            mergeAndRemoveDuplicates_argumentList: "bioBamBamOpt",
            PICARD_VERSION                       : "4.0",
            SAMBAMBA_MARKDUP_VERSION             : "5.0",
            SAMBAMBA_MARKDUP_OPTS                : "sambambaOpt",
            SAMTOOLS_VERSION                     : "6.0",
    ].asImmutable()

    private final RoddyAlignmentInfoService service = new RoddyAlignmentInfoService()

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
        ]
    }

    @Unroll
    void "getAlignmentInfo, when #name, then return the tools used by the run"() {
        given:
        WorkflowRun run = createRunWithCValues(BWA_VALUES + MERGE_VALUES + toolSelection)

        when:
        RoddyAlignmentInfo alignmentInfo = service.getAlignmentInfo(run, createBamFileOfSeqType(SeqTypeNames.WHOLE_GENOME))

        then:
        alignmentInfo.alignmentProgram == alignmentProgram
        alignmentInfo.alignmentParameter == alignmentParameter
        alignmentInfo.mergeCommand == mergeCommand
        alignmentInfo.mergeOptions == mergeOptions
        alignmentInfo.samToolsCommand == "Version 6.0"
        alignmentInfo.programVersion == PROGRAM_VERSION

        where:
        name                                          | toolSelection                                                         || alignmentProgram     | alignmentParameter  | mergeCommand                              | mergeOptions
        "sambamba is selected"                        | [markDuplicatesVariant: "sambamba"]                                   || "BWA Version 1.0"    | "alnOpt"            | "Sambamba Version 5.0"                    | "sambambaOpt"
        "biobambam is selected"                       | [markDuplicatesVariant: "biobambam"]                                  || "BWA Version 1.0"    | "alnOpt"            | "Biobambam bammarkduplicates Version 3.0" | "bioBamBamOpt"
        "picard is selected"                          | [markDuplicatesVariant: "picard"]                                     || "BWA Version 1.0"    | "alnOpt"            | "Picard Version 4.0"                      | ""
        "no variant, but biobambam flag is set"       | [markDuplicatesVariant: "", useBioBamBamMarkDuplicates: "true"]       || "BWA Version 1.0"    | "alnOpt"            | "Biobambam bammarkduplicates Version 3.0" | "bioBamBamOpt"
        "no variant and no biobambam flag"            | [markDuplicatesVariant: ""]                                           || "BWA Version 1.0"    | "alnOpt"            | "Picard Version 4.0"                      | ""
        "accelerated hardware is used"                | [markDuplicatesVariant: "sambamba", useAcceleratedHardware: "true"]   || "bwa-bb Version 2.0" | "alnOpt conveyOpt"  | "Sambamba Version 5.0"                    | "sambambaOpt"
    }

    void "getAlignmentInfo, when rna, then return the STAR and sambamba tools"() {
        given:
        WorkflowRun run = createRunWithCValues([
                STAR_VERSION        : "2.0",
                STAR_PARAMS_2PASS   : "2PASS",
                STAR_PARAMS_OUT     : "OUT",
                STAR_PARAMS_CHIMERIC: "CHIMERIC",
                STAR_PARAMS_INTRONS : "INTRONS",
                SAMBAMBA_VERSION    : "3.0",
                SAMTOOLS_VERSION    : "1.0",
        ])

        when:
        RoddyAlignmentInfo alignmentInfo = service.getAlignmentInfo(run, createBamFileOfSeqType(SeqTypeNames.RNA))

        then:
        alignmentInfo.alignmentProgram == "STAR Version 2.0"
        alignmentInfo.alignmentParameter == "2PASS OUT CHIMERIC INTRONS"
        alignmentInfo.mergeCommand == "Sambamba Version 3.0"
        alignmentInfo.mergeOptions == ""
        alignmentInfo.samToolsCommand == "Version 1.0"
        alignmentInfo.programVersion == PROGRAM_VERSION
    }

    @Unroll
    void "getAlignmentInfo, when #name is missing, then throw ParsingException"() {
        given:
        WorkflowRun run = createRunWithCValues(BWA_VALUES + MERGE_VALUES + [markDuplicatesVariant: "sambamba"] - [(key): BWA_VALUES[key] ?: MERGE_VALUES[key]])

        when:
        service.getAlignmentInfo(run, createBamFileOfSeqType(SeqTypeNames.WHOLE_GENOME))

        then:
        thrown(ParsingException)

        where:
        name                 | key
        "alignment program"  | "BWA_VERSION"
        "merging program"    | "SAMBAMBA_MARKDUP_VERSION"
    }

    void "getAlignmentInfo, when combined config is no valid json, then throw JsonProcessingException"() {
        given:
        WorkflowRun run = new WorkflowRun(combinedConfig: "{invalid", workflowVersion: new WorkflowVersion(workflowVersion: PROGRAM_VERSION))

        when:
        service.getAlignmentInfo(run, createBamFileOfSeqType(SeqTypeNames.WHOLE_GENOME))

        then:
        thrown(JsonProcessingException)
    }

    private WorkflowRun createRunWithCValues(Map<String, String> cValues) {
        String combinedConfig = JsonOutput.toJson([
                RODDY: [
                        cvalues: cValues.collectEntries { String key, String value ->
                            [(key): [value: value, type: "string"]]
                        },
                ],
        ])
        return new WorkflowRun(combinedConfig: combinedConfig, workflowVersion: new WorkflowVersion(workflowVersion: PROGRAM_VERSION))
    }

    private RoddyBamFile createBamFileOfSeqType(SeqTypeNames seqTypeName) {
        return createBamFile([
                workPackage: createMergingWorkPackage([
                        seqType: createSeqType([name: seqTypeName.seqTypeName]),
                ]),
        ])
    }
}
