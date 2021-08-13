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

//Overview of samples of a run

//Show for a run all samples with SeqType and creation date grouped by FastqImportInstance

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.TimeFormats

Run run = Run.findByName("140819_SN7001149_0212_BC4H1BACXX")

List output = []
List<FastqImportInstance> fastqImportInstances = FastqImportInstance.findAllByRun(run).sort{it.id}
fastqImportInstances.each { FastqImportInstance fastqImportInstance ->
    output << "fastqImportInstance: ${fastqImportInstance.id}"
    List<DataFile> dataFiles = DataFile.findAllByFastqImportInstance(fastqImportInstance)
    output << dataFiles.findAll{it?.seqTrack}.collect { "${TimeFormats.DATE.getFormattedDate(it.dateCreated)}  ${it.seqTrack.sample} ${it.seqTrack.seqType} ${SampleIdentifier.findAllBySample(it.seqTrack.sample)}" }.sort().unique().join("\n")
    output << ""
}

println output.join('\n')
''
