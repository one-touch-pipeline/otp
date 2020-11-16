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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path
import java.nio.file.Paths

class DataSwapServiceSpec extends Specification implements DataTest, ServiceUnitTest<DataSwapService>, DomainFactoryCore {

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
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    protected TestConfigService configService

    void "completeOmittedNewValuesAndLog, valid keys, keys are used as values"() {
        given:
        Map<String, String> swapMap = [
                ("key1"): "",
                ("key2"): "key2_value",
                ("key3"): "",
        ]

        when:
        service.completeOmittedNewValuesAndLog(swapMap, "label", new StringBuilder())

        then:
        swapMap["key1"] == "key1"
        swapMap["key2"] == "key2_value"
        swapMap["key3"] == "key3"
    }

    void "completeOmittedNewValuesAndLog, empty key"() {
        given:
        Map<String, String> swapMap = [
                (""): "value",
        ]

        when:
        service.completeOmittedNewValuesAndLog(swapMap, "label", new StringBuilder())

        then:
        swapMap[""] == "value"
    }

    void "completeOmittedNewValuesAndLog, builds valid log"() {
        given:
        StringBuilder log = new StringBuilder()
        Map<String, String> swapMap = [
                ("key1"): "",
                ("key2"): "key2_value",
                ("")    : "key3_value",
        ]

        when:
        service.completeOmittedNewValuesAndLog(swapMap, "label", log)

        then:
        log.toString() == """
  swapping label:
    - key1 --> key1
    - key2 --> key2_value
    -  --> key3_value"""
    }

    @Unroll
    void "collectFileNamesOfDataFiles, when single cell is #singleCell and label is #wellLabel, then return correct list"() {
        given:
        DataFile dataFile = createDataFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: singleCell,
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        )

