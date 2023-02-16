/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Transactional
class ReferenceGenomeSelectorService {

    OtpWorkflowService otpWorkflowService

    Errors createOrUpdate(Project project, SeqType seqType, Set<SpeciesWithStrain> species, Workflow workflow, ReferenceGenome referenceGenome) {
        ReferenceGenomeSelector existing = atMostOneElement(ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(project, seqType, workflow)
                .findAll { it.species == species })
        try {
            if (existing) {
                if (referenceGenome) {
                    existing.referenceGenome = referenceGenome
                    existing.save(flush: true)
                } else {
                    existing.delete(flush: true)
                }
            } else if (referenceGenome) {
                new ReferenceGenomeSelector(
                        project: project,
                        seqType: seqType,
                        species: species,
                        workflow: workflow,
                        referenceGenome: referenceGenome,
                ).save(flush: true)
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    Map<Set<SpeciesWithStrain>, List<ReferenceGenome>> getMappingOfSpeciesCombinationsToReferenceGenomes(Project project) {
        Set<SpeciesWithStrain> projectSpecies = project.speciesWithStrains
        List<ReferenceGenome> referenceGenomes = ReferenceGenome.all

        Map<Set<SpeciesWithStrain>, List<ReferenceGenome>> result = [:]
        referenceGenomes.each { referenceGenome ->
            if (referenceGenome.speciesWithStrain.any { it in projectSpecies } || referenceGenome.species.any { it in projectSpecies*.species }) {
                List<Collection<SpeciesWithStrain>> speciesReplacements = referenceGenome.species.collect {
                    Species s -> projectSpecies.findAll { SpeciesWithStrain sws -> sws.species == s } ?: SpeciesWithStrain.findAllBySpecies(s)
                }
                Set<Set<SpeciesWithStrain>> speciesWithStrainSets = speciesToSpeciesWithStrain(speciesReplacements, referenceGenome.speciesWithStrain)
                speciesWithStrainSets.each { speciesWithStrain ->
                    if (result[speciesWithStrain]) {
                        result[speciesWithStrain].add(referenceGenome)
                    } else {
                        result[speciesWithStrain] = [referenceGenome]
                    }
                }
            }
        }
        return result
    }

    private Set<Set<SpeciesWithStrain>> speciesToSpeciesWithStrain(List<Collection<SpeciesWithStrain>> species,
                                                                   Set<SpeciesWithStrain> speciesWithStrain = [] as Set) {
        Set<Set<SpeciesWithStrain>> result = [] as Set
        if (species) {
            while (species) {
                Collection<SpeciesWithStrain> sp = species.remove(0)
                sp.each {
                    result.addAll(speciesToSpeciesWithStrain(species, speciesWithStrain + [it]))
                }
            }
        } else {
            result.add(speciesWithStrain)
        }
        return result
    }

    boolean hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies(Project project, SeqType seqType, List<SpeciesWithStrain> speciesWithStrains) {
        if (speciesWithStrains.empty) {
            return false
        }
        Set<String> otpWorkflows = otpWorkflowService.lookupAlignableOtpWorkflowBeans().keySet()
        Set<SpeciesWithStrain> speciesWithStrainSet = speciesWithStrains as Set
        List<Workflow> workflows = Workflow.findAllByBeanNameInList(otpWorkflows as List)
        ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflowInList(project, seqType, workflows).
                findAll { ReferenceGenomeSelector rgs ->
                    rgs.species == speciesWithStrainSet
                }
    }
}
