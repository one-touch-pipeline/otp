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

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import static org.springframework.util.Assert.*

/**
 * Script to create a tsv file including the reference genome meta information:
 * reference genome entry names
 * length of reference genome entry
 * lengthWithoutN of reference genome entry
 *
 * This script is supposed to be called from (web)console, so no
 * arguments can be specified but are inserted manually.
 */

ReferenceGenomeService referenceGenomeService = new ReferenceGenomeService()

// the referenceGenomeName always has to be changed to the used referenceGenome
String referenceGenomeName = "methylCtools_mm9_PhiX_Lambda" // e.g. "hs37d5" or "hg19"
ReferenceGenome referenceGenome = ReferenceGenome.findByName(referenceGenomeName)
notNull referenceGenome

// prepare to store the meta information
String referenceMetaDataPath = referenceGenomeService.referenceGenomeMetaInformationPath(referenceGenome).absolutePath
File referenceMetaData = new File(referenceMetaDataPath)
referenceMetaData.createNewFile()
isTrue referenceMetaData.canWrite()

// store the names and length values for each reference genome entry of the reference genome
List<ReferenceGenomeEntry> referenceGenomeEntries = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome)
String entry = ""
referenceGenomeEntries.each { ReferenceGenomeEntry referenceGenomeEntry ->
    entry += referenceGenomeEntry.name + "\t" + referenceGenomeEntry.length + "\t" + referenceGenomeEntry.lengthWithoutN + "\n"
}
referenceMetaData.write(entry)
