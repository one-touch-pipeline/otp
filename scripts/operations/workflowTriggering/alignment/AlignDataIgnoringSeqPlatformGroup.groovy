/*
 * Copyright 2011-2021 The OTP authors
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
package operations.workflowTriggering.alignment

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.PanCanAlignmentDecider
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Add all lanes given by Pid, SampleType, SeqType and optional SampleName to MWP ignoring the seqPlatformGroup
 *
 * In case some lanes was not aligned because the SeqPlatformGroup are not compatible, this script
 * will add this lanes to the other and trigger the alignment.
 */

//-------------------------------
//input area

/**
 * Input, defined by:
 * - pid
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 *
 * The columns can be separated by space, comma, semicolon or tab. Multiple separators are merged together.
 */
String multiColumnInput = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
#pid5,control,RNA,SINGLE,true,sampleName2

"""

//-------------------------------
//work area

OtrsTicketService otrsTicketService = ctx.otrsTicketService

SeqTypeService seqTypeService = ctx.seqTypeService

PanCanAlignmentDecider panCanAlignmentDecider = ctx.panCanAlignmentDecider

IlseSubmission.withTransaction {
    List<SeqTrack> seqTracks = multiColumnInput.split('\n')*.trim().findAll { String line ->
        line && !line.startsWith('#')
    }.collectMany { String line ->
        List<String> values = line.split('[ ,;\t]+')*.trim()
        int valueSize = values.size()
        assert valueSize in [5, 6]: "A multi input is defined by 5 or 6 columns"
        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(values[0], values[0], values[0]),
                "Could not find one individual with name ${values[0]}")
        SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByNameIlike(values[1]),
                "Could not find one sampleType with name ${values[1]}")

        SequencingReadType sequencingReadType = SequencingReadType.findByName(values[3])
        assert sequencingReadType: "${values[3]} is no valid sequencingReadType"
        boolean singleCell = Boolean.parseBoolean(values[4])

        SeqType seqType = seqTypeService.findByNameOrImportAlias(values[2], [
                libraryLayout: sequencingReadType,
                singleCell   : singleCell,
        ])
        assert seqType: "Could not find seqType with : ${values[2]} ${values[3]} ${values[4]}"

        List<SeqTrack> seqTracks = SeqTrack.withCriteria {
            sample {
                eq('individual', individual)
                eq('sampleType', sampleType)
            }
            eq('seqType', seqType)
            if (values.size() == 6) {
                eq('sampleIdentifier', values[5])
            }
        }
        assert seqTracks: "Could not find any seqtracks for ${values.join(' ')}"
        return seqTracks
    }

    List<SeqTrack> retriggeredSeqTracks = []
    Set<SeqTrack> retriggeredMergingWorkPackages = [] as Set

    seqTracks.groupBy {
        [
                it.individual,
                it.sampleType,
                it.seqType,
                it.libraryPreparationKit,
        ]
    }.each { List group, List<SeqTrack> seqTracksPerGroup ->
        MergingWorkPackage mergingWorkPackage = CollectionUtils.atMostOneElement(MergingWorkPackage.withCriteria {
            sample {
                eq('individual', group[0])
                eq('sampleType', group[1])
            }
            eq('seqType', group[2])
            eq('libraryPreparationKit', group[3])
        } as Collection<Object>)
        if (!mergingWorkPackage) {
            SeqTrack seqTrack = seqTracksPerGroup.first()
            mergingWorkPackage = panCanAlignmentDecider.decideAndPrepareForAlignment(seqTrack, false).first()
            retriggeredSeqTracks << seqTrack
            seqTracksPerGroup.remove(seqTrack)
            retriggeredMergingWorkPackages << mergingWorkPackage
        }
        seqTracksPerGroup.each {
            if (mergingWorkPackage.seqTracks.add(it)) {
                mergingWorkPackage.needsProcessing = true
                retriggeredSeqTracks << it
                retriggeredMergingWorkPackages << mergingWorkPackage
            }
        }
        mergingWorkPackage.save()

        println "Trigger ${mergingWorkPackage}: ${mergingWorkPackage.needsProcessing}"
    }
    println "\nadded ${retriggeredSeqTracks.size()} lanes to ${retriggeredMergingWorkPackages.size()} mergingWorkPackages\n"

    if (retriggeredSeqTracks) {
        otrsTicketService.findAllOtrsTickets(retriggeredSeqTracks).each {
            otrsTicketService.resetAlignmentAndAnalysisNotification(it)
            println "reset notification for ticket ${it}"
        }
    }
    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}

''
