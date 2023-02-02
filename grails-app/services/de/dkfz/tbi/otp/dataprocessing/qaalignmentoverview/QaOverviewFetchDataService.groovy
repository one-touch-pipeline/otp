/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.project.Project

/**
 * Helper service for the {@link QaOverviewService} to fetch additional data and add it it the maps.
 *
 * It shouldn't be used outside that service.
 *
 * Currently, it fetch the following additional data for the bam file ids of the maps in the list: in the list:
 * - LibraryPreparationKit name and short name
 * - DataFile.sequenceLength
 *
 * Each type use single query.
 */
@CompileStatic
@Transactional(readOnly = true)
class QaOverviewFetchDataService {

    private static final List<List<String>> NO_LIBRARY_PREPARATION_KIT = [["", ""].asImmutable()].asImmutable()

    final static String FETCH_LIBRARY_PREPARATION_KIT = """
            |select
            |    new map (
            |        mergingWorkPackage.id as mergingWorkPackageId,
            |        kit.name as libraryPreparationKitName,
            |        kit.shortDisplayName as libraryPreparationKitShortName
            |    )
            |from
            |    MergingWorkPackage mergingWorkPackage
            |    join mergingWorkPackage.seqTracks seqTrack1
            |    left outer join seqTrack1.libraryPreparationKit kit
            |where
            |    mergingWorkPackage.id in (:mergingWorkPackageIds)
        """.stripMargin()

    final static String FETCH_SEQUENCE_LENGTH = """
            |select
            |    new map (
            |        mergingWorkPackage.id as mergingWorkPackageId,
            |        dataFile.sequenceLength as sequenceLength
            |    )
            |from
            |    MergingWorkPackage mergingWorkPackage
            |    join mergingWorkPackage.seqTracks seqTrack1,
            |    DataFile dataFile
            |    join dataFile.seqTrack seqTrack2
            |where
            |      mergingWorkPackage.id in (:mergingWorkPackageIds)
            |      and  seqTrack1 = seqTrack2
        """.stripMargin()

    /**
     * extract the bamIds for each map of the list, fetch the needed data and put it in the corresponding map.
     */
    void addLibraryPreparationKitAndSequencingLengthInformation(List<Map<String, ?>> qaMapList) {
        if (!qaMapList) {
            return
        }
        List<?> mergingWorkPackageIds = qaMapList*.get('mergingWorkPackageId')
        addLibraryPreparationKit(qaMapList, mergingWorkPackageIds)
        addSequencingLengthInformation(qaMapList, mergingWorkPackageIds)
    }

    private void addLibraryPreparationKit(List<Map<String, ?>> qaMapList, List<?> mergingWorkPackageIds) {
        Map<Long, List<Map<String, ?>>> groupedResult = queryAndGroup(FETCH_LIBRARY_PREPARATION_KIT, mergingWorkPackageIds)

        qaMapList.each { Map<String, ?> qaMap ->
            List<Map<String, ?>> valueMapList = groupedResult[qaMap.mergingWorkPackageId]
            addLibraryPreparationKitInformation(qaMap, valueMapList)
        }
    }

    private void addSequencingLengthInformation(List<Map<String, ?>> qaMapList, List<?> mergingWorkPackageIds) {
        Map<Long, List<Map<String, ?>>> groupedResult = queryAndGroup(FETCH_SEQUENCE_LENGTH, mergingWorkPackageIds)

        qaMapList.each { Map<String, ?> qaMap ->
            List<Map<String, ?>> valueMapList = groupedResult[qaMap.mergingWorkPackageId]
            addReadLengthInformation(qaMap, valueMapList)
        }
    }

    private Map<Long, List<Map<String, ?>>> queryAndGroup(String query, List<?> mergingWorkPackageIds) {
        List<Map<String, ?>> result = Project.executeQuery(query, [mergingWorkPackageIds: mergingWorkPackageIds])

        return result.groupBy { map ->
            map['mergingWorkPackageId']
        } as Map<Long, List<Map<String, ?>>>
    }

    private void addLibraryPreparationKitInformation(Map qaMap, List<Map<String, ?>> valueMapList) {
        List<List<String>> libPrepKits = valueMapList ? valueMapList.collect {
            [
                    it.libraryPreparationKitShortName ?: "-",
                    it.libraryPreparationKitName ?: "-",
            ]
        }.unique().sort() as List<List<String>> : NO_LIBRARY_PREPARATION_KIT

        qaMap['libraryPreparationKitShortName'] = libPrepKits*.getAt(0).join(', ')
        qaMap['libraryPreparationKitName'] = libPrepKits*.getAt(1).join(', ')
    }

    private void addReadLengthInformation(Map qaMap, List<Map<String, ?>> valueMapList) {
        if (!valueMapList) {
            qaMap['readLength'] = null
            return
        }

        String sequenceLengthOfFirstDataFile = valueMapList.first().sequenceLength

        if (!sequenceLengthOfFirstDataFile) {
            qaMap['readLength'] = null
            return
        }
        /*
            This method assumes, that it does not matter which seqTrack is used to get the sequencedLength.
            Within one merged bam file all are the same. This is incorrect, see OTP-1670.
        */
        double readLength = (sequenceLengthOfFirstDataFile.contains('-') ? ((sequenceLengthOfFirstDataFile.split('-').sum {
            it as double
        } as double) / 2) : sequenceLengthOfFirstDataFile) as double

        qaMap['readLength'] = readLength
    }
}
