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

import de.dkfz.tbi.otp.ngsdata.*

//************ List of Sample Types (one Type per line) ************//
List sampleTypeNames = """
#SampleType1
#SampleType2


""".split("\n").findAll {
    it && !it.startsWith('#')
}

//************ Choose specific reference genome (per project or sample type) ************//
SampleType.SpecificReferenceGenome specificReferenceGenome =
        //SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC

SampleType.withTransaction {
    sampleTypeNames.each {
        println "create: " + new SampleType (
                name: it.toString().toLowerCase(),
                specificReferenceGenome: specificReferenceGenome,
        ).save(flush: true)
    }

    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}

""