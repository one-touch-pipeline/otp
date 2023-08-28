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
package migration

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.utils.TransactionUtils
import de.dkfz.tbi.util.TimeFormats

import static groovyx.gpars.GParsPool.withPool

/**
 * Migration script to set the species for Individual and Sample
 * this script iterates over :
 * - all the MetaDataEntries and sets the species for corresponding Individuals and Samples from the MetadataEntries mapping table
 *
 * creates a mismatch overview when it does not find corresponding species from the mapping table
 * creates an overview when there are conflicts
 *
 * Input: mapping tables for MetadataEntries
 */

// input area

class SpeciesMigrationByMetaData {

    /**
     * Blacklist of comma separated pids from the first migration script (otp-1288-species-migration-heidelberg-1-ReferenceGenome.groovy)
     */
    String individualsWithProblemInMigration = ""

    String mappingMetadataSpecies = """\
        C3H/HeJ x CAST/E | Mouse (Mus musculus) [[C3H/HeJ x CAST/E]] |
        C3H/HeJ x CAST/Ej | Mouse (Mus musculus) [[C3H/HeJ x CAST/Ej]] |
        C3H:Castaneus | Mouse (Mus musculus) [C3H:Castaneus] |
        CAST/Ej x C3H/HeJ | Mouse (Mus musculus) [[CAST/Ej x C3H/HeJ]] |
        CAST/Ej x C57BL6/J | Mouse (Mus musculus) [[CAST/Ej x C57BL6/J]] |
        Castaneus:C3H | Mouse (Mus musculus) [Castaneus:C3H] |
        Gallus gallus | Chicken (Gallus gallus) [Unknown] |
        H. Sapien | Human (Homo sapiens) [No strain available] |
        H.sapiens | Human (Homo sapiens) [No strain available] |
        Heterocephalus glaber | Naked mole rat (Heterocephalus glaber) [Unknown] |
        Heterocephalus glaber | Naked mole rat (Heterocephalus glaber) [Unknown] |
        Homo sapiens | Human (Homo sapiens) [No strain available] |
        Human | Human (Homo sapiens) [No strain available] |
        Human (Homo sapiens) | Human (Homo sapiens) [No strain available] |
        Human (Homo sapiens)[No strain available] | Human (Homo sapiens) [No strain available] |
        Human (Homo sapiens) + Mouse (Mus musculus) | Human (Homo sapiens) [No strain available] | Mouse (Mus musculus) [Unknown]
        Human (Homo sapiens)[No strain available]+Mouse (Mus musculus)[Unknown] | Human (Homo sapiens) [No strain available] | Mouse (Mus musculus) [Unknown]
        Human Genome (hg19, GRCh37) | Human (Homo sapiens) [No strain available] |
        Mouse (Mus musculus) | Mouse (Mus musculus) [Unknown] |
        Mouse (Mus musculus)[Unknown] | Mouse (Mus musculus) [Unknown] |
        Mouse (Mus musculus)[[C57BL/6NRj]] | Mouse (Mus musculus) [[C57BL/6NRj]] |
        Mouse (Mus musculus)[[C57Bl/6J x HBV1.3tg]] | Mouse (Mus musculus) [[C57Bl/6J x HBV1.3tg]] |
        Mouse (Mus musculus)[[C57Bl/6J x HBVxfs]] | Mouse (Mus musculus) [[C57Bl/6J x HBVxfs]] |
        Mouse (Mus musculus)[[CAST/Ei x C57BL/6-Ly5.1]F1] | Mouse (Mus musculus) [[CAST/Ei x C57BL/6-Ly5.1]F1] |
        Mouse Genome | Mouse (Mus musculus) [Unknown] |
        Mouse Genome (mm10) | Mouse (Mus musculus) [Unknown] |
        Mus C57BL/6:Castaneus | Mouse (Mus musculus) [C57BL/6:Castaneus] |
        Mus Caroli | Mouse (Mus caroli) [Unknown] |
        Mus caroli | Mouse (Mus caroli) [Unknown] |
        Mus castaneus | Mouse (Mus castaneus) [Unknown] |
        Mus castaneus x Mus musculus | Mouse (Mus castaneus) [Unknown] | Mouse (Mus musculus) [Unknown]
        Mus musculus | Mouse (Mus musculus) [Unknown] |
        Mus musculus musculus x Mus musculus castaneus | Mouse (Mus musculus) [Unknown] | Mouse (Mus castaneus) [Unknown]
        Mus musculus x Mus castaneus | Mouse (Mus musculus) [Unknown] | Mouse (Mus castaneus) [Unknown]
        Mus spretus | Mouse (Mus spretus) [Unknown] |
        Mus spretus x Mus musculus | Mouse (Mus spretus) [Unknown] | Mouse (Mus musculus) [Unknown]
        #Other Species (please use remarks) | - |
        Xenograft | Human (Homo sapiens) [No strain available] | Mouse (Mus musculus) [Unknown]
        hg19 | Human (Homo sapiens) [No strain available] |
        hgla | Naked mole rat (Heterocephalus glaber) [Unknown] |
        homo sapiens sapiens | Human (Homo sapiens) [No strain available] |
        human | Human (Homo sapiens) [No strain available] |
        mm | Mouse (Mus musculus) [Unknown] |
        mm10 | Mouse (Mus musculus) [Unknown] |
        mm9 | Mouse (Mus musculus) [Unknown] |
        mmus | Mouse (Mus musculus) [Unknown] |
        mouse | Mouse (Mus musculus) [Unknown] |
        rn4 | Rat (Rattus norvegicus) [Unknown] |
        xenograft | Human (Homo sapiens) [No strain available] | Mouse (Mus musculus) [Unknown]
        xg_hs37d5_mm9 | Human (Homo sapiens) [No strain available] | Mouse (Mus musculus) [Unknown]
        """

