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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.TestData

@Rollback
@Integration
class ProcessedBamFileIntegrationTests {

    TestData data = new TestData()

    void setupData() {
        data.createObjects()
    }

    @Test
    void test_getOverallQualityAssessment_WhenOnePassExists_ShouldReturnThis() {
        setupData()

        final Long ARBITRARY_IDENTIFIER = 42

        def processedBamFile = createProcessedBamFile()
        def oqa = createOverallQualityAssessment(processedBamFile, ARBITRARY_IDENTIFIER)

        assert processedBamFile.overallQualityAssessment == oqa
    }

    @Test
    void test_getOverallQualityAssessment_WhenMultiplePassesExists_ShouldReturnLatest() {
        setupData()

        final Long IDENTIFIER_FORMER = 100
        final Long IDENTIFIER_LATER = 200

        assert IDENTIFIER_FORMER < IDENTIFIER_LATER

        def processedBamFile = createProcessedBamFile()
        def oqaFormer = createOverallQualityAssessment(processedBamFile, IDENTIFIER_FORMER)
        def oqaLater = createOverallQualityAssessment(processedBamFile, IDENTIFIER_LATER)

        assert processedBamFile.overallQualityAssessment == oqaLater
        assert processedBamFile.overallQualityAssessment != oqaFormer
    }

    private ProcessedBamFile createProcessedBamFile() {
        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass()
        alignmentPass.save([flush: true])

        ProcessedBamFile processedBamFile = new ProcessedBamFile([
                type                   : AbstractBamFile.BamType.SORTED,
                withdrawn              : false,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
                alignmentPass          : alignmentPass,
        ])
        assert processedBamFile.save([flush: true])

        return processedBamFile
    }

    private static OverallQualityAssessment createOverallQualityAssessment(ProcessedBamFile processedBamFile, Long identifier) {
        assert processedBamFile: 'processedBamFile must not be null'

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass([
                processedBamFile: processedBamFile,
                identifier      : QualityAssessmentPass.nextIdentifier(processedBamFile),
        ])
        assert qualityAssessmentPass.save([flush: true])

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment(
                AbstractBamFileServiceIntegrationTests.ARBITRARY_QA_VALUES + [
                id                   : identifier,
                qualityAssessmentPass: qualityAssessmentPass,
        ])
        assert overallQualityAssessment.save([flush: true])

        return overallQualityAssessment
    }
}
