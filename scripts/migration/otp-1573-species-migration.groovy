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

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TransactionUtils

import java.nio.file.Path
import java.nio.file.Paths

// This script can be used to manually fix species assignments for individuals/samples that were incorrectly/not set with the automatic scripts from otp-1288.
// It should be run after these scripts.
//
// It requires a TSV files with the following data:
//  * First column: PID
//  * Second column: Sample type name
//  * Fifth column: Species name, as listed as key in the map below
//  * Other columns are ignored
//  * The file must not have a header

// Include absolute path to the TSV file here:
Path pathToTsvFile = Paths.get("")

Map speciesMap = [
        "Human (Homo sapiens) [No strain available]": CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Homo sapiens" && strain.name == "No strain available" }.list()),
        "[Mouse (Mus musculus) [Unknown]]"          : CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Mus musculus" && strain.name == "Unknown" }.list()),
        "Bos taurus"                                : CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Bos taurus" && strain.name == "No strain available" }.list()),
]

TransactionUtils.withNewTransaction { session ->
    pathToTsvFile.readLines().each { line ->
        String[] fields = line.split(/\t/)

        String pid = fields[1].trim()
        String sampleType1 = fields[2].trim()
        Sample sample = CollectionUtils.atMostOneElement(Sample.createCriteria().list {
            individual {
                eq("pid", pid)
            }
            sampleType {
                eq("name", sampleType1)
            }
        } as List<Sample>)
        if (!sample) {
            println "Unknown sample with PID '${pid}' and sample type '${sampleType1}'."
            return
        }
        if (fields.size() < 6 || !fields[5].trim()) {
            println "No update for sample with PID '${pid}' and sample type '${sampleType1}'."
            return
        }

        List<String> species = fields[5].split(/\+/).collect { it.trim() }
        boolean unknownSpecies = false
        species.each {
            if (!(it in speciesMap.keySet())) {
                println "Unknown species '${it}'"
                unknownSpecies = true
            }
        }
        if (unknownSpecies) {
            return
        }
        String mainSpecies = species.remove(0)
        List<String> mixedInSpecies = species

        sample.individual.species = speciesMap[mainSpecies]
        sample.individual.save(flush: false)
        sample.mixedInSpecies = mixedInSpecies.collect { speciesMap[it] } as Set
        sample.save(flush: false)
        println "update for sample with PID '${pid}' and sample type '${sampleType1}'."
    }
    session.flush()
}

[]