    /**
     * Specifies how many metaDataEntries are to be processed together in one batch
     *
     * Recommended:  numOfMetaDataEntry / numLogicalCPUCores / n (some integer)
     */
    int batchSize = 100

    /**
     * Run this script w/o modification of database if set to true
     */
    boolean dryRun = true

    // script area

    MetaDataKey key = MetaDataKey.findByNameIlike(MetaDataColumn.SPECIES.name())

    List<String> individualsWithProblems = []

    Set<Long> individualIds = MetaDataEntry.withCriteria {
        projections {
            eq('key', key)
            isNotNull("value")
            ne("value", "")
            dataFile {
                seqTrack {
                    sample {
                        individual {
                            property('id')
                            if (individualsWithProblemInMigration) {
                                not {
                                    'in'('pid', individualsWithProblemInMigration.split(',')*.trim())
                                }
                            }
                        }
                    }
                }
            }
        }
    } as Set

    int numIndividualIdsSize = individualIds.size()

    int numCores = Runtime.runtime.availableProcessors()

    long numBatches = Math.ceil(numIndividualIdsSize / batchSize) as long

    List<List<Long>> listOfIndividualIdLists = individualIds.collate(batchSize)

    List<String> individualMismatch = [].asSynchronized()
    List<String> sampleMismatch = [].asSynchronized()

    List<String> individualMismatchExisting = [].asSynchronized()
    List<String> sampleMismatchExisting = [].asSynchronized()
    List<String> valueUnknown = [].asSynchronized()

    void info() {
        println "There are ${numIndividualIdsSize} individuals with species metadata entries to be processed"
        println "${numCores} logical CPU core(s) are available"
        println "${numBatches} batches will be processed"

        println "\nknownSpecies:"
        println creatSpeciesMap().keySet().sort().join('\n')
        println "\n"
    }

    void check() {
        assert batchSize > 1

        // check, will be done per batch
        createMetadataSpeciesMap()
    }

    Map<String, SpeciesWithStrain> creatSpeciesMap() {
        return SpeciesWithStrain.list().collectEntries {
            [(it.toString()): it]
        }
    }

    Map<String, Map<String, SpeciesWithStrain>> createMetadataSpeciesMap() {
        Map<String, SpeciesWithStrain> speciesMap = creatSpeciesMap()
        return mappingMetadataSpecies.split("\n")*.trim().findAll {
            it && !it.startsWith('#')
        }.collectEntries {
            List<String> values = it.split("\\|")*.trim()
            String name = values[0]
            SpeciesWithStrain speciesWithStrain1 = speciesMap[values[1]]
            assert speciesWithStrain1: "no species found for ${values[1]} of entry ${name}"
            SpeciesWithStrain speciesWithStrain2 = speciesMap[values[2]]
            if (values[2]) {
                assert speciesWithStrain2: "no species found for ${values[2]} of entry ${name}"
            }
            [(name): [main: speciesWithStrain1, mixedin: speciesWithStrain2]]
        }
    }

    String createString(Object entity, boolean isSample, List<SpeciesWithStrain> speciesWithStrains, List<String> speciesValue, List<MetaDataEntry> metaDataEntries) {
        List<String> value = [
                "Could not set ${isSample ? 'mixedin' : 'main'} species for ${isSample ? 'sample' : 'individual'} ${entity}, as the data has a different species"
        ]
        value << "- found species:"
        speciesWithStrains.sort().each {
            value << "  - ${it}"
        }
        value << "- based of species value:"
        speciesValue.sort().each {
            value << "  - ${it}"
        }
        value << "- dataFile files:"
        value.addAll(metaDataEntries.sort {
            it.id
        }.collect {
            "  - ${it.dataFile.seqTrack} ${it.dataFile.fileName} ${it} (${TimeFormats.DATE.getFormattedDate(it.dataFile.dateCreated)})".toString()
        }.sort())
        return value.join('\n')
    }

