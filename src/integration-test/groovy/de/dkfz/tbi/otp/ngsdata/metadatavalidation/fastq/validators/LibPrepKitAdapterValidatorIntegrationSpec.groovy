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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.*
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.MapUtilService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Integration
@Rollback
class LibPrepKitAdapterValidatorIntegrationSpec extends Specification implements RnaAlignmentWorkflowDomainFactory, PanCancerWorkflowDomainFactory, WgbsAlignmentWorkflowDomainFactory {

    void 'validate, when metadata file contains valid data, succeeds'() {
        given:
        WorkflowVersion workflowVersionWgs = createPanCancerWorkflowVersion()
        WorkflowVersion workflowVersionRna = createRnaAlignmentVersion()
        WorkflowVersion workflowVersionWgbs = createWgbsAlignmenWorkflowVersion()

        LibPrepKitAdapterValidator validator = new LibPrepKitAdapterValidator()
        validator.libraryPreparationKitService = new LibraryPreparationKitService()
        validator.validatorHelperService = new ValidatorHelperService()
        validator.seqTypeService = Mock(SeqTypeService) {
            findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomePairedSeqType
            findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> SeqTypeService.exomePairedSeqType
            findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> SeqTypeService.rnaPairedSeqType
            findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> SeqTypeService.chipSeqPairedSeqType
            findByNameOrImportAlias('WHOLE_UNKNOWN_SEQUENCING', [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> null
        }
        validator.workflowVersionSelectorService = new WorkflowVersionSelectorService()
        validator.workflowService = new WorkflowService(workflowVersionService: new WorkflowVersionService())
        validator.workflowService.applicationContext = Mock(ApplicationContext) {
            _ * getBeansOfType(OtpWorkflow) >> [
                    "rnaAlignmentWorkflow": new RnaAlignmentWorkflow(),
                    "panCancerWorkflow"   : new PanCancerWorkflow(),
                    "wgbsWorkflow"        : new WgbsWorkflow(),
                    "bamImportWorkflow"   : new BamImportWorkflow(),
            ]
            0 * _
        }
        validator.configSelectorService = new ConfigSelectorService()
        validator.configFragmentService = new ConfigFragmentService(mapUtilService: new MapUtilService())
        validator.roddyConfigValueService = new RoddyConfigValueService()
        validator.speciesWithStrainService = new SpeciesWithStrainService()
        validator.referenceGenomeSelectorService = new ReferenceGenomeSelectorService()

        LibraryPreparationKit kitWithoutAdapterFileAndSequence = createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file_and_sequence')
        LibraryPreparationKit kitWithoutAdapterFile = createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_file', reverseComplementAdapterSequence: "ACGTC")
        LibraryPreparationKit kitWithoutAdapterSequence = createLibraryPreparationKit(name: 'lib_prep_kit_without_adapter_sequence', adapterFile: "/asdf")

        Project wgsProject = createProject()
        Project wgbsProject = createProject()
        Project rnaProject = createProject()
        Project noWorkflowSelectorProject = createProject()
        Project noReferenceGenomeSelectorProject = createProject()

        createWorkflowVersionSelector(project: wgsProject, seqType: DomainFactory.createWholeGenomeSeqType(), workflowVersion: workflowVersionWgs)
        createWorkflowVersionSelector(project: wgbsProject, seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(), workflowVersion: workflowVersionWgbs)
        createWorkflowVersionSelector(project: rnaProject, seqType: DomainFactory.createRnaPairedSeqType(), workflowVersion: workflowVersionRna)
        createWorkflowVersionSelector(project: noReferenceGenomeSelectorProject, seqType: DomainFactory.createRnaPairedSeqType(), workflowVersion: workflowVersionRna)
        createReferenceGenome(species: [findOrCreateMouseSpecies().species], speciesWithStrain: null)
        ReferenceGenome human = createReferenceGenome(species: [findOrCreateHumanSpecies().species], speciesWithStrain: null)
        ReferenceGenome humanWithMouse = createReferenceGenome(species: [findOrCreateHumanSpecies().species], speciesWithStrain: [findOrCreateMouseSpecies()])
        createReferenceGenomeSelector(
                project: wgsProject,
                seqType: DomainFactory.createWholeGenomeSeqType(),
                referenceGenome: human,
                workflow: workflowVersionWgs.workflow,
        )
        createReferenceGenomeSelector(
                project: wgbsProject,
                seqType: DomainFactory.createWholeGenomeBisulfiteSeqType(),
                referenceGenome: human,
                workflow: workflowVersionWgbs.workflow,
        )
        createReferenceGenomeSelector(
                project: rnaProject,
                seqType: DomainFactory.createRnaPairedSeqType(),
                referenceGenome: human,
                workflow: workflowVersionRna.workflow,
        )
        createReferenceGenomeSelector(
                project: rnaProject,
                seqType: DomainFactory.createRnaPairedSeqType(),
                referenceGenome: humanWithMouse,
                workflow: workflowVersionRna.workflow,
        )
        createReferenceGenomeSelector(
                project: noWorkflowSelectorProject,
                seqType: DomainFactory.createRnaPairedSeqType(),
                referenceGenome: human,
                workflow: workflowVersionRna.workflow,
        )

        // default is false
        createExternalWorkflowConfigSelector(getEWCSProperties(false) + [projects: null])
        // true for wgbs Project
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE.name()}\t${LIB_PREP_KIT.name()}\t${PROJECT.name()}\t${SEQUENCING_READ_TYPE.name()}\t${SPECIES.name()}\n" +
                        // No trimming no Problems
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${wgsProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterFile.name}\t${wgsProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${wgsProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        // WGBS with Trimming, Problems when Adapter File is missing
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${wgbsProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterFile.name}\t${wgbsProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${wgbsProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        // RNA is always with Trimming, Problems when Adapter Sequence is missing
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFile.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        // RNA with Mixed-in-Species
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN+MOUSE\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFile.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN+MOUSE\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN+MOUSE\n" +
                        // wrong seqtype
                        "${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "unknown\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        // wrong libprepkit
                        "${SeqTypeNames.RNA.seqTypeName}\tunknown\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        // wrong project
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\tunknown\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        // wrong sequencingReadType
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\tunknown\tHUMAN\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t\tHUMAN\n" +
                        // wrong species
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tMOUSE\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tHUMAN+unknown\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\tunknown\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${rnaProject.name}\t${SequencingReadType.PAIRED}\t\n" +
                        // missing selectors
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${noWorkflowSelectorProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n" +
                        "${SeqTypeNames.RNA.seqTypeName}\t${kitWithoutAdapterFileAndSequence.name}\t${noReferenceGenomeSelectorProject.name}\t${SequencingReadType.PAIRED}\tHUMAN\n"
        )
        createExternalWorkflowConfigSelector(getEWCSProperties(true) + [projects: [wgbsProject]])

        when:
        validator.validate(context)

        then:

        Collection<Problem> expectedProblems = [
                createKitWithoutAdapterFileProblem(context, 3, kitWithoutAdapterFileAndSequence),
                createKitWithoutAdapterFileProblem(context, 4, kitWithoutAdapterFile),
                createKitWithoutAdapterSequenceProblem(context, 6, kitWithoutAdapterFileAndSequence),
                createKitWithoutAdapterSequenceProblem(context, 8, kitWithoutAdapterSequence),
                createKitWithoutAdapterSequenceProblem(context, 9, kitWithoutAdapterFileAndSequence),
                createKitWithoutAdapterSequenceProblem(context, 11, kitWithoutAdapterSequence),
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    private Problem createKitWithoutAdapterFileProblem(MetadataValidationContext context, int row, LibraryPreparationKit kit) {
        return new Problem((context.spreadsheet.dataRows[row].cells) as Set, LogLevel.WARNING,
                "Adapter trimming is requested but adapter file for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the adapter file for at least one library preparation kit is missing.")
    }

    private Problem createKitWithoutAdapterSequenceProblem(MetadataValidationContext context, int row, LibraryPreparationKit kit) {
        return new Problem((context.spreadsheet.dataRows[row].cells) as Set, LogLevel.WARNING,
                "Adapter trimming is requested but reverse complement adapter sequence for library preparation kit '${kit}' is missing.", "Adapter trimming is requested but the reverse complement adapter sequence for at least one library preparation kit is missing.")
    }

    private Map getEWCSProperties(boolean trimming) {
        return [
                workflowVersions              : WorkflowVersion.all,
                workflows                     : Workflow.all,
                libraryPreparationKits        : LibraryPreparationKit.all,
                seqTypes                      : SeqType.all,
                referenceGenomes              : ReferenceGenome.all,
                externalWorkflowConfigFragment: createExternalWorkflowConfigFragment(configValues: """
                    {
                        "RODDY": {
                            "cvalues": {
                                "useAdaptorTrimming": {
                                    "value": "${trimming}"
                                }
                            }
                        }
                    }
                """),
        ]
    }
}
