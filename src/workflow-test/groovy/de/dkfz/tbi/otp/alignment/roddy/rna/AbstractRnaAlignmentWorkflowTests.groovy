/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.alignment.roddy.rna

import grails.converters.JSON

import de.dkfz.tbi.otp.alignment.roddy.AbstractRoddyAlignmentWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeProjectSeqTypeService
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.Files
import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

abstract class AbstractRnaAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests implements DomainFactoryCore {

    void "testAlignLanesOnly_OneLane_allFine"() {
        given:
        SessionUtils.withTransaction {
            createProjectConfigRna(exactlyOneElement(MergingWorkPackage.findAll()), [:])
            createSeqTrack("readGroup1")
        }

        when:
        execute()

        then:
        checkWorkPackageState()
        SessionUtils.withTransaction {
            RoddyBamFile bamFile = exactlyOneElement(RnaRoddyBamFile.findAll())
            checkFirstBamFileState(bamFile, true)
            assertBamFileFileSystemPropertiesSet(bamFile)

            checkFileSystemState(bamFile)
            checkFileSystemStateRna(bamFile)

            checkQcRna(bamFile)
            return true
        }
    }

    void "testAlignLanesOnly_withoutArribaProcessing_OneLane_allFine"() {
        given:
        SessionUtils.withTransaction {
            createProjectConfigRna(exactlyOneElement(MergingWorkPackage.findAll()), [
                    configVersion: "v2_0",
            ], [
                    mouseData: true,
            ])
            createSeqTrack("readGroup1")
        }

        when:
        execute()

        then:
        checkWorkPackageState()
        SessionUtils.withTransaction {
            RoddyBamFile bamFile = exactlyOneElement(RnaRoddyBamFile.findAll())
            checkFirstBamFileState(bamFile, true)
            assertBamFileFileSystemPropertiesSet(bamFile)
            checkFileSystemState(bamFile)
            checkFileSystemStateRna(bamFile)

            checkQcRna(bamFile)
            return true
        }
    }

    static void checkFileSystemStateRna(RnaRoddyBamFile bamFile) {
        [
                bamFile.workDirectory,
                bamFile.workQADirectory,
                bamFile.workExecutionStoreDirectory,
                bamFile.workMergedQADirectory,
        ].each {
            assert it.exists() && it.isDirectory() && it.canRead() && it.canExecute()
        }

        [
                bamFile.finalQADirectory,
                bamFile.finalExecutionStoreDirectory,
                bamFile.finalBamFile,
                bamFile.finalBaiFile,
                bamFile.finalMd5sumFile,
                bamFile.finalMergedQADirectory,
        ].each {
            assert it.exists() && Files.isSymbolicLink(it.toPath()) && it.canRead()
        }

        [
                bamFile.workBamFile,
                bamFile.workBaiFile,
                bamFile.workMd5sumFile,
                bamFile.correspondingWorkChimericBamFile,
                bamFile.workMergedQAJsonFile,
        ].each {
            assert it.exists() && it.isFile() && it.canRead() && it.size() > 0
        }
    }

    static void checkQcRna(RnaRoddyBamFile bamFile) {
        RnaQualityAssessment rnaQa = CollectionUtils.atMostOneElement(RnaQualityAssessment.findAllByAbstractBamFile(bamFile))
        assert rnaQa

        JSON.parse(bamFile.finalMergedQAJsonFile.text)
        assert bamFile.finalMergedQAJsonFile.text.trim() != ""

        assert bamFile.coverage == null

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.UNCHECKED
    }

    @Override
    Pipeline findPipeline() {
        return DomainFactory.createRnaPipeline()
    }

    @Override
    void createProjectConfig(MergingWorkPackage workPackage, Map options = [:]) { }

