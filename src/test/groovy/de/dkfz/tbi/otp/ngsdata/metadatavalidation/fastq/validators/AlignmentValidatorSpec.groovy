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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class AlignmentValidatorSpec extends Specification implements DataTest, DomainFactoryCore, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerConfig,
                Pipeline,
                Project,
                RoddyWorkflowConfig,
                SeqType,
                SampleIdentifier,
                WorkflowVersionSelector,
                ReferenceGenomeSelector,
        ]
    }

    private SeqType seqTypeNotAlignable
    private SeqType seqTypeOldSystem
    private SeqType seqTypeNewSystem
    private SeqType seqTypeCellRanger

    private SeqTypeService mockSeqTypeService
    private AlignmentValidator validator

    private void setupData(int count = 1) {
        seqTypeNotAlignable = createSeqTypePaired()
        seqTypeOldSystem = DomainFactory.createRnaPairedSeqType()
        seqTypeNewSystem = createSeqTypePaired()
        seqTypeCellRanger = DomainFactory.createCellRangerAlignableSeqTypes().first()
        SpeciesWithStrain speciesWithStrainMouse = findOrCreateMouseSpecies()
        SpeciesWithStrain speciesWithStrainHuman = findOrCreateHumanSpecies()
        SpeciesWithStrainService speciesWithStrainService
        speciesWithStrainService = Mock(SpeciesWithStrainService) {
            _ * it.getByAlias("mouse") >> speciesWithStrainMouse
            _ * it.getByAlias("human") >> speciesWithStrainHuman
        }
        mockSeqTypeService = Mock(SeqTypeService) {
            count * findAlignAbleSeqTypes() >> [
                    seqTypeOldSystem,
                    seqTypeNewSystem,
            ]
            count * seqTypesNewWorkflowSystem >> [
                    seqTypeNewSystem,
            ]
            _ * findByNameOrImportAlias(_, _) >> { String nameOrImportAlias, Map properties ->
                return CollectionUtils.exactlyOneElement(SeqType.findAllByName(nameOrImportAlias), "Not found: ${nameOrImportAlias}")
            }
            0 * _
        }
        validator = new AlignmentValidator([
                validatorHelperService        : new ValidatorHelperService([seqTypeService: mockSeqTypeService, speciesWithStrainService: speciesWithStrainService]),
                seqTypeService                : mockSeqTypeService,
                projectService                : new ProjectService(),
                workflowVersionSelectorService: Mock(WorkflowVersionSelectorService) {
                    0 * _
                },
                referenceGenomeSelectorService: Mock(ReferenceGenomeSelectorService) {
                    _ * _
                },
        ])
    }

    @Unroll
    void 'validate, when column SEQUENCING_TYPE is missing, returns error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""""".replaceAll(',', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR, "Required column '${SEQUENCING_READ_TYPE}' is missing."),
                new Problem(Collections.emptySet(), LogLevel.ERROR, "Required column '${SEQUENCING_TYPE}' is missing."),
        ]

        when:
        new AlignmentValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, does not check for missing optional columns'() {
        given:
        setupData()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${SEQUENCING_READ_TYPE}
${seqTypeNotAlignable.name},${seqTypeNotAlignable.libraryLayout}
""".replaceAll(',', '\t'))

        when:
        validator.validate(context)

        then:
        TestCase.assertContainSame(context.problems, [])
    }

    void 'validate, when seqType is not supported for alignment, returns info'() {
        given:
        setupData()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE},${SPECIES}
${seqTypeNotAlignable.name},${createProject().name},,DNA,${seqTypeNotAlignable.libraryLayout},mouse
""".replaceAll(',', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.INFO, "Alignment for SeqType ${seqTypeNotAlignable} is not supported"),
        ]

        when:
        validator.validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when alignment is not configured, return warning'() {
        given:
        setupData()
        DomainFactory.createPanCanPipeline()
        Pipeline pipeline = DomainFactory.createRnaPipeline()
        Project project = createProject()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE},${SPECIES}
