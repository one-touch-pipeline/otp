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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Script to delete a FastqImportInstance if it was loaded twice.
 */

long fastqImportInstanceId = 0

FastqImportInstance.withTransaction {
    FastqImportInstance fastqImportInstance = FastqImportInstance.get(fastqImportInstanceId)
    assert fastqImportInstance
    DataFile.findAllByFastqImportInstance(fastqImportInstance).each { DataFile dataFile ->
        MetaDataEntry.findAllByDataFile(dataFile).each { MetaDataEntry entry ->
            entry.delete(flush: true)
        }
        assert MetaDataEntry.countByDataFile(dataFile) == 0
        CollectionUtils.exactlyOneElement(FastqcProcessedFile.findAllByDataFile(dataFile)).delete()
        SeqTrack seqTrack = dataFile.seqTrack
        dataFile.delete(flush: true)
        if (!seqTrack.dataFiles) {
            RoddyBamFile roddyBamFile = CollectionUtils.atMostOneElement(RoddyBamFile.createCriteria().list {
                seqTracks {
                    eq('id', seqTrack.id)
                }
            })
            if (roddyBamFile) {
                assert roddyBamFile.seqTracks.remove(seqTrack)
                roddyBamFile.numberOfMergedLanes--
                assert roddyBamFile.save(flush: true)
            }
            MergingAssignment.findBySeqTrack(seqTrack)*.delete()
            seqTrack.delete(flush: true)
        }
    }
    assert DataFile.countByFastqImportInstance(fastqImportInstance) == 0
    MetaDataFile.findAllByFastqImportInstance(fastqImportInstance).each {
        it.delete(flush: true)
    }
    assert MetaDataFile.countByFastqImportInstance(fastqImportInstance) == 0
    assert FastqImportInstance.get(fastqImportInstanceId) == fastqImportInstance
    fastqImportInstance.delete()
    assert FastqImportInstance.get(fastqImportInstanceId) == null
    assert false
}
''
