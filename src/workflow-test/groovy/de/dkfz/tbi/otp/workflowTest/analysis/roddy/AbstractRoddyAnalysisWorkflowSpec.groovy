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
package de.dkfz.tbi.otp.workflowTest.analysis.roddy

import spock.lang.IgnoreIf
import spock.lang.Shared

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowTest.analysis.AbstractAnalysisWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.roddy.RoddyReferences

import java.nio.file.Path

abstract class AbstractRoddyAnalysisWorkflowSpec<I extends BamFilePairAnalysis> extends AbstractAnalysisWorkflowSpec implements RoddyReferences, RoddyPanCancerFactory,
        FastqcDomainFactory {

    /**
     * These two options have been added to speedup testing.
     * It is not possible to automatically execute multiple test methods in parallel
     * if they are located in the same class.
     * Therefore you have to create two classes and only activate one of the two tests in each.
     **/
    @Shared
    boolean ignoreRoddyBamFileTest = false

    @Shared
    boolean ignoreExternalBamFileTest = false

    @IgnoreIf({ instance.ignoreRoddyBamFileTest })
    void "testWholeWorkflowWithRoddyBamFile"() {
        given:
        SessionUtils.withTransaction {
            setupRoddyBamFile()
            setupData()
            decide(expectedExistingWorkflowArtefactCount, expectedNewWorkflowArtefactCount)
        }

        when:
        execute()

        then:
        checkInstance()
    }

    @IgnoreIf({ instance.ignoreRoddyBamFileTest })
    void "testWholeWorkflowWithRoddyBamFileLowCoverage"() {
        given:
        SessionUtils.withTransaction {
            setupRoddyBamFileLowCoverage()
            setupData()
            decide(expectedExistingWorkflowArtefactCount, expectedNewWorkflowArtefactCount)
        }

        when:
        execute(CheckType.SKIPPED)

        then:
        SessionUtils.withTransaction {
            I createdInstance = BamFilePairAnalysis.listOrderById().last()
            assert createdInstance.processingState == AnalysisProcessingStates.IN_PROGRESS
            assert createdInstance.withdrawn
            assert createdInstance.sampleType1BamFile == bamFileTumor
            assert createdInstance.sampleType2BamFile == bamFileControl
            true
        }
    }

    @IgnoreIf({ instance.ignoreExternalBamFileTest })
    void "testWholeWorkflowWithExternalBamFile"() {
        given:
        SessionUtils.withTransaction {
            setupExternalBamFile()
            setupData()
            decide(expectedExistingWorkflowArtefactCount, expectedNewWorkflowArtefactCount)
        }

        when:
        execute()

        then:
        checkInstance()
    }

    void checkInstance() {
        SessionUtils.withTransaction {
            I createdInstance = BamFilePairAnalysis.listOrderById().last()
            assert createdInstance.processingState == AnalysisProcessingStates.FINISHED
            assert createdInstance.sampleType1BamFile == bamFileTumor
            assert createdInstance.sampleType2BamFile == bamFileControl

            filesToCheck(createdInstance).flatten().each { Path file ->
                fileService.ensureFileIsReadableAndNotEmpty(file)
            }
            checkQc(createdInstance)
        }
    }

    /**
     * Provides the reference genome depending VEP settings.
     *
     * General are provided via default fragments.
     *
     * The location specific cvalue VEP_CACHE_BASE needs to be set via otp init script.
     */
    @SuppressWarnings('GStringExpressionWithinString')
    void createVepFragment() {
        workflowAnalysis.refresh()
        referenceGenome.refresh()
        String fragment = """
                    {
                        "RODDY": {
                            "cvalues": {
                                "VEP_FA_INDEX": {
                                    "type": "path",
                                    "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/tools_data/VEP/reference/hs37d5_PhiX.fa"
                                },
                                "VEP_PLUGIN_CADD_SNV": {
                                    "type": "path",
                                    "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/tools_data/VEP/plugins/CADD/v1.7/whole_genome_SNVs.tsv.gz"
                                },
                                "VEP_PLUGIN_SPLICEAI_SNV": {
                                    "type": "path",
                                    "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/tools_data/VEP/plugins/SpliceAI/v1.3/spliceai_scores.raw.snv.hg19.vcf.gz"
                                },
                                "VEP_PLUGIN_SPLICEAI_INDEL": {
                                    "type": "path",
                                    "value": "\${BASE_REFERENCE_GENOME}/bwa06_1KGRef_PhiX/tools_data/VEP/plugins/SpliceAI/v1.3/spliceai_scores.raw.indel.hg19.vcf.gz"
                                },
                                "VEP_SPECIES": {
                                    "type": "string",
                                    "value": "homo_sapiens"
                                },
                                "VEP_ASSEMBLY": {
                                    "type": "string",
                                    "value": "GRCh37"
                                }
                            }
                        }
                    }
                """
        createFragmentAndSelector("reference genome depending VEP", fragment, [
                workflows       : [workflowAnalysis],
                referenceGenomes: [referenceGenome],
        ])
    }

    abstract List<Path> filesToCheck(I instance)

    abstract void checkQc(I instance)

    int expectedExistingWorkflowArtefactCount = 2
    int expectedNewWorkflowArtefactCount = 1
}
