package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import grails.test.spock.*

class AbstractAlignmentDeciderIntegrationSpec extends IntegrationSpec {

    void "test isLibraryPreparationKitOrBedFileMissing, with null"() {
        when:
        AbstractAlignmentDecider.hasLibraryPreparationKitAndBedFile(null)
        then:
        AssertionError e = thrown()
        e.message.contains("The input seqTrack of method hasLibraryPreparationKitAndBedFile is null")
    }


    void "test isLibraryPreparationKitOrBedFileMissing, with normal seqTrack"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        expect:
        AbstractAlignmentDecider.hasLibraryPreparationKitAndBedFile(seqTrack)
    }

    void "isLibraryPreparationKitOrBedFileMissing, with exome seqTrack"() {
        given:
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitMethod()
        ReferenceGenome referenceGenome = referenceGenomeMethod()
        SeqTrack seqTrack = DomainFactory.createExomeSeqTrack([
                libraryPreparationKit: libraryPreparationKit,

        ])
        DomainFactory.createReferenceGenomeProjectSeqType([
                referenceGenome: referenceGenome ?: DomainFactory.createReferenceGenome(),
                project        : seqTrack.project,
                seqType        : seqTrack.seqType,
        ])
        DomainFactory.createBedFile([
                libraryPreparationKit: libraryPreparationKit ?: DomainFactory.createLibraryPreparationKit(),
                referenceGenome      : referenceGenome ?: DomainFactory.createReferenceGenome(),
        ])

        expect:
        AbstractAlignmentDecider.hasLibraryPreparationKitAndBedFile(seqTrack) == (libraryPreparationKit && referenceGenome)

        where:
        libraryPreparationKitMethod                       | referenceGenomeMethod                       | result
        ({ null })                                        | ({ null })                                  | false
        ({ null })                                        | ({ DomainFactory.createReferenceGenome() }) | false
        ({ DomainFactory.createLibraryPreparationKit() }) | ({ null })                                  | false
        ({ DomainFactory.createLibraryPreparationKit() }) | ({ DomainFactory.createReferenceGenome() }) | true

    }
}
