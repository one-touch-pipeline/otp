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
package migration

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.TransactionUtils

import static groovyx.gpars.GParsPool.withPool

/**
 * Migration script to set the species for Individual and Sample
 * this script iterates over :
 * - all the Projects and sets the species for corresponding Individuals and Samples given in the project
 *
 * creates a mismatch overview when it does not find corresponding species from the Project's species
 * creates an overview when there are conflicts
 *
 * If no projects with exactly one SpeciesWithStrain exist, a very confusing error is thrown: org.hibernate.exception.SQLGrammarException: could not extract ResultSet
 */

// input area
class SpeciesMigrationByProject {

    /**
     * Blacklist of comma separated pids from the first migration script (otp-1288-species-migration-heidelberg-1-ReferenceGenome.groovy)
     */
    String individualsWithProblemInMigration1 = ""

    /**
     * Blacklist of comma separated pids from the second migration script (otp-1288-species-migration-heidelberg-2-metadata.groovy)
     *
     * It is used as a second parameter to avoid combining both list manually.
     */
    String individualsWithProblemInMigration2 = ""

    /**
     * Specifies how many projects are to be processed together in one batch
     *
     * Recommended:  numOfProjects / numLogicalCPUCores / n (some integer)
     */
    int batchSize = 100

    /**
     * Run this script w/o modification of database if set to true
     */
    boolean dryRun = true

    // script area
    List<Project> projects = Project.list().findAll {
        it.speciesWithStrains.size() == 1
    }

    Set<Long> individualIds = Individual.withCriteria {
        projections {
            isNull('species')
            property('id')
            'in'('project', projects)
            if (individualsWithProblemInMigration1) {
                not {
                    'in'('pid', individualsWithProblemInMigration1.split(',')*.trim())
                }
            }
            if (individualsWithProblemInMigration2) {
                not {
                    'in'('pid', individualsWithProblemInMigration2.split(',')*.trim())
                }
            }
        }
    } as Set

    int numIndividualIdsSize = individualIds.size()

    int numCores = Runtime.runtime.availableProcessors()

    long numBatches = Math.ceil(numIndividualIdsSize / batchSize) as long

    List<List<Long>> listOfIndividualIdLists = individualIds.collate(batchSize)

    void info() {
        println "There are ${numIndividualIdsSize} individuals without species"
        println "${numCores} logical CPU core(s) are available"
        println "${numBatches} batches will be processed"
    }

    void check() {
        assert batchSize > 1
    }

    void updateSpecies(List<Individual> individuals) {
        individuals.each { Individual individual ->
            assert individual.project.speciesWithStrains.size() == 1
            individual.species = individual.project.speciesWithStrains[0]
            individual.save(flush: false)
        }
    }

    void execute() {
        info()
        check()
        withPool(numCores, {
            // loop through each batch and process it
            listOfIndividualIdLists.makeConcurrent().each { List<Long> individualIdsBatch ->
                TransactionUtils.withNewTransaction { session ->
                    List<Individual> individuals = Individual.findAllByIdInList(individualIdsBatch)
                    updateSpecies(individuals)
                    // flush changes to the database
                    if (!dryRun) {
                        session.flush()
                    }
                    print('.')
                }
            }
        })
        printResult()
    }

    void printResult() {
        println " finished\n"
        if (dryRun) {
            println "\n\nNothing was saved, since only dry run"
        }
    }
}

SpeciesMigrationByProject speciesMigrationByProject = new SpeciesMigrationByProject()
speciesMigrationByProject.execute()
