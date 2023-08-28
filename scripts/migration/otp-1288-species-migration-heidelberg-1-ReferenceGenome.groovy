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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TransactionUtils
import de.dkfz.tbi.util.TimeFormats

import static groovyx.gpars.GParsPool.withPool

/**
 * Migration script to set the species for Individual and Sample
 * this script iterates over :
 * - all the RoddyBamFiles and sets the species for corresponding Individuals and Samples from the ReferenceGenome mapping table
 *
 * creates a mismatch overview when it does not find corresponding species from the mapping table
 * creates an overview when there are conflicts
 *
 * Input: mapping tables for ReferenceGenome
 */

// input area
String mappingReferenceGenomeSpecies = """\
1KGRef_PhiX | human |
GRCh38_decoy_ebv_phiX_alt_hla_chr | human |
GRCm38mm10 | mouse |
GRCm38mm10_PhiX | mouse |
GRCm38mm10_PhiX_hD3A | mouse |
hg19 | human |
hg38 | human |
hg38_CGA_000001405.15-no_alt_analysis_set | human |
hg38_PhiX | human |
hg_GRCh38 | human |
hg_GRCh38-2020-A | human |
hg_GRCh38-2020-A_premrna | human |
hg_GRCm38 | mouse |
hg_GRCm38-2020-A | mouse |
hs37d5 | human |
hs37d5+mouse | human | mouse
hs37d5_Bovine_Phix | human | bovine
hs37d5_GRCm38mm | human | mouse
hs37d5_GRCm38mm_PhiX | human | mouse
methylCtools_GRCm38mm10_PhiX_Lambda | mouse |
methylCtools_GRCm38mm10_PhiX_Lambda_hD3A | mouse |
methylCtools_hg38_PhiX_Lambda | human |
methylCtools_hg38_PhiX_Lambda_Benchmark | human |
methylCtools_hs37d5_GRCm38mm10_PhiX_Lambda | human | mouse
methylCtools_hs37d5_PhiX_Lambda | human |
methylCtools_mm10_UCSC_PhiX_Lambda | mouse |
methylCtools_mm9_PhiX_Lambda | mouse |
mm9_phiX | mouse |
"""

/**
 * Specifies how many roddyBamFiles are to be processed together in one batch
 *
 * Recommended:  numOfRoddyBamFile / numLogicalCPUCores / n (some integer)
 */
int batchSize = 100

/**
 * Run this script w/o modification of database if set to true
 */
boolean dryRun = true

// script area
assert batchSize > 1

Set<Long> individualIds = AbstractBamFile.createCriteria().listDistinct {
    projections {
        workPackage {
            sample {
                individual {
                    property('id')
                }
            }
        }
    }
} as Set

int numIndividualIdsSize = individualIds.size()
println "There are ${numIndividualIdsSize} individuals with bam files to be processed"
int numCores = Runtime.runtime.availableProcessors()
println "${numCores} logical CPU core(s) are available"

long numBatches = Math.ceil(numIndividualIdsSize / batchSize) as long
println "${numBatches} batches will be processed"

List<List<Long>> listOfIndividualIdLists = individualIds.collate(batchSize)

List<String> individualsWithProblems = []

// check, will be done per batch
createMetadataSpeciesMap(mappingReferenceGenomeSpecies)

Map<String, SpeciesWithStrain> creatSpeciesMap() {
    SpeciesWithStrain speciesWithStrainHuman = CollectionUtils.exactlyOneElement(SpeciesWithStrain.withCriteria {
        species {
            speciesCommonName {
                eq('name', 'Human')
            }
            eq('scientificName', 'Homo sapiens')
        }
        strain {
            eq('name', 'No strain available')
        }
    })

    SpeciesWithStrain speciesWithStrainMouse = CollectionUtils.exactlyOneElement(SpeciesWithStrain.withCriteria {
        species {
            speciesCommonName {
                eq('name', 'Mouse')
            }
            eq('scientificName', 'Mus musculus')
        }
        strain {
            eq('name', 'Unknown')
        }
    })

    SpeciesWithStrain speciesWithStrainBovine = CollectionUtils.exactlyOneElement(SpeciesWithStrain.withCriteria {
        species {
            speciesCommonName {
                eq('name', 'Bovine')
            }
            eq('scientificName', 'Bos taurus')
        }
        strain {
            eq('name', 'No strain available')
        }
    })

    return [
            human : speciesWithStrainHuman,
            mouse : speciesWithStrainMouse,
            bovine: speciesWithStrainBovine,
    ]
}

