package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification


@TestFor(ChromosomeIdentifierMappingService)
@TestMixin(GrailsUnitTestMixin)
@Mock([ReferenceGenome, ReferenceGenomeEntry, Realm, Project, SeqType, ReferenceGenomeProjectSeqType])
class ChromosomeIdentifierMappingServiceTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry
    ReferenceGenomeEntry referenceGenomeEntryTwo
    Project project
    SeqType seqType

    ChromosomeIdentifierMappingService chromosomeIdentifierMappingService

    @Before
    void setUp() {
        chromosomeIdentifierMappingService = new ChromosomeIdentifierMappingService()
        chromosomeIdentifierMappingService.referenceGenomeService = new ReferenceGenomeService()

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()

        project = new Project()
        project.name = "SOME_PROJECT"
        project.dirName = "/tmp/alignmentPassService/"
        project.realmName = realm.name
        project.save(flush: true)

        seqType = new SeqType()
        seqType.name = "WHOLE_GENOME"
        seqType.libraryLayout = "SINGLE"
        seqType.dirName = "whole_genome_sequencing"
        seqType.save(flush: true)

        referenceGenome = new ReferenceGenome(
                        name: "hg19_1_24",
                        path: "referenceGenome",
                        fileNamePrefix: "prefixName",
                        length: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthRefChromosomes: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthRefChromosomesWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        )
        referenceGenome.save(flush: true, failOnError: true)

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(
                        project: project,
                        seqType: seqType,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeProjectSeqType.save(flush: true, failOnError: true)

        referenceGenomeEntry = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "1",
                        classification: Classification.CHROMOSOME,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntry.save(flush: true, failOnError: true)

        referenceGenomeEntryTwo = new ReferenceGenomeEntry(
                        name: "chr2",
                        alias: "2",
                        classification: Classification.UNDEFINED,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntryTwo.save(flush: true, failOnError: true)
    }

    void tearDown() {
        referenceGenome = null
        referenceGenomeEntry = null
        referenceGenomeEntryTwo = null
        project = null
        seqType = null
    }

    void testMappingAllFromReferenceGenome() {
        Map<String, String> mappingExp = ["chr1":"1", "chr2":"2", "*": "*"]
        Map<String, String> mappingAct = chromosomeIdentifierMappingService.mappingAll(referenceGenome)
        assertEquals(mappingExp, mappingAct)
    }

    void testMappingAllFromProjectAndSeqType() {
        Map<String, String> mappingExp = ["chr1":"1", "chr2":"2", "*": "*"]
        Map<String, String> mappingAct = chromosomeIdentifierMappingService.mappingAll(project, seqType)
        assertEquals(mappingExp, mappingAct)
    }

    void testMappingOneFromProjectAndSeqType() {
        String mappingExp = "1"
        String mappingAct = chromosomeIdentifierMappingService.mappingOne("chr1", project, seqType)
        assertEquals(mappingExp, mappingAct)
    }

    void testMappingOneFromReferenceGenome() {
        String mappingExp = "1"
        String mappingAct = chromosomeIdentifierMappingService.mappingOne("chr1", referenceGenome)
        assertEquals(mappingExp, mappingAct)
    }
}
