/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame

@Rollback
@Integration
class ProjectRunNameFileNameValidatorIntegrationSpec extends Specification {

    static final String PROJECT = 'project'
    static final String RUN_ID = "runName"
    static final String SAMPLE_NAME = "sampleIdentifierName"
    static final String DATAFILE = "DataFileFileName.gz"
    static final String DATAFILE_PATH = "/tmp/DataFileFileName.gz"
    static final String DATAFILE_NEW = "DataFileFileNameNew.gz"

    static RawSequenceFile rawSequenceFile

    static final String VALID_METADATA =
            "${MetaDataColumn.FASTQ_FILE}\t${MetaDataColumn.RUN_ID}\t${MetaDataColumn.PROJECT}\n" +
                    "${DATAFILE}\t${RUN_ID}\t${PROJECT}\n"

    void setupData() {
        Run run = DomainFactory.createRun(["name": RUN_ID])
        Project project = DomainFactory.createProject(["name": PROJECT])
        Individual individual = DomainFactory.createIndividual(["project": project])
        Sample sample = DomainFactory.createSample(["individual": individual])
        SeqTrack seq = DomainFactory.createSeqTrack(["run": run, "sample": sample])
        rawSequenceFile = DomainFactory.createFastqFile(["fileName": DATAFILE, "seqTrack": seq])
    }

    void 'validate, when file name does not exist for specified run and project (not using parseSampleIdentifier)'() {
        given:
        setupData()
        DomainFactory.createSampleIdentifier(["name": SAMPLE_NAME, "sample": rawSequenceFile.seqTrack.sample]).name

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${MetaDataColumn.RUN_ID}\t${MetaDataColumn.PROJECT}\n" +
                        "${DATAFILE_NEW}\t${RUN_ID}\t${PROJECT}\t\n"
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()

        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when file name already exists for specified run and project (using parseSampleIdentifier)'() {
        given:
        setupData()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()

        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set,
                        LogLevel.ERROR, "A file with name '${DATAFILE}' already exists for run '${RUN_ID}' and project '${PROJECT}'", "At least one project, run and file combination already exists in OTP")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when file name already exists for specified run and project (not using parseSampleIdentifier)'() {
        given:
        setupData()
        DomainFactory.createSampleIdentifier(["name": PROJECT, "sample": rawSequenceFile.seqTrack.sample]).name

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()

        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set,
                        LogLevel.ERROR, "A file with name '${DATAFILE}' already exists for run '${RUN_ID}' and project '${PROJECT}'", "At least one project, run and file combination already exists in OTP")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when file name already exists for specified run and project while using absolute path'() {
        given:
        setupData()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${MetaDataColumn.RUN_ID}\t${MetaDataColumn.PROJECT}\n" +
                        "${DATAFILE_PATH}\t${RUN_ID}\t${PROJECT}\n"
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()

        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set,
                        LogLevel.ERROR, "A file with name '${DATAFILE}' already exists for run '${RUN_ID}' and project '${PROJECT}'", "At least one project, run and file combination already exists in OTP")
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}
