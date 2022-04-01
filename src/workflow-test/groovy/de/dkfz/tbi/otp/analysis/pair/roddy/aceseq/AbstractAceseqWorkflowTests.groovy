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
package de.dkfz.tbi.otp.analysis.pair.roddy.aceseq

import grails.plugin.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.analysis.pair.roddy.AbstractRoddyBamFilePairAnalysisWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.project.RoddyConfiguration
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.Path
import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

abstract class AbstractAceseqWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<AceseqInstance> {

    AceseqService aceseqService
    LsdfFilesService lsdfFilesService
    ProjectService projectService
    SophiaService sophiaService

    @Override
    void setupData() {
        SessionUtils.withTransaction {
            createSophiaInput()
            super.setupData()
        }
    }

    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createProcessingOptionLazy([
                name   : PIPELINE_MIN_COVERAGE,
                type   : Pipeline.Type.ACESEQ.toString(),
                project: null,
                value  : "20",
        ])
        DomainFactory.createAceseqPipelineLazy()
        DomainFactory.createAceseqSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        createDirectories([new File(projectService.getSequencingDirectory(project).toString())])

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            config = projectService.configureAceseqPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(PIPELINE_ACESEQ_DEFAULT_PLUGIN_NAME),
                            programVersion   : processingOptionService.findOptionAsString(PIPELINE_ACESEQ_DEFAULT_PLUGIN_VERSION, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(PIPELINE_ACESEQ_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    @SuppressWarnings('GStringExpressionWithinString')
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        referenceGenome.gcContentFile = 'hg19_GRch37_100genomes_gc_content_10kb.txt'

        File referenceGenomePath = new File(referenceGenomeService.referenceGenomeDirectory(referenceGenome, false), 'databases')
        referenceGenome.geneticMapFile = new File(referenceGenomePath, 'IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/genetic_map_chr${CHR_NAME}_combined_b37.txt').absolutePath
        referenceGenome.geneticMapFileX = new File(referenceGenomePath, 'IMPUTE/ALL_1000G_phase1integrated_v3_impute/genetic_map_chrX_nonPAR_combined_b37.txt').absolutePath
        referenceGenome.knownHaplotypesFile = new File(referenceGenomePath, 'IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.haplotypes.gz').absolutePath
        referenceGenome.knownHaplotypesFileX = new File(referenceGenomePath, 'IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.hap.gz').absolutePath
        referenceGenome.knownHaplotypesLegendFile = new File(referenceGenomePath, 'IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.legend.gz').absolutePath
        referenceGenome.knownHaplotypesLegendFileX = new File(referenceGenomePath, 'IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.legend.gz').absolutePath
        referenceGenome.mappabilityFile = new File(referenceGenomePath, 'UCSC/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz').absolutePath
        referenceGenome.replicationTimeFile = new File(referenceGenomePath, 'ENCODE/ReplicationTime_10cellines_mean_10KB.Rda').absolutePath
        referenceGenome.save(flush: true)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processingOptionService.createOrUpdate(
                    OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }

    void createSophiaInput() {
        File sourceSophiaInputFile = new File(workflowData, "svs_stds_filtered_somatic_minEventScore3.tsv")

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstance(samplePair)
        File sophiaInputFile = new File(sophiaService.getFinalAceseqInputFile(sophiaInstance).toString())
        SamplePair sp = SamplePair.get(samplePair.id)
        sp.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        assert sp.save(flush: true)

        linkFileUtils.createAndValidateLinks([
                (sourceSophiaInputFile): sophiaInputFile,
        ], realm)
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddyAceseqWorkflow.groovy",
        ]
    }

    List<Path> filesToCheck(AceseqInstance aceseqInstance) {
        return aceseqService.getAllFiles(aceseqInstance)
    }

    @Override
    void checkAnalysisSpecific(AceseqInstance aceseqInstance) {
        assert AceseqQc.countByAceseqInstance(aceseqInstance) > 0
    }

    @Override
    File getWorkflowData() {
        new File(inputRootDirectory, 'aceseq')
    }

    @Override
    Duration getTimeout() {
        Duration.ofHours(24)
    }
}
