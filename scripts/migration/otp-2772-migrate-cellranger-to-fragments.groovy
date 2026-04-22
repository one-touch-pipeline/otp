/*
 * Copyright 2011-2026 The OTP authors
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

/**
 * Migrate the current Cell Ranger configs to the new fragment-based workflow system.
 * This will create WorkflowVersionSelectors and ReferenceGenomeSelectors for all existing CellRangerConfigs,
 * and set the default seqTypes and reference genomes for the workflow versions.
 *
 * Note: enforcedCells and expectedCells are not migrated, as they are not used in the new workflow system.
 */

import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

// input
String newWorkflowName = CellRangerWorkflow.WORKFLOW
List<SeqType> seqTypes = [] // Will be populated from existing configs

/** Variable to enable or disable a dry run, that will cause no changes **/
Boolean dryRun = true

// script
MergingCriteriaService mergingCriteriaService = ctx.mergingCriteriaService
WorkflowVersionService workflowVersionService = ctx.workflowVersionService

Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByName(newWorkflowName))
List<WorkflowVersion> workflowVersions = workflowVersionService.findAllByWorkflow(workflow)
List<ReferenceGenome> allUsedReferenceGenomes = []

List<CellRangerConfig> activeConfigs = CellRangerConfig.findAllByObsoleteDateIsNull()

// Get the CellRanger configs that contain versions not supported by the new system.
// Stop running the script if there are any versions not being able to be supported.
List<CellRangerConfig> nonMigratableConfigs = activeConfigs.findAll {
    it.programVersion && it.programVersion !in workflowVersions*.workflowVersion
}

if (nonMigratableConfigs.size()) {
    println("Following ${nonMigratableConfigs.size()} Cell Ranger configurations cannot be migrated to the new workflow system:")
    nonMigratableConfigs.each {
        println("  ${it.project} ${it.seqType} with version ${it.programVersion}")
    }
    println("Please fix the inconsistent Cell Ranger configurations before running this script again")
    // Stop the script here, as we cannot be sure about the correct WorkflowVersionSelector for the migratable configs
    // if there are non-migratable configs with versions that are not supported by the new system
    return
}

seqTypes = activeConfigs.collect { it.seqType }.unique()

println("# ${activeConfigs.size()} CellRangerConfig(s) found for migration.")

println("## Creating WorkflowVersionSelectors for the new workflow system:")
WorkflowVersionSelector.withTransaction {
    // Create WorkflowVersionSelectors from existing CellRangerConfigs
    activeConfigs
            .groupBy {
                [
                        it.project,
                        it.seqType,
                        it.programVersion
                ]
            }.each { _, List<CellRangerConfig> configs ->
        CellRangerConfig config = configs.first()

        String version = config.programVersion
        //E.g. CellRanger/8.0.1
        WorkflowVersion workflowVersion = workflowVersions.find { it.workflowVersion == version }
        // We have filtered the CellRangerConfigs by workflowVersion, so this should never be null
        if (!workflowVersion) {
            throw new RuntimeException("Workflow version '${version}' for '${config.project} ${config.seqType}' could not be found")
        }

        new WorkflowVersionSelector(
                project: config.project,
                seqType: config.seqType,
                workflowVersion: workflowVersion,
        ).save(flush: true)
        println("  WorkflowVersionSelector of version ${workflowVersion.workflowVersion} created for ${config.project} and ${config.seqType}")
    }

    println("## Creating ReferenceGenomeSelectors for the new workflow system:")
    // Create ReferenceGenomeSelectors from CellRangerConfig objects
    activeConfigs.findAll { it.referenceGenomeIndex?.referenceGenome }
            .groupBy {
                [
                        it.project,
                        it.seqType,
                        it.referenceGenomeIndex.referenceGenome.species,
                        it.referenceGenomeIndex.referenceGenome.speciesWithStrain,
                ]
            }.each { _, List<CellRangerConfig> configs ->

        CellRangerConfig config = configs.first()
        ReferenceGenome referenceGenome = config.referenceGenomeIndex.referenceGenome

        if (configs*.referenceGenomeIndex*.referenceGenome.unique().size() > 1) {
            throw new RuntimeException("Multiple reference genomes found for '${config.project} ${config.seqType} ${referenceGenome.speciesWithStrain.join("+")}'")
        }

        println "  Creating ReferenceGenomeSelector for ${config.project} ${config.seqType} ${referenceGenome.species.join("+")} ${referenceGenome.speciesWithStrain.join("+")}"

        Set<SpeciesWithStrain> speciesWithStrainsFromProject = config.project.speciesWithStrains
        println speciesWithStrainsFromProject

        //we have no reference genome with more than one species, therefore limit migration to handle only that case
        SpeciesWithStrain speciesWithStrain = CollectionUtils.atMostOneElement(referenceGenome.speciesWithStrain)

        //we have no reference genome with more than one species, therefore limit migration to handle only that case
        Species species = CollectionUtils.atMostOneElement(referenceGenome.species)

        List<List<SpeciesWithStrain>> speciesWithStrainsList = []
        if (species) {
            speciesWithStrainsList.addAll(SpeciesWithStrain.findAllBySpecies(species).findAll {
                it in speciesWithStrainsFromProject
            }.collect {
                [it]
            })
            if (speciesWithStrain) {
                speciesWithStrainsList.each {
                    it.add(speciesWithStrain)
                }
            }
        } else {
            speciesWithStrainsList.add([speciesWithStrain])
        }
        speciesWithStrainsList.each { List<SpeciesWithStrain> speciesWithStrainList ->
            new ReferenceGenomeSelector(
                    project: config.project,
                    seqType: config.seqType,
                    species: speciesWithStrainList as Set,
                    workflow: workflow,
                    referenceGenome: referenceGenome,
            ).save(flush: true)
            println("    ReferenceGenomeSelector of ${referenceGenome} for ${config.project}, ${config.seqType} ${speciesWithStrainList.join(", ")} created")
        }

        allUsedReferenceGenomes.add(referenceGenome)
    }

    workflow.defaultSeqTypesForWorkflowVersions = seqTypes as Set
    workflow.defaultReferenceGenomesForWorkflowVersions = allUsedReferenceGenomes as Set
    workflow.save(flush: true)

    workflowVersionService.findAllByWorkflow(workflow).collect { wv ->
        wv.supportedSeqTypes = seqTypes as Set
        wv.allowedReferenceGenomes = allUsedReferenceGenomes as Set
        wv.save(flush: true)
    }

    seqTypes.each {
        mergingCriteriaService.createDefaultMergingCriteria(it)
    }

    assert !dryRun: "This is a dry run, w/o modification of database."
}
[]
