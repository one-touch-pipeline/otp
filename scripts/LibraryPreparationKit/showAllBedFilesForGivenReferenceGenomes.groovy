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

import static org.springframework.util.Assert.*

//-----------------------------------
//input area

//The names of reference genomes for which the bedfiles and library preperation kit should be shown
List<String> refGenomeNames = [
        '1KGRef_PhiX'
]

//-----------------------------------
//checks

assert refGenomeNames: 'At least one reference genome have to be provided'


List<ReferenceGenome> referenceGenomes = refGenomeNames.collect {
    ReferenceGenome referenceGenome = ReferenceGenome.findByName(it)
    assert referenceGenome: "The ReferenceGenome '${it} could not found in OTP"
    return referenceGenome
}

//-----------------------------------
//work

BedFile.withTransaction {
    referenceGenomes.each { ReferenceGenome referenceGenome ->
        List<BedFile> bedFileList = BedFile.findAllByReferenceGenome(referenceGenome)
        bedFileList.each {
            BedFile bedFileObject -> 
            println "${bedFileObject.referenceGenome}\t${bedFileObject.libraryPreparationKit}\t${bedFileObject.fileName}"
        }
    }
}


