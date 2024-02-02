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

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SpeciesValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    ValidatorHelperService validatorHelperService

    @Autowired
    SpeciesWithStrainService speciesWithStrainService

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Autowired
    SampleService sampleService

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return [
                "The column ${SPECIES} must exist.",
                "The field ${SPECIES} must not be empty.",
                "Each value of the + separated list in field ${SPECIES} has to be known to OTP.",
                "All entries of a PID must have the same main species.",
                "If the PID already exists, the main species has to match.",
                "All entries of a sample must have the same mixed-in species.",
                "If the sample already exists, the mixed-in species have to match.",
                "The main species must match the species set for the project.",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SPECIES]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [PROJECT, SAMPLE_NAME]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @CompileDynamic
    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        Set<Cell> emptyCells = [] as Set
        Map<String, Set<Cell>> unknownCells = [:]

        Collection<ValueTuple> valueTuplesValidSpecies = valueTuples.findAll { ValueTuple valueTuple ->
            String speciesValue = valueTuple.getValue(SPECIES.name()).trim()
            if (speciesValue) {
                List<Boolean> speciesAreKnown = speciesValue.split(/\+/).collect {
                    if (!speciesWithStrainService.getByAlias(it.trim())) {
                        if (unknownCells.containsKey(it)) {
                            unknownCells[it].addAll(valueTuple.cells)
                        } else {
                            unknownCells[it] = new HashSet<Cell>(valueTuple.cells)
                        }
                        return false
                    }
                    return true
                }
                return speciesAreKnown.every()
            }
            emptyCells.addAll(valueTuple.cells)
            return false
        }

        if (emptyCells) {
            context.addProblem(emptyCells, LogLevel.ERROR, "The field '${SPECIES}' must not be empty.")
        }
        unknownCells.each { String speciesName, Set<Cell> cells ->
            context.addProblem(cells, LogLevel.ERROR,
                    "The species '${speciesName}' is not known to OTP.",
                    "Each value of the + separated list in field '${SPECIES}' has to be known to OTP.")
        }

        List<Value> values = valueTuplesValidSpecies.collect {
            List<String> speciesNames = it.getValue(SPECIES.name()).trim().split(/\+/) as List
            SpeciesWithStrain species = speciesWithStrainService.getByAlias(speciesNames.remove(0).trim())
            Set<SpeciesWithStrain> mixedInSpecies = speciesNames.collect { speciesWithStrainService.getByAlias(it.trim()) } as Set

            String sampleType = validatorHelperService.getSampleType(it)
            String pid = validatorHelperService.getPid(it)
            Sample sample = validatorHelperService.findExistingSampleForValueTuple(it)
            Individual individual = CollectionUtils.atMostOneElement(Individual.findAllByPid(pid))

            Project project = ProjectService.findByNameOrNameInMetadataFiles(it.getValue(PROJECT.name()))

            new Value(
                    sampleType,
                    sample,
                    pid,
                    individual,
                    species,
                    mixedInSpecies,
                    project,
                    it,
            )
        }

        values.groupBy { it.pid }.each { String pid, List<Value> value ->
            if (pid && value*.species.unique().size() > 1) {
                context.addProblem(value.collectMany { it.valueTuple.cells } as Set, LogLevel.ERROR,
                        "PID '${pid}' has distinct species ${value*.species.collect { "'${it}'" }.join(", ")}.",
                        "All entries of a PID must have the same main species.")
            }
        }
        values.groupBy { it.individual }.each { Individual individual, List<Value> value ->
            List<SeqTrack> seqTracksWithIndividual = sampleService.getSamplesByIndividual(individual).collectMany { it.seqTracks ?: [] } as List<SeqTrack>
            if (individual && value.species.unique().size() == 1 &&
                    (value*.species.first() != individual?.species) && (seqTracksWithIndividual || individual.species)) {
                context.addProblem(value.collectMany { it.valueTuple.cells } as Set, LogLevel.ERROR,
                        "Species '${value*.species.first()}' for PID '${individual}' doesn't match with existing PID with species '${individual.species}'.",
                        "If the PID already exists, the main species has to match."
                )
            }
        }

        values.groupBy { [it.pid, it.sampleType] }.each { List<String> sample, List<Value> value ->
            if (sample && value*.mixedInSpecies.unique().size() > 1) {
                context.addProblem(value.collectMany { it.valueTuple.cells } as Set, LogLevel.ERROR,
                        "Sample '${sample.join(" ")}' has distinct mixed-in species ${value*.mixedInSpecies.collect { "'${it.join(",")}'" }.join(", ")}.",
                        "All entries of a sample must have the same mixed-in species."
                )
            }
        }
        values.groupBy { it.sample }.each { Sample sample, List<Value> value ->
            if (sample && value*.mixedInSpecies.unique().size() == 1 &&
                    (value*.mixedInSpecies.first() != (sample.mixedInSpecies ?: [] as Set)) &&
                    !(!sample.seqTracks && !sample.mixedInSpecies)) {
                String message = ""
                if (value*.mixedInSpecies.first()) {
                    message += "Mixed-in species '${value*.mixedInSpecies.first().join(",")}'"
                } else {
                    message += "No mixed-in species"
                }
                message += " for sample '${sample}' doesn't match with existing sample "

                if (sample.mixedInSpecies) {
                    message += "with mixed-in species '${(sample.mixedInSpecies ?: []).join(",")}'."
                } else {
                    message += "without mixed-in species."
                }
                context.addProblem(value.collectMany { it.valueTuple.cells } as Set, LogLevel.ERROR,
                        message,
                        "If the sample already exists, the mixed-in species have to match."
                )
            }
        }

        values.groupBy { [it.species, it.project] }.each { List list, List<Value> value ->
            SpeciesWithStrain species = list.first()
            Project project = list.last()

            if (project && species && !(species in project.speciesWithStrains)) {
                String message = project.speciesWithStrains ?
                        "The main species '${species}' doesn't match the species '${(project.speciesWithStrains).join(", ")}' " +
                                "set for the project '${project}'." :
                        "No allowed species are set for project '${project}'."
                context.addProblem(value.collectMany { it.valueTuple.cells } as Set, LogLevel.ERROR,
                        message,
                        "The main species must match the species set for the project."
                )
            }
        }
    }
}

@Canonical
class Value {
    String sampleType
    Sample sample
    String pid
    Individual individual
    SpeciesWithStrain species
    Set<SpeciesWithStrain> mixedInSpecies
    Project project
    ValueTuple valueTuple
}
