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
package de.dkfz.tbi.otp.dataswap

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataswap.parameters.LaneSwapParameters
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactoryInstance
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.*

class LaneSwapServiceSpec extends Specification implements DataTest, ServiceUnitTest<LaneSwapService>, DomainFactoryCore, GrailsWebUnitTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                FastqFile,
                FastqcProcessedFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternallyProcessedBamFile,
                RoddyBamFile,
        ]
    }

    @TempDir
    Path tempDir

    void "swap, succeed if parameters match existing entities and data files"() {
        given:
        final Path scriptFolder = tempDir.resolve("files")
        final String scriptName = "TEST-Swap-Lane"
        Files.createDirectories(scriptFolder)
        Path bashScriptPath = Files.createFile(scriptFolder.resolve("${scriptName}.sh"))

        // Services
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            _ * fastqcOutputPath(_) >> path
        }
        service.seqTrackService = new SeqTrackService()

        service.configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.toString(),
        ])
        service.fileService = Mock(FileService) {
            1 * createOrOverwriteScriptOutputFile(scriptFolder, "${scriptName}.sh") >> bashScriptPath
            _ * createOrOverwriteScriptOutputFile(_, _) >> CreateFileHelper.createFile(tempDir.resolve("test2.txt"))
        }
        CommentService mockedCommendService = Mock(CommentService) {
            _ * saveComment(_, _) >> null
        }
        service.commentService = mockedCommendService
        service.projectService = new ProjectService()
        service.projectService.configService = service.configService
        service.projectService.fileSystemService = new TestFileSystemService()
        service.individualService = new IndividualService([
                commentService: mockedCommendService,
                configService : service.configService,
                projectService: service.projectService,
        ])
        service.lsdfFilesService = new LsdfFilesService([
                projectService   : service.projectService,
                individualService: service.individualService,
        ])

        service.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
        }
        service.deletionService = Mock(DeletionService)
        service.sampleService = Mock(SampleService)

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
                individual    : oldIndividual,
                mixedInSpecies: [TaxonomyFactoryInstance.INSTANCE.createSpeciesWithStrain(), TaxonomyFactoryInstance.INSTANCE.createSpeciesWithStrain()],
        ])
        Sample correctlyLabeledSample = createSample([
                individual: oldIndividual,
        ])
        final SeqType falsySampleSeqType = createSeqType()
        SeqTrack seqTrackWithFalsySample1 = createSeqTrackWithOneFastqFile([
                seqType: falsySampleSeqType,
                sample : falsyLabeledSample,
        ])
        SeqTrack seqTrackWithFalsySample2 = createSeqTrackWithOneFastqFile([
                sample : falsyLabeledSample,
                run    : seqTrackWithFalsySample1.run,
                seqType: seqTrackWithFalsySample1.seqType,
        ])
        SeqTrack seqTrackWithCorrectlyLabeledSample = createSeqTrackWithOneFastqFile([
                sample: correctlyLabeledSample,
                run   : seqTrackWithFalsySample1.run,
        ])
        createSeqTrack()  // a unconnected SeqTrack

        // prepare input
        RawSequenceFile seqTrack1File = RawSequenceFile.findAllBySeqTrack(seqTrackWithFalsySample1).first()
        Files.createDirectories(service.lsdfFilesService.getFileFinalPathAsPath(seqTrack1File).parent)
        Files.createFile(service.lsdfFilesService.getFileFinalPathAsPath(seqTrack1File))
        RawSequenceFile seqTrack2File = RawSequenceFile.findAllBySeqTrack(seqTrackWithFalsySample2).first()
        Files.createFile(service.lsdfFilesService.getFileFinalPathAsPath(seqTrack2File))

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
                rawSequenceFileSwaps: [new Swap(seqTrack1File.fileName, 'newFileName1'), new Swap(seqTrack2File.fileName, 'newFileName2')],
                bashScriptName: scriptName,
                log: new StringBuilder(),
                failOnMissingFiles: true,
                scriptOutputDirectory: scriptFolder,
                linkedFilesVerified: true
        )

        when:
        service.swap(parameters)

        then: "no errors"
        noExceptionThrown()

        and: "bash script created correctly"
        File bashScript = scriptFolder.resolve("${scriptName}.sh").toFile()
        bashScript.exists()
        String bashScriptContent = bashScript.text
        bashScriptContent.startsWith(AbstractDataSwapService.BASH_HEADER)
        bashScriptContent.contains(
                "rm -rf ${oldIndividual.getViewByPidPath(falsySampleSeqType).absoluteDataManagementPath.toPath().resolve(falsyLabeledSample.sampleType.dirName)}"
        )
        !bashScriptContent.contains(
                "rm -rf ${oldIndividual.getViewByPidPath(falsySampleSeqType).absoluteDataManagementPath.toPath()}\n"
        )

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
        CollectionUtils.containSame(RawSequenceFile.findAllBySeqTrackInList(seqTracks)*.fileName, ['newFileName1', 'newFileName2'])

        and: "Old dataFiles are untouched"
        RawSequenceFile.findAll().size() == 3
        RawSequenceFile.findAllBySeqTrack(seqTrackWithCorrectlyLabeledSample).size() == 1

        and: "Old SeqTracks connection is removed"
        CollectionUtils.containSame(SeqTrack.findAllBySampleInList(Sample.findAllByIndividual(oldIndividual))*.id, [seqTrackWithCorrectlyLabeledSample.id])
        SeqTrack.findAllBySample(falsyLabeledSample) == []

        and: "Falsy labeled sample is removed from oldIndividual but correctly is still there"
        CollectionUtils.containSame(Sample.findAllByIndividual(oldIndividual), [correctlyLabeledSample])

        and: "Swapped sample should have the mixedInSpecies copied"
        CollectionUtils.exactlyOneElement(Sample.findAllByIndividual(newIndividual)).mixedInSpecies.size() == 2

        and:
        1 * service.sampleService.getSamplesByIndividual(oldIndividual) >> [falsyLabeledSample, correctlyLabeledSample]
    }
}