    void updateSpecies(List<MetaDataEntry> metaDataEntryList) {
        Map<String, Map<String, SpeciesWithStrain>> metadataSpeciesMap = createMetadataSpeciesMap()
        metaDataEntryList.groupBy {
            it.dataFile.individual
        }.each { Individual individual, List<MetaDataEntry> metaDataEntryListPerIndividual ->
            List<SpeciesWithStrain> speciesForIndividual = metaDataEntryListPerIndividual.collect {
                Map<String, SpeciesWithStrain> speciesEntry = metadataSpeciesMap[it.value]
                if (!speciesEntry) {
                    valueUnknown << "Unknown species: ${it.value} in: ${it.dataFile.seqTrack} ${it.dataFile}}".toString()
                    individualsWithProblems << individual.pid
                    return null
                }
                speciesEntry.main
            }.unique()
            if (speciesForIndividual.size() > 1) {
                individualMismatch << createString(individual, false, speciesForIndividual, metaDataEntryListPerIndividual*.value.unique(), metaDataEntryListPerIndividual)
                individualsWithProblems << individual.pid
                return
            }
            if (individual.species && speciesForIndividual[0] && speciesForIndividual[0] != individual.species) {
                individualMismatchExisting << "Individual ${individual} has already ${individual.species}, but according the metadata it should have ${speciesForIndividual[0]}".toString()
                individualsWithProblems << individual.pid
                return
            }
            individual.species = speciesForIndividual[0]
            individual.save(flush: false)

            metaDataEntryListPerIndividual.groupBy {
                it.dataFile.sample
            }.each { Sample sample, List<MetaDataEntry> metaDataEntryListPerSample ->
                List<SpeciesWithStrain> speciesForSample = metaDataEntryListPerSample.collect {
                    Map<String, SpeciesWithStrain> speciesEntry = metadataSpeciesMap[it.value]
                    if (!speciesEntry) {
                        // was already reported for individual
                        return null
                    }
                    speciesEntry.mixedin
                }.unique()
                if (speciesForSample.size() > 1) {
                    sampleMismatch << createString(sample, true, speciesForSample, metaDataEntryListPerSample*.value.unique(), metaDataEntryListPerSample)
                    individualsWithProblems << individual.pid
                    return
                }
                if (speciesForSample && sample.mixedInSpecies && speciesForSample as Set != sample.mixedInSpecies) {
                    individualMismatchExisting << "Sample ${sample} has already ${sample.mixedInSpecies}, but according the metadata it should have ${speciesForSample[0]}".toString()
                    individualsWithProblems << individual.pid
                    return
                }
                if (speciesForSample[0]) {
                    sample.mixedInSpecies.addAll(speciesForSample)
                    sample.save(flush: false)
                }
            }
        }
    }

    void execute() {
        info()
        check()
        withPool(numCores, {
            // loop through each batch and process it
            listOfIndividualIdLists.makeConcurrent().each { List<Long> individualIdsBatch ->
                try {
                    TransactionUtils.withNewTransaction { session ->
                        List<MetaDataEntry> metaDataEntries = MetaDataEntry.withCriteria {
                            eq('key', key)
                            isNotNull("value")
                            ne("value", "")
                            dataFile {
                                seqTrack {
                                    sample {
                                        individual {
                                            'in'('id', individualIdsBatch)
                                        }
                                    }
                                }
                            }
                        }
                        updateSpecies(metaDataEntries)
                        // flush changes to the database
                        if (!dryRun) {
                            session.flush()
                        }
                        print('.')
                    }
                } catch (Throwable t) {
                    // the pool do not provide the stacktrace
                    System.out.println StackTraceUtils.getStackTrace(t)
                    throw t
                }
            }
        })
        printResult()
    }

    void printResult() {
        println " finished\n"
        println([
                "\nResult",
                "${valueUnknown.size()} datafiles ignored, because missig mapping of value",
                "${individualMismatch.size()} mismatches for individuals in metadata",
                "${individualMismatchExisting.size()} mismatches for individuals with already migrated values",
                "${sampleMismatch.size()} mismatches for samples in metadata",
                "${sampleMismatchExisting.size()} mismatches for samples with already migrated values",
        ].join('\n'))
        if (dryRun) {
            println "\n\nNothing was saved, since only tryrun"
        }
        if (valueUnknown) {
            println "\n\n============================================================\n"
            println "unknown value:\n"
            println valueUnknown.sort().join('\n')
        }
        if (individualMismatch) {
            println "\n\n============================================================\n"
            println "individuals in metadata:\n"
            println individualMismatch.sort().join('\n\n------------------------------------------------------------\n\n')
        }
        if (individualMismatchExisting) {
            println "\n\n============================================================\n"
            println "individuals with already migrated:\n"
            println individualMismatchExisting.sort().join('\n\n------------------------------------------------------------\n\n')
        }
        if (sampleMismatch) {
            println "\n\n============================================================\n"
            println "samples in metadata:\n"
            println sampleMismatch.sort().join('\n\n------------------------------------------------------------\n\n')
        }
        if (sampleMismatchExisting) {
            println "\n\n============================================================\n"
            println "samples with already migrated:\n"
            println sampleMismatchExisting.sort().join('\n\n------------------------------------------------------------\n\n')
        }
        if (individualsWithProblems) {
            println "\n\n============================================================\n"
            println "individuals with problem:\n"
            println individualsWithProblems.unique().sort().join(', ')
        }
    }
}

SpeciesMigrationByMetaData speciesMigrationByMetaData = new SpeciesMigrationByMetaData()
speciesMigrationByMetaData.execute()
