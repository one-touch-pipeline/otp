/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.*

// -----------
// input

int year = 2018
int month =12
int day = 17

// ---------------------------
// work area

Date dateStart = new Date(year - 1900, month - 1, day)
Date dateEnd = dateStart.plus(1)

List<String> output = []

List<RoddyBamFile> bamFile = RoddyBamFile.findAllByDateCreatedBetween(dateStart, dateEnd)

output << "Alignment workflows started on: ${dateStart}"
output << "Count of files: ${bamFile.size()}"
output << "Files: "

output <<  [
        'otp id',
        'mwp identifier',
        'latest',
        'withdrawn',
        'qcTrafficLightStatus',
        'project',
        'individual',
        'sampleType',
        'seqTypeName',
        'libraryLayout',
        'bulk or single cell',
        'libraryPreparationKit',
        'referenceGenom',
        'is fasttrack',
        'dateCreated',
        'workDirectory',
].join('|')

output << bamFile.collect {
    [

            it.id,
            it.identifier,
            it.isMostRecentBamFile() ? 'latest':'',
            it.withdrawn ? 'withdrawn' : '',
            it.qcTrafficLightStatus,
            it.project,
            it.individual,
            it.sampleType,
            it.seqType.seqTypeName,
            it.seqType.libraryLayout,
            it.seqType.singleCellDisplayName,
            it.mergingWorkPackage.libraryPreparationKit ?: '',
            it.referenceGenome,
            it.processingPriority.name,
            it.dateCreated,
            it.workDirectory,
    ].join('|')
}.join('\n')

println output.join('\n')
