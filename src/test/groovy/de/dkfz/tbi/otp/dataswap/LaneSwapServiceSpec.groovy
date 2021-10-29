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
package de.dkfz.tbi.otp.dataswap

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataswap.parameters.LaneSwapParameters
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.IndividualService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.MergingAssignment
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.FileSystems
import java.nio.file.Path

class LaneSwapServiceSpec extends Specification implements DataTest, ServiceUnitTest<LaneSwapService>, DomainFactoryCore, GrailsWebUnitTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternallyProcessedMergedBamFile,
                RoddyBamFile,
                AlignmentPass,
                MergingAssignment,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    void "swap, succeed if parameters match existing entities and data files"() {
        given:
        // Services
        Path path = temporaryFolder.newFile().toPath()
        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            _ * fastqcOutputFile(_) >> path
        }
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> path
            _ * getFileViewByPidPath(_) >> path
            _ * getWellAllFileViewByPidPath(_) >> path
        }
        service.seqTrackService = new SeqTrackService()

        Realm realm = createRealm()
        service.configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.root.toString(),
        ])
        service.configService.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(ProcessingOption.OptionName.REALM_DEFAULT_VALUE) >> realm.name
        }
        service.fileService = Mock(FileService) {
            _ * createOrOverwriteScriptOutputFile(_, _, _) >> temporaryFolder.newFile().toPath()
        }
        CommentService mockedCommendService = Mock(CommentService) {
            _ * saveComment(_, _) >> null
        }
        service.commentService = mockedCommendService
        service.individualService = new IndividualService([
                commentService: mockedCommendService,
                configService: service.configService,
        ])

        service.deletionService = Mock(DeletionService) {
            _ * deleteAllMergingAssignmentsWithAlignmentDataFilesAndSeqScans(_) >> _
        }
        service.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystemOnDefaultRealm() >> FileSystems.default
        }

        // Domain
        SampleType newSampleType = createSampleType()
        SeqType newSeqType = createSeqType([
                singleCell: true
        ])
        Individual newIndividual = createIndividual()
        Individual oldIndividual = createIndividual()

        // creates the sample that will be on two SeqTracks of the same run
        // the same individual will have another Sample, but this is correct
        Sample falsyLabeledSample = createSample([
                individual: oldIndividual,
        ])
        Sample correctlyLabeledSample = createSample([
                individual: oldIndividual,
        ])
        SeqTrack seqTrackWithFalsySample1 = createSeqTrackWithOneDataFile([
                sample: falsyLabeledSample,
        ])
        SeqTrack seqTrackWithFalsySample2 = createSeqTrackWithOneDataFile([
                sample : falsyLabeledSample,
                run    : seqTrackWithFalsySample1.run,
                seqType: seqTrackWithFalsySample1.seqType,
        ])
        SeqTrack seqTrackWithCorrectlyLabeledSample = createSeqTrackWithOneDataFile([
                sample: correctlyLabeledSample,
                run   : seqTrackWithFalsySample1.run,
        ])
        createSeqTrack()  // a unconnected SeqTrack

        // prepare input
        String seqTrack1FileName = DataFile.findAllBySeqTrack(seqTrackWithFalsySample1).first().fileName
        String seqTrack2FileName = DataFile.findAllBySeqTrack(seqTrackWithFalsySample2).first().fileName
        Map<String, String> dataFileMap = [:]
        dataFileMap.put(seqTrack1FileName, 'newFileName1')
        dataFileMap.put(seqTrack2FileName, 'newFileName2')

        LaneSwapParameters parameters = new LaneSwapParameters(
                projectNameSwap: new Swap(falsyLabeledSample.individual.project.name, newIndividual.project.name),
                pidSwap: new Swap(falsyLabeledSample.individual.pid, newIndividual.pid),
                sampleTypeSwap: new Swap(falsyLabeledSample.sampleType.name, newSampleType.name),
                seqTypeSwap: new Swap(seqTrackWithFalsySample1.seqType.name, newSeqType.name),
                singleCellSwap: new Swap(seqTrackWithFalsySample1.seqType.singleCell, newSeqType.singleCell),
                sequencingReadTypeSwap: new Swap(seqTrackWithFalsySample1.seqType.libraryLayout.name(), newSeqType.libraryLayout.name()),
                runName: seqTrackWithFalsySample1.run.name,
                lanes: [
                        seqTrackWithFalsySample1.laneId,
                        seqTrackWithFalsySample2.laneId,
                ],
                sampleNeedsToBeCreated: true,
                dataFileSwaps: [new Swap(seqTrack1FileName, 'newFileName1'), new Swap(seqTrack2FileName, 'newFileName2')],
                bashScriptName: 'newUniqueScriptName',
                log: new StringBuilder(),
                failOnMissingFiles: true,
                scriptOutputDirectory: temporaryFolder.newFolder() as Path,
                linkedFilesVerified: true
        )

        when:
        service.swap(parameters)

        then: "no errors"
        noExceptionThrown()

        and: "SeqTracks have the new attributes"
        List<SeqTrack> seqTracks = SeqTrack.findAllBySample(CollectionUtils.exactlyOneElement(Sample.findAllByIndividual(newIndividual)))
        assert seqTracks.size() == 2
        List<Boolean> resultBoolList = []
        seqTracks.each {
            resultBoolList.add(it.individual == newIndividual)
            resultBoolList.add(it.run == seqTrackWithFalsySample1.run)
            resultBoolList.add(it.sampleType == newSampleType)
            resultBoolList.add(it.seqType == newSeqType)
            resultBoolList.add(it.swapped)
        }
        assert resultBoolList.every { it }

        and: "DataFiles to the new SeqTracks have the new name"
        CollectionUtils.containSame(DataFile.findAllBySeqTrackInList(seqTracks)*.fileName, ['newFileName1', 'newFileName2'])

        and: "Old dataFiles are untouched"
        DataFile.findAll().size() == 3
        DataFile.findAllBySeqTrack(seqTrackWithCorrectlyLabeledSample).size() == 1

        and: "Old SeqTracks connection is removed"
        CollectionUtils.containSame(SeqTrack.findAllBySampleInList(Sample.findAllByIndividual(oldIndividual))*.id, [seqTrackWithCorrectlyLabeledSample.id])
        SeqTrack.findAllBySample(falsyLabeledSample) == []

        and: "Falsy labeled sample is NOT removed from oldIndividual"
        CollectionUtils.containSame(Sample.findAllByIndividual(oldIndividual), [falsyLabeledSample, correctlyLabeledSample])
    }
}