Map<String, Map<String, SpeciesWithStrain>> createMetadataSpeciesMap(String mappingReferenceGenomeSpecies) {
    Map<String, SpeciesWithStrain> speciesMap = creatSpeciesMap()
    return mappingReferenceGenomeSpecies.split("\n").collectEntries {
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

String createString(Object entity, boolean isSample, List<SpeciesWithStrain> speciesWithStrains, List<String> referenceGenomes, List<AbstractBamFile> bamFiles) {
    List<String> value = [
            "Could not set ${isSample ? 'mixedin' : 'main'} species for ${isSample ? 'sample' : 'individual'} ${entity}, as the data has a different species"
    ]
    value << "- found species:"
    speciesWithStrains.sort().each {
        value << "  - ${it}"
    }
    value << "- based of reference genomes:"
    referenceGenomes.sort().each {
        value << "  - ${it}"
    }
    value << "- bam files:"
    bamFiles.sort {
        it.id
    }.each {
        value << "  - ${it}"
    }
    value << "- imported at:"
    List<DataFile> dataFiles = DataFile.withCriteria {
        'in'('seqTrack', bamFiles*.containedSeqTracks.flatten())
    }
    dataFiles*.dateCreated.collect {
        TimeFormats.DATE.getFormattedDate(it)
    }.unique().sort().collect {
        value << "  - ${it}"
    }
    value << "- species in metadata:"
    MetaDataEntry.withCriteria {
        'in'('sequenceFile', dataFiles)
        key {
            'eq'('name', MetaDataColumn.SPECIES.name())
        }
    }*.value.unique().sort().each {
        value << "  - ${it}"
    }
    return value.join('\n')
}

void updateSpecies(List<AbstractBamFile> abstractBamFiles, Map<String, Map<String, SpeciesWithStrain>> metadataSpeciesMap,
                   List<String> individualMismatch, List<String> sampleMismatch, List<String> individualsWithProblems = []) {
    abstractBamFiles.groupBy {
        it.individual
    }.each { Individual individual, List<AbstractBamFile> abstractBamFilesPerIndividual ->
        List<String> referenceGenomeNamesForIndividual = abstractBamFilesPerIndividual*.referenceGenome*.name.unique()
        List<SpeciesWithStrain> speciesForIndividual = referenceGenomeNamesForIndividual.collect {
            Map<String, SpeciesWithStrain> speciesEntry = metadataSpeciesMap[it]
            assert speciesEntry: "Could not found species entry for ${it}"
            speciesEntry.main
        }.unique()
        if (speciesForIndividual.size() > 1) {
            individualMismatch << createString(individual, false, speciesForIndividual, referenceGenomeNamesForIndividual, abstractBamFilesPerIndividual)
            individualsWithProblems << individual.pid
            return
        }
        individual.species = speciesForIndividual[0]
        individual.save(flush: false)

        abstractBamFilesPerIndividual.groupBy {
            it.sample
        }.each { Sample sample, List<AbstractBamFile> abstractBamFilesPerSample ->
            List<String> referenceGenomeNamesForSample = abstractBamFilesPerSample*.referenceGenome*.name.unique()
            List<SpeciesWithStrain> speciesForSample = referenceGenomeNamesForSample.collect {
                Map<String, SpeciesWithStrain> speciesEntry = metadataSpeciesMap[it]
                assert speciesEntry: "Could not found species entry for ${it}"
                speciesEntry.mixedin
            }.unique()
            if (speciesForSample.size() > 1) {
                sampleMismatch << createString(sample, true, speciesForSample, referenceGenomeNamesForSample, abstractBamFilesPerSample)
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

List<String> individualMismatch = [].asSynchronized()
List<String> sampleMismatch = [].asSynchronized()

withPool(numCores, {
    // loop through each batch and process it
    listOfIndividualIdLists.makeConcurrent().each { List<Long> individualIdsBatch ->
        TransactionUtils.withNewTransaction { session ->
            List<AbstractBamFile> abstractBamFiles = AbstractBamFile.withCriteria {
                workPackage {
                    sample {
                        individual {
                            'in'('id', individualIdsBatch)
                        }
                    }
                }
                eq('withdrawn', false)
            }.findAll { AbstractBamFile abstractBamFile ->
                abstractBamFile.isMostRecentBamFile()
            }
            Map<String, Map<String, SpeciesWithStrain>> metadataSpeciesMap = createMetadataSpeciesMap(mappingReferenceGenomeSpecies)
            updateSpecies(abstractBamFiles, metadataSpeciesMap, individualMismatch, sampleMismatch, individualsWithProblems)
            // flush changes to the database
            if (!dryRun) {
                session.flush()
            }
            print('.')
        }
    }
})

println " finished\n"
println "Found ${individualMismatch.size()} mismatches for individuals and ${sampleMismatch.size()} for samples"

if (dryRun) {
    println "\n\nNothing was saved, since only tryrun"
}
if (individualMismatch) {
    println "\n\n============================================================\n"
    println "individuals:\n"
    println individualMismatch.sort().join('\n\n------------------------------------------------------------\n\n')
}
if (sampleMismatch) {
    println "\n\n============================================================\n"
    println "samples:\n"
    println sampleMismatch.sort().join('\n\n------------------------------------------------------------\n\n')
}

if (individualsWithProblems) {
    println "\n\n============================================================\n"
    println "individuals with problem:\n"
    println individualsWithProblems.unique().sort().join(', ')
}
