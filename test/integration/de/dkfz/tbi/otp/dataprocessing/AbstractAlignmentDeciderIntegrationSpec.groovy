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

package de.dkfz.tbi.otp.dataprocessing

import grails.test.spock.IntegrationSpec

import de.dkfz.tbi.otp.ngsdata.*

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

    @SuppressWarnings('SpaceAfterOpeningBrace')
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
