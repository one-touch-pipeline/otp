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

import de.dkfz.tbi.ngstools.bedUtils.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import static org.springframework.util.Assert.*

//-----------------------------------
//input area

/**
 * Name of the bed file. It has to be exist for all given reference genomes in the target region directory
 */
String bedFileName = 'Agilent_S0447132_Covered.bed'

//Name of the lib prep kit the bedfile is to load for
String libPrepKitName = '10XGenomics'

//The name of the reference genomes the bedfile should be load for
List<String> refGenomeNames = [
        'hs37d5',
        '1KGRef_PhiX',
        'hs37d5_GRCm38mm',
        'hs37d5_GRCm38mm_PhiX',
]

//-----------------------------------
//checks

assert bedFileName: 'Please provide a bed file name.'
assert libPrepKitName: 'Please provide a libPrepKit.'
assert refGenomeNames: 'At least one reference genome have to be provided'

LibraryPreparationKit libraryPreparationKit = CollectionUtils.atMostOneElement(LibraryPreparationKit.findAllByName(libPrepKitName))
assert libraryPreparationKit: "The LibraryPreparationKit '${libPrepKitName} could not found in OTP"

List<ReferenceGenome> referenceGenomes = refGenomeNames.collect {
    ReferenceGenome referenceGenome = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(it))
    assert referenceGenome: "The ReferenceGenome '${it} could not found in OTP"
    return referenceGenome
}

//-----------------------------------
//work

BedFileService bedFileService = ctx.bedFileService

BedFile.withTransaction {
    referenceGenomes.each { ReferenceGenome referenceGenome ->
        BedFile existingBedFile = CollectionUtils.atMostOneElement(BedFile.findAllByFileNameAndReferenceGenome(bedFileName, referenceGenome))
        if (existingBedFile) {
            println "bedFile with name ${bedFileName} and reference genome ${referenceGenome.name} already exists in the db and will be skipped."
            return
        }
        BedFile bedFileDom = new BedFile(
                fileName: bedFileName,
                referenceGenome: referenceGenome,
                libraryPreparationKit: libraryPreparationKit)
        notNull bedFilePath = bedFileService.filePath(bedFileDom)
        // retrieve referenceGenomeEntryNames for a specific referenceGenome
        List<ReferenceGenomeEntry> referenceGenomeEntries = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome)
        notEmpty referenceGenomeEntries
        TargetIntervals targetIntervals = TargetIntervalsFactory.create(bedFilePath, referenceGenomeEntries*.name)
        bedFileDom.targetSize = targetIntervals.baseCount
        bedFileDom.mergedTargetSize = targetIntervals.uniqueBaseCount
        notNull bedFileDom.validate()
        bedFileDom.save(flush: true)
        println "Added ${bedFileName} to OTP database."
    }
}
