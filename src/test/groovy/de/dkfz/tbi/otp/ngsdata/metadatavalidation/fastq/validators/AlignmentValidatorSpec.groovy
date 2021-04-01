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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class AlignmentValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        [
                RoddyWorkflowConfig,
                CellRangerConfig,
                Project,
                SeqType,
                SampleIdentifier,
                Pipeline,
        ]
    }

    @Unroll
    void 'validate, when  column SEQUENCING_TYPE is missing, returns error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""""".replaceAll(',', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR, "Required column '${SEQUENCING_READ_TYPE}' is missing."),
                new Problem(Collections.emptySet(), Level.ERROR, "Required column '${SEQUENCING_TYPE}' is missing."),
        ]

        when:
        new AlignmentValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    @Unroll
    void 'validate, does not check for missing optional columns'() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${SEQUENCING_READ_TYPE}
${seqType.seqTypeName},${seqType.libraryLayout}
""".replaceAll(',', '\t'))

        when:
        new AlignmentValidator([metadataImportService: new MetadataImportService([seqTypeService: new SeqTypeService()])]).validate(context)

        then:
        containSame(context.problems, [])
    }

    @Unroll
    void 'validate, when seqType is not supported for alignment, returns info'() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        SeqType seqType = createSeqType()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE}
${seqType.name},${createProject().name},,DNA,${SequencingReadType.SINGLE}
""".replaceAll(',', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.INFO, "Alignment for SeqType ${seqType} is not supported"),
        ]

        when:
        new AlignmentValidator([metadataImportService: new MetadataImportService([seqTypeService: new SeqTypeService()])]).validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    @Unroll
    void 'validate, when alignment is not configured, return warning'() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        Pipeline pipeline = DomainFactory.createPanCanPipeline()
        SeqType seqType1 = DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType()
        SeqType seqType2 = DomainFactory.createCellRangerAlignableSeqTypes().first()
        Project project = createProject()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE}
${seqType1.name},${project.name},,DNA,${SequencingReadType.PAIRED}
${seqType2.name},${project.name},,${SeqType.SINGLE_CELL_DNA},${SequencingReadType.PAIRED}
""".replaceAll(',', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING, "${pipeline.name} is not configured for Project '${project}' and SeqType '${seqType1}'", "At least one Alignment is not configured."),
                new Problem(Collections.emptySet(), Level.WARNING, "CellRanger is not configured for Project '${project}' and SeqType '${seqType2}'", "At least one Alignment is not configured."),
        ]

        when:
        new AlignmentValidator([
                metadataImportService: new MetadataImportService([seqTypeService: new SeqTypeService()]),
                projectService: new ProjectService(),
        ]).validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    @Unroll
    void 'validate, when alignment is configured, no warnings or errors'() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        Pipeline pipeline = DomainFactory.createPanCanPipeline()
        SeqType seqType1 = DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType()
        SeqType seqType2 = DomainFactory.createCellRangerAlignableSeqTypes().first()
        Project project = createProject()
        DomainFactory.createRoddyWorkflowConfig(project: project, pipeline: pipeline, seqType: seqType1)
        DomainFactory.proxyCellRanger.createConfig(project: project)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE}
${seqType1.name},${project.name},,DNA,${SequencingReadType.PAIRED}
${seqType2.name},${project.name},,${SeqType.SINGLE_CELL_DNA},${SequencingReadType.PAIRED}
""".replaceAll(',', '\t'))

        when:
        new AlignmentValidator([
                metadataImportService: new MetadataImportService([seqTypeService: new SeqTypeService()]),
                projectService: new ProjectService(),
        ]).validate(context)

        then:
        context.problems.empty
    }
}
