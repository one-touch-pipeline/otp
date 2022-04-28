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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class ProcessedBamFileSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BamFilePairAnalysis,
                BedFile,
                DataFile,
                FileType,
                MergingSetAssignment,
                ProcessedBamFile,
                ProcessedMergedBamFile,
                ReferenceGenomeProjectSeqType,
                FastqImportInstance,
        ]
    }

    void "getBedFile, when exome, then return bedFile"() {
        given:
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenomeLazy()

        SeqTrack exomeSeqTrack = createExomeSeqTrack([
                seqType              : exomeSeqType,
                libraryPreparationKit: libraryPreparationKit,
        ])

        DomainFactory.createReferenceGenomeProjectSeqType([
                seqType        : exomeSeqType,
                referenceGenome: referenceGenome,
        ])

        BedFile bedFile = DomainFactory.createBedFile([
                referenceGenome      : referenceGenome,
                libraryPreparationKit: libraryPreparationKit,
        ])

        AlignmentPass exomeAlignmentPass = DomainFactory.createAlignmentPass(
                seqTrack: exomeSeqTrack,
        )

        ProcessedBamFile exomeBamFile = DomainFactory.createProcessedBamFile([
                alignmentPass: exomeAlignmentPass,
        ])

        expect:
        exomeBamFile.bedFile == bedFile
    }

    void "getBedFile, when not exome, then throw an exception"() {
        given:
        ProcessedBamFile bamFile = DomainFactory.createProcessedBamFile()

        when:
        bamFile.bedFile

        then:
        AssertionError e = thrown()
        e.message.contains("A BED file is only available when needed")
    }

    void "withdraw, when it call, then bam file and depending bam file are marked as withdrawn"() {
        given:
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        ProcessedBamFile bamFile = DomainFactory.assignNewProcessedBamFile(processedMergedBamFile)

        when:
        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }

        then:
        bamFile.withdrawn
        processedMergedBamFile.withdrawn
    }
}