${seqTypeOldSystem.name},${project.name},,DNA,${seqTypeOldSystem.libraryLayout},mouse
${seqTypeCellRanger.name},${project.name},,${SeqType.SINGLE_CELL_DNA},${seqTypeCellRanger.libraryLayout},mouse
${seqTypeNewSystem.name},${project.name},,DNA,${seqTypeNewSystem.libraryLayout},mouse
""".replaceAll(',', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING, "${pipeline.name} is not configured for Project '${project}' and SeqType '${seqTypeOldSystem}'", "At least one Alignment or Reference Genome is not configured."),
                new Problem(Collections.emptySet(), LogLevel.WARNING, "CellRanger is not configured for Project '${project}' and SeqType '${seqTypeCellRanger}'", "At least one Alignment or Reference Genome is not configured."),
                new Problem(Collections.emptySet(), LogLevel.WARNING, "Alignment is not configured for Project '${project}' and SeqType '${seqTypeNewSystem}'", "At least one Alignment or Reference Genome is not configured."),
        ]

        when:
        validator.validate(context)

        then:
        validator.workflowVersionSelectorService.hasAlignmentConfigForProjectAndSeqType(project, seqTypeNewSystem) >> false
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when alignment is configured but reference genome is not configured, return warning'() {
        given:
        setupData()
        List<SpeciesWithStrain> speciesWithStrainList = [findOrCreateHumanSpecies()]
        Project project = createProject()
        createWorkflow()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE},${SPECIES}
${seqTypeNewSystem.name},${project.name},,DNA,${seqTypeNewSystem.libraryLayout},human
""".replaceAll(',', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING,
                        "Reference Genome is not configured for Project '${project}', SeqType '${seqTypeNewSystem}' and Species '${speciesWithStrainList.join(' + ')}'",
                        "At least one Alignment or Reference Genome is not configured."),
        ]

        when:
        validator.validate(context)

        then:
        validator.workflowVersionSelectorService.hasAlignmentConfigForProjectAndSeqType(project, seqTypeNewSystem) >> true
        validator.referenceGenomeSelectorService.hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies(project, seqTypeNewSystem, speciesWithStrainList) >> false
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when alignment is configured and reference genome is also configured, returns no warning'() {
        given:
        setupData()
        List<SpeciesWithStrain> speciesWithStrainList = [findOrCreateHumanSpecies()]
        Project project = createProject()
        SeqType seqType = createSeqTypePaired()
        ReferenceGenome referenceGenome = createReferenceGenome([species: [] as Set, speciesWithStrain: speciesWithStrainList as Set])
        Workflow workflow = createWorkflow()
        createReferenceGenomeSelector([
                project        : project,
                seqType        : seqType,
                referenceGenome: referenceGenome,
                workflow       : workflow,
        ])
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE},${SPECIES}
${seqTypeNewSystem.name},${project.name},,DNA,${seqTypeNewSystem.libraryLayout},human
""".replaceAll(',', '\t'))

        when:
        validator.validate(context)

        then:
        validator.workflowVersionSelectorService.hasAlignmentConfigForProjectAndSeqType(project, seqTypeNewSystem) >> true
        validator.referenceGenomeSelectorService.hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies(project, seqTypeNewSystem, speciesWithStrainList) >> true
        context.problems.empty
    }

    void 'validate, when alignment is configured, no warnings or errors'() {
        given:
        setupData()
        Pipeline pipeline = DomainFactory.createRnaPipeline()
        Project project = createProject()
        DomainFactory.createRoddyWorkflowConfig(project: project, pipeline: pipeline, seqType: seqTypeOldSystem)
        DomainFactory.proxyCellRanger.createConfig(project: project, seqType: seqTypeCellRanger)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${PROJECT},${SAMPLE_NAME},${BASE_MATERIAL},${SEQUENCING_READ_TYPE}
${seqTypeOldSystem.name},${project.name},,DNA,${seqTypeOldSystem.libraryLayout}
${seqTypeCellRanger.name},${project.name},,${SeqType.SINGLE_CELL_DNA},${seqTypeCellRanger.libraryLayout}
${seqTypeNewSystem.name},${project.name},,DNA,${seqTypeNewSystem.libraryLayout}
""".replaceAll(',', '\t'))

        when:
        validator.validate(context)

        then:
        validator.workflowVersionSelectorService.hasAlignmentConfigForProjectAndSeqType(project, seqTypeNewSystem) >> true
        context.problems.empty
    }
}
