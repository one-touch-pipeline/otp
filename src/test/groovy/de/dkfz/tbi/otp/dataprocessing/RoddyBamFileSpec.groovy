/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class RoddyBamFileSpec extends Specification implements IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                Comment,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                Project,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    RoddyBamFile roddyBamFile

    void setup() {
        roddyBamFile = createBamFile()
        new TestConfigService()
    }

    @Unroll
    void "test getFinalLibraryQAJsonFiles OneSeqTrack libraryName is #libraryName"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator().next()
        seqTrack.libraryName = libraryName
        seqTrack.normalizedLibraryName = normalizedName
        assert seqTrack.save(flush: true)

        when:
        Map<String, File> result = roddyBamFile."get${finalOrWork}LibraryQAJsonFiles"()

        then:
        assert result.size() == 1
        assert result.containsKey(directoryName)
        assert result.get(directoryName).path.endsWith(path)

        where:
        finalOrWork | libraryName | normalizedName | directoryName || path
        "Work"      | null        | null           | "libNA"       || 'qualitycontrol/libNA/qualitycontrol.json'
        "Work"      | "library1"  | "1"            | "lib1"        || 'qualitycontrol/lib1/qualitycontrol.json'
        "Final"     | null        | null           | "libNA"       || 'qualitycontrol/libNA/qualitycontrol.json'
        "Final"     | "library1"  | "1"            | "lib1"        || 'qualitycontrol/lib1/qualitycontrol.json'
        "Final"     | "abc"       | "abc"          | "libabc"      || 'qualitycontrol/libabc/qualitycontrol.json'
    }

    @Unroll
    void "test getFinalLibraryQAJsonFiles MultipleSeqTrack libraryName is #libraryName"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.workPackage as MergingWorkPackage, [
                libraryName          : libraryName,
                normalizedLibraryName: normalizedName,
        ])
        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes = 2
        roddyBamFile.save(flush: true)

        when:
        Map<String, File> result = roddyBamFile."get${finalOrWork}LibraryQAJsonFiles"()

        then:
        assert result.size() == resultSize
        assert result.containsKey('libNA')
        assert result.containsKey(directoryName)

        where:
        finalOrWork | libraryName | normalizedName | directoryName || resultSize
        "Work"      | null        | null           | "libNA"       || 1
        "Work"      | "library1"  | "1"            | "lib1"        || 2
        "Final"     | null        | null           | "libNA"       || 1
        "Final"     | "library1"  | "1"            | "lib1"        || 2
    }

    void "getNumberOfReadsFromFastQc, when all fine, returns sum of all number of readsnumber of reads of all DataFiles of all SeqTracks of the RoddyBamFile"() {
        given:
        long numberOfReads1 = DomainFactory.counter++
        long numberOfReads2 = DomainFactory.counter++
        long numberOfReads3 = DomainFactory.counter++
        long expectedNumberOfReads = 2 * (numberOfReads1 + numberOfReads2 + numberOfReads3)

        roddyBamFile.numberOfMergedLanes = 3
        roddyBamFile.seqTracks = [
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: numberOfReads1]),
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: numberOfReads2]),
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: numberOfReads3]),
        ]
        assert roddyBamFile.save(flush: true)

        expect:
        expectedNumberOfReads == roddyBamFile.numberOfReadsFromFastQc
    }

    void "getNumberOfReadsFromFastQc, when a fastqc workflow has not finished, throws exception"() {
        given:
        roddyBamFile.numberOfMergedLanes = 3
        roddyBamFile.seqTracks = [
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS], [nReads: DomainFactory.counter++]),
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
        ]
        assert roddyBamFile.save(flush: true)

        when:
        roddyBamFile.numberOfReadsFromFastQc

        then:
        AssertionError e = thrown()
        e.message.contains('Not all Fastqc workflows of all seqtracks are finished')
    }

    void "getNumberOfReadsFromFastQc, when number of reads of a SeqTrack is null, throws exception"() {
        given:
        roddyBamFile.numberOfMergedLanes = 3
        roddyBamFile.seqTracks = [
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: null]),
                DomainFactory.createSeqTrackWithFastqFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
        ]
        assert roddyBamFile.save(flush: true)

        when:
        roddyBamFile.numberOfReadsFromFastQc

        then:
        AssertionError e = thrown()
        e.message.contains('At least one seqTrack has no value for number of reads')
    }

    void "test getFinalInsertSizeDirectory method"() {
        given:
        File expectedPath = new File("${roddyBamFile.baseDirectory}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}/${RoddyBamFileService.INSERT_SIZE_FILE_DIRECTORY}")

        expect:
        expectedPath == roddyBamFile.finalInsertSizeDirectory
    }

    void "test getFinalInsertSizeFile method"() {
        given:
        File expectedPath = new File("${roddyBamFile.baseDirectory}/${RoddyBamFileService.QUALITY_CONTROL_DIR}/${RoddyBamFileService.MERGED_DIR}/" +
                "${RoddyBamFileService.INSERT_SIZE_FILE_DIRECTORY}/${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_" +
                "${RoddyBamFileService.INSERT_SIZE_FILE_SUFFIX}")

        expect:
        expectedPath == roddyBamFile.finalInsertSizeFile
    }

    void "test getMaximalReadLength method, when sequenceLength is not set, should fail"() {
        given:
        roddyBamFile.seqTracks*.sequenceFilesWhereIndexFileIsFalse.flatten().each {
            it.sequenceLength = null
            assert it.save(flush: true)
        }

        when:
        roddyBamFile.maximalReadLength

        then:
        RuntimeException e = thrown()
        e.message.contains("No meanSequenceLength could be extracted since sequenceLength of")
    }

    void "test getMaximalReadLength method, only one value available for sequenceLength, should return this value"() {
        when:
        roddyBamFile.seqTracks*.sequenceFilesWhereIndexFileIsFalse.flatten().each { RawSequenceFile rawSequenceFile ->
            rawSequenceFile.sequenceLength = 100
            assert rawSequenceFile.save(flush: true)
        }

        then:
        100 == roddyBamFile.maximalReadLength
    }

    void "test getMaximalReadLength method, two values available for sequenceLength, should return higher value"() {
        when:
        roddyBamFile.seqTracks*.sequenceFilesWhereIndexFileIsFalse.flatten().each { RawSequenceFile rawSequenceFile ->
            rawSequenceFile.sequenceLength = 100
            assert rawSequenceFile.save(flush: true)
        }

        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                sample: roddyBamFile.sample,
                seqType: roddyBamFile.seqType,
                pipelineVersion: roddyBamFile.seqTracks.first().pipelineVersion
        )
        DomainFactory.createSequenceDataFile([seqTrack: seqTrack, sequenceLength: 80])

        roddyBamFile.seqTracks.add(seqTrack)

        then:
        100 == roddyBamFile.maximalReadLength
    }
}
