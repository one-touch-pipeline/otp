package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.Mock
import spock.lang.*

@Mock([
        GeneModel,
        ReferenceGenome,
        ReferenceGenomeIndex,
        ToolName,
])
class RoddyRnaConfigTemplateSpec extends Specification {

    final static String OUTPUT_WITHOUT_MOUSE_DATA =
            "<cvalue name=\"${RoddyRnaConfigTemplate.GENOME_STAR_INDEX}\" value=\"/basePath/indexes/path_without_mouse_star/path_without_mouse\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.ARRIBA_KNOWN_FUSIONS}\" value=\"/basePath/indexes/path_without_mouse_known_fusion/path_without_mouse\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.ARRIBA_BLACKLIST}\" value=\"/basePath/indexes/path_without_mouse_blacklist/path_without_mouse\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS}\" value=\"/basePath/gencode/path_without_mouse/fileName.gtf\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS_DEXSEQ}\" value=\"/basePath/gencode/path_without_mouse/dexSeqFileName.gtf\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS_EXCLUDE}\" value=\"/basePath/gencode/path_without_mouse/excludeFileName.gtf\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS_GC}\" value=\"/basePath/gencode/path_without_mouse/gcFileName.gtf\" type=\"string\"/>"

    final static String OUTPUT_WITH_MOUSE_DATA =
            "<cvalue name=\"${RoddyRnaConfigTemplate.RUN_ARRIBA}\" value=\"false\" type=\"boolean\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.RUN_FEATURE_COUNTS_DEXSEQ}\" value=\"false\" type=\"boolean\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENOME_STAR_INDEX}\" value=\"/basePath/indexes/path_with_mouse_star/path_with_mouse\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS}\" value=\"/basePath/gencode/path_with_mouse/fileName.gtf\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS_EXCLUDE}\" value=\"/basePath/gencode/path_with_mouse/excludeFileName.gtf\" type=\"string\"/>\n" +
            "                <cvalue name=\"${RoddyRnaConfigTemplate.GENE_MODELS_GC}\" value=\"/basePath/gencode/path_with_mouse/gcFileName.gtf\" type=\"string\"/>"


    @Unroll
    void "test getConfigurationValues"() {
        given:
        RnaAlignmentConfiguration rnaAlignmentConfiguration = new RnaAlignmentConfiguration(
                mouseData: mouseData,
                geneModel: DomainFactory.createGeneModel(
                        path: path,
                ),
                referenceGenomeIndex: [
                        DomainFactory.createReferenceGenomeIndex(
                                path: path,
                                toolName: DomainFactory.createToolName(
                                        path: path + "_star",
                                        name: RoddyRnaConfigTemplate.GENOME_STAR_INDEX,
                                ),
                        ),
                        DomainFactory.createReferenceGenomeIndex(
                                path: path,
                                toolName: DomainFactory.createToolName(
                                        path: path + "_known_fusion",
                                        name: RoddyRnaConfigTemplate.ARRIBA_KNOWN_FUSIONS
                                ),
                        ),
                        DomainFactory.createReferenceGenomeIndex(
                                path: path,
                                toolName: DomainFactory.createToolName(
                                        path: path + "_blacklist",
                                        name: RoddyRnaConfigTemplate.ARRIBA_BLACKLIST
                                ),
                        ),
                ]
        )

        ReferenceGenomeService referenceGenomeService = [
                referenceGenomeDirectory: { referenceGenome, checkExistence -> new File("/basePath") }
        ] as ReferenceGenomeService

        GeneModelService geneModelService = new GeneModelService()
        geneModelService.referenceGenomeService = referenceGenomeService

        ReferenceGenomeIndexService referenceGenomeIndexService = new ReferenceGenomeIndexService()
        referenceGenomeIndexService.referenceGenomeService = referenceGenomeService

        when:
        String output = RoddyRnaConfigTemplate.getConfigurationValues(rnaAlignmentConfiguration, referenceGenomeIndexService, geneModelService)

        then:
        output == outputString

        where:
        mouseData | path                 || outputString
        false     | "path_without_mouse" || OUTPUT_WITHOUT_MOUSE_DATA
        true      | "path_with_mouse"    || OUTPUT_WITH_MOUSE_DATA
    }
}