    @SuppressWarnings('Indentation')
    void createProjectConfigRna(MergingWorkPackage workPackage, Map configOptions = [:], Map referenceGenomeConfig = [:]) {
        createDirectories([new File(projectService.getSequencingDirectory(workPackage.project).toString())])

        GeneModel geneModel = DomainFactory.createGeneModel(
                referenceGenome: workPackage.referenceGenome,
                path: "gencode19",
                fileName: "gencode.v19.annotation_plain.gtf",
                excludeFileName: "gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf",
                dexSeqFileName: "gencode.v19.annotation_plain.dexseq.gff",
                gcFileName: "gencode.v19.annotation_plain.transcripts.autosomal_transcriptTypeProteinCoding_nonPseudo.1KGRef.gc",
        )

        ToolName toolNameGatk = createToolName(
                name: "GENOME_GATK_INDEX",
                path: "gatk",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameStar200 = createToolName(
                name: "GENOME_STAR_INDEX_200",
                path: "star_200",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameKallisto = createToolName(
                name: ProjectService.GENOME_KALLISTO_INDEX,
                path: "kallisto",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameArribaFusions = createToolName(
                name: ProjectService.ARRIBA_KNOWN_FUSIONS,
                path: "arriba-fusion",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameArribaBlacklist = createToolName(
                name: ProjectService.ARRIBA_BLACKLIST,
                path: "arriba-blacklist",
                type: ToolName.Type.RNA,
        )

        ReferenceGenomeIndex referenceGenomeIndexGatk = createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameGatk,
                path: "hs37d5_PhiX.fa",
        )
        ReferenceGenomeIndex referenceGenomeIndexStar200 = createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameStar200,
                path: "STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp",
        )
        ReferenceGenomeIndex referenceGenomeIndexKallisto = createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameKallisto,
                path: "kallisto-0.43.0_1KGRef_Gencode19_k31.index",
        )
        ReferenceGenomeIndex referenceGenomeIndexArribaFusions = createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameArribaFusions,
                path: "known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz",
        )
        ReferenceGenomeIndex referenceGenomeIndexArribaBlacklist = createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameArribaBlacklist,
                path: "blacklist_hs37d5_gencode19_2017-01-09.tsv.gz",
        )

        doWithAuth(OPERATOR) {
            projectService.configureRnaAlignmentConfig(new RoddyConfiguration([
                    project          : workPackage.project,
                    seqType          : workPackage.seqType,
                    pluginName       : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, workPackage.seqType.roddyName),
                    programVersion   : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION, workPackage.seqType.roddyName),
                    baseProjectConfig: processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG, workPackage.seqType.roddyName),
                    configVersion    : "v1_0",
                    resources        : "t",
            ] + configOptions))
            projectService.configureRnaAlignmentReferenceGenome(new RnaAlignmentReferenceGenomeConfiguration([
                    project             : workPackage.project,
                    seqType             : workPackage.seqType,
                    referenceGenome     : workPackage.referenceGenome,
                    geneModel           : geneModel,
                    referenceGenomeIndex: [
                            referenceGenomeIndexGatk,
                            referenceGenomeIndexStar200,
                            referenceGenomeIndexKallisto,
                            referenceGenomeIndexArribaFusions,
                            referenceGenomeIndexArribaBlacklist,
                    ],
            ] + referenceGenomeConfig))

            workPackage.alignmentProperties.addAll(ReferenceGenomeProjectSeqTypeService
                    .getConfiguredReferenceGenomeProjectSeqType(workPackage.project, workPackage.seqType, workPackage.sampleType)
                    .alignmentProperties.collect {
                new MergingWorkPackageAlignmentProperty(name: it.name, value: it.value, mergingWorkPackage: workPackage)
            })
            workPackage.save(flush: true)
        }
        assert ReferenceGenomeProjectSeqTypeAlignmentProperty.list().size() >= 1
        assert MergingWorkPackageAlignmentProperty.list().size() >= 1
    }

    @Override
    void setUpFilesVariables() {
        testFastqFiles = [
                readGroup1: [
                        new File(inputRootDirectory, 'fastqFiles/rna/tumor/paired/run4/sequence/gerald_D1VCPACXX_x_R1.fastq.gz'),
                        new File(inputRootDirectory, 'fastqFiles/rna/tumor/paired/run4/sequence/gerald_D1VCPACXX_x_R2.fastq.gz'),
                ].asImmutable(),
        ].asImmutable()
        firstBamFile = null
        refGenDir = new File(referenceGenomeDirectory, 'bwa06_1KGRef_PhiX')
        fingerPrintingFile = new File(referenceGenomeDirectory, 'bwa06_1KGRef_PhiX/fingerPrinting/snp138Common.n1000.vh20140318.bed')
    }

    @Override
    protected String getRefGenFileNamePrefix() {
        return 'hs37d5_PhiX'
    }

    @Override
    protected String getReferenceGenomeSpecificPath() {
        return 'bwa06_1KGRef_PhiX'
    }

    @Override
    protected String getChromosomeStatFileName() {
        return null
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RnaAlignmentWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(5)
    }
}