        service.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile) >> 'finalFile'
            1 * getFileViewByPidPath(dataFile) >> 'viewByPidFile'
            wellCount * getWellAllFileViewByPidPath(dataFile) >> 'wellFile'
            0 * _
        }
        service.singleCellService = Mock(SingleCellService) {
            wellCount * singleCellMappingFile(dataFile) >> Paths.get('wellMappingFile')
            wellCount * mappingEntry(dataFile) >> 'entry'
            0 * _
        }

        Map<DataFile, Map<String, ?>> expected = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): 'finalFile',
                        (DataSwapService.VBP_FILE_NAME)   : 'viewByPidFile',
                ],
        ]
        if (wellCount) {
            expected[dataFile] << [
                    (DataSwapService.WELL_FILE_NAME)              : 'wellFile',
                    (DataSwapService.WELL_MAPPING_FILE_NAME)      : Paths.get('wellMappingFile'),
                    (DataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): 'entry',
            ]
        }

        expect:
        expected == service.collectFileNamesOfDataFiles([dataFile])

        where:
        singleCell | wellLabel || wellCount
        false      | ''        || 0
        true       | ''        || 0
        false      | 'WELL'    || 0
        true       | 'WELL'    || 1
    }

    @Unroll
    void "createSingeCellScript, when single cell is #singleCell and label is #wellLabel, then return empty list"() {
        given:
        DataFile dataFile = createDataFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: singleCell,
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        )

        expect:
        service.createSingeCellScript(dataFile, [:]) == ''

        where:
        singleCell | wellLabel
        false      | ''
        true       | ''
        false      | 'WELL'
    }

    @Unroll
    void "createSingeCellScript, when seqType is single cell and well label given, create script containing expected commands"() {
        given:
        final String OLD_FINAL_PATH = "oldFinalPath"
        final String OLD_PATH = 'oldPath'
        final String OLD_ALL_PATH = "${OLD_PATH}/all"
        final String OLD_WELL_PATH = "${OLD_ALL_PATH}/oldFile"
        final String OLD_MAPPING_PATH = "${OLD_ALL_PATH}/mapping"
        final String OLD_ENTRY = 'oldEntry\tvalue'

        final String NEW_FINAL_PATH = "newFinalPath"
        final String NEW_PATH = 'oldPath'
        final String NEW_ALL_PATH = "${NEW_PATH}/all"
        final String NEW_WELL_PATH = "${NEW_ALL_PATH}/newFile"
        final String NEW_MAPPING_PATH = "${NEW_ALL_PATH}/mapping"
        final String NEW_ENTRY = 'newEntry\tvalue'

        DataFile dataFile = createDataFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'WELL',
                ]),
        )

        service.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile) >> NEW_FINAL_PATH
            1 * getWellAllFileViewByPidPath(dataFile) >> NEW_WELL_PATH
            0 * _
        }

        service.singleCellService = Mock(SingleCellService) {
            1 * singleCellMappingFile(dataFile) >> Paths.get(NEW_MAPPING_PATH)
            1 * mappingEntry(dataFile) >> NEW_ENTRY
            0 * _
        }

        Map<String, ?> oldValues = [
                (DataSwapService.DIRECT_FILE_NAME)            : OLD_FINAL_PATH,
                (DataSwapService.WELL_FILE_NAME)              : OLD_WELL_PATH,
                (DataSwapService.WELL_MAPPING_FILE_NAME)      : Paths.get(OLD_MAPPING_PATH),
                (DataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): OLD_ENTRY,
        ]

        List<String> commands = [
                "rm -f '${OLD_WELL_PATH}'",
                "mkdir -p -m 2750 '${NEW_ALL_PATH}'",
                "ln -s '${NEW_FINAL_PATH}' \\",
                "'${NEW_WELL_PATH}'",
                "sed -i '/${OLD_ENTRY}/d' ${OLD_MAPPING_PATH}",
                "touch '${NEW_MAPPING_PATH}'",
                "echo '${NEW_ENTRY}' >> '${NEW_MAPPING_PATH}'",
                "if [ ! -s '${OLD_MAPPING_PATH}' ]",
                "then",
                "rm '${OLD_MAPPING_PATH}'",
                "fi",
        ]

        when:
        String script = service.createSingeCellScript(dataFile, oldValues)

        List<String> extractCommands = script.split('\n')*.trim().findAll { String line ->
            line && !line.startsWith('#')
        }

        then:
        extractCommands == commands
    }

    void "swapLane, succeed"() {
        given:

        // Services
        Path path = temporaryFolder.newFile().toPath()
        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            fastqcOutputFile(_) >> path
        }
        service.lsdfFilesService = Mock(LsdfFilesService) {
            getFileFinalPath(_) >> path
            getFileViewByPidPath(_) >> path
            getWellAllFileViewByPidPath(_) >> path
        }
        service.seqTrackService = new SeqTrackService()
        Realm realm = createRealm()
        service.configService = configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.root.toString(),
        ])
        configService.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(ProcessingOption.OptionName.REALM_DEFAULT_VALUE) >> realm.name
        }
        service.fileService = Mock(FileService) {
            createOrOverwriteScriptOutputFile(_, _, _) >> temporaryFolder.newFile().toPath()
        }
        CommentService mockedCommendService = Mock(CommentService) {
            saveComment(_, _) >> null
        }
        service.commentService = mockedCommendService
        service.individualService = new IndividualService([
                commentService: mockedCommendService
        ])

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

        Map<String, List<String>> inputInformation = [
                'oldProjectName'        : [falsyLabeledSample.individual.project.name],
                'newProjectName'        : [newIndividual.project.name],
                'oldPid'                : [falsyLabeledSample.individual.pid],
                'newPid'                : [newIndividual.pid],
                'oldSampleTypeName'     : [falsyLabeledSample.sampleType.name],
                'newSampleTypeName'     : [newSampleType.name],
                'oldSeqTypeName'        : [seqTrackWithFalsySample1.seqType.name],
                'newSeqTypeName'        : [newSeqType.name],
                'oldSingleCell'         : [seqTrackWithFalsySample1.seqType.singleCell.toString()],
                'newSingleCell'         : [newSeqType.singleCell.toString()],
                'oldLibraryLayout'      : [seqTrackWithFalsySample1.seqType.libraryLayout.name()],
                'newLibraryLayout'      : [newSeqType.libraryLayout.name()],
                'runName'               : [seqTrackWithFalsySample1.run.name],
                'lane'                  : [
                        seqTrackWithFalsySample1.laneId,
                        seqTrackWithFalsySample2.laneId,
                ],
                'sampleNeedsToBeCreated': [(true).toString()],
        ]

        when:
        service.swapLane(
                inputInformation,
                dataFileMap,
                'newUniqueScriptName',
                new StringBuilder(),
                true,
                temporaryFolder.newFolder() as Path,
                true
        )

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
