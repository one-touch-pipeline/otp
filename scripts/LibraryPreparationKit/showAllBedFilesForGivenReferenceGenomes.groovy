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

import de.dkfz.tbi.otp.ngsdata.BedFile
import de.dkfz.tbi.otp.ngsdata.BedFileService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

/**
 * Script to show for selected bam files the bed files together with the library preparation kit.
 *
 * The input is a list of reference genomes.
 *
 * The output is a table of following columns:
 * - reference genome name
 * - library preparation kit
 * - bed file name
 * - bed file path
 */

//-----------------------------------
//input area

//The names of reference genomes for which the bedfiles and library preparation kit should be shown
List<String> refGenomeNames = [
        '1KGRef_PhiX'
]

//-----------------------------------
//checks

assert refGenomeNames: 'At least one reference genome must be specified'

List<ReferenceGenome> referenceGenomes = refGenomeNames.collect {
    ReferenceGenome referenceGenome = ReferenceGenome.findByName(it)
    assert referenceGenome: "The ReferenceGenome '${it}' could not be found in OTP"
    return referenceGenome
}

//-----------------------------------
//work

BedFileService bedFileService = ctx.bedFileService

BedFile.withTransaction {
    referenceGenomes.each { ReferenceGenome referenceGenome ->
        List<BedFile> bedFileList = BedFile.findAllByReferenceGenome(referenceGenome)
        bedFileList.each { BedFile bedFileObject ->
            println "${bedFileObject.referenceGenome}\t${bedFileObject.libraryPreparationKit}\t${bedFileObject.fileName}\t${bedFileService.filePath(bedFileObject, false)}"
        }
    }
}
