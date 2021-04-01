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
package de.dkfz.tbi.otp.egaSubmission

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.submissions.ega.EgaSubmissionFactory
import de.dkfz.tbi.otp.ngsdata.*

class EgaFileContentServiceSpec extends Specification implements EgaSubmissionFactory, IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                BamFileSubmissionObject,
                DataFileSubmissionObject,
                EgaSubmission,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
        ]
    }

    @Unroll
    void "createKeyForFastq, when dataFileSubmissionObject is given, then create expected key #expectedKey"() {
        given:
        DataFileSubmissionObject dataFileSubmissionObject = createDataFileSubmissionObject([
                dataFile: createDataFile([
                        seqTrack: createSeqTrack([
                                seqType              : createSeqType([
                                        displayName  : seqTypeDisplayName,
                                        libraryLayout: libraryLayout,
                                ]),
                                run                  : createRun([
                                        seqPlatform: createSeqPlatform([
                                                name                 : seqTypeDisplayName,
                                                seqPlatformModelLabel: seqPlatformModelLabelName ? createSeqPlatformModelLabel([
                                                        name: seqPlatformModelLabelName,
                                                ]) : null,
                                        ]),
                                ]),
                                libraryPreparationKit: libraryPreparationKitName ? createLibraryPreparationKit([
                                        name: libraryPreparationKitName,
                                ]) : null,
                        ]),
                ]),
        ])

        when:
        String key = new EgaFileContentService().createKeyForFastq(dataFileSubmissionObject)

        then:
        key == expectedKey

        where:
        seqTypeDisplayName | libraryLayout        | seqPlatformName | seqPlatformModelLabelName | libraryPreparationKitName || expectedKey
        'seqType'          | SequencingReadType.SINGLE | 'platform' | 'model' | 'kit' || 'seqType-SINGLE-seqType-model-kit'
        'seqType'          | SequencingReadType.SINGLE | 'platform' | 'model' | null  || 'seqType-SINGLE-seqType-model-unspecified'
        'seqType'          | SequencingReadType.SINGLE | 'platform' | null    | 'kit' || 'seqType-SINGLE-seqType-unspecified-kit'
        'seqType'          | SequencingReadType.PAIRED | 'platform' | 'model' | 'kit' || 'seqType-PAIRED-seqType-model-kit'
    }

    @Unroll
    void "createKeyForBamFile, when bamFileSubmissionObject is given, then create expected key #expectedKey"() {
        given:
        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject([
                bamFile: createBamFile([
                        seqTracks: [
                                createSeqTrack([
                                        seqType              : createSeqType([
                                                displayName  : seqTypeDisplayName,
                                                libraryLayout: libraryLayout,
                                        ]),
                                        run                  : createRun([
                                                seqPlatform: createSeqPlatform([
                                                        name                 : seqTypeDisplayName,
                                                        seqPlatformModelLabel: seqPlatformModelLabelName ? createSeqPlatformModelLabel([
                                                                name: seqPlatformModelLabelName,
                                                        ]) : null,
                                                ]),
                                        ]),
                                        libraryPreparationKit: libraryPreparationKitName ? createLibraryPreparationKit([
                                                name: libraryPreparationKitName,
                                        ]) : null,
                                ]),
                        ] as Set
                ]),
        ])

        when:
        String key = new EgaFileContentService().createKeyForBamFile(bamFileSubmissionObject)

        then:
        key == expectedKey

        where:
        seqTypeDisplayName | libraryLayout        | seqPlatformName | seqPlatformModelLabelName | libraryPreparationKitName || expectedKey
        'seqType'          | SequencingReadType.SINGLE | 'platform' | 'model' | 'kit' || 'seqType-SINGLE-seqType-model-kit'
        'seqType'          | SequencingReadType.SINGLE | 'platform' | 'model' | null  || 'seqType-SINGLE-seqType-model-unspecified'
        'seqType'          | SequencingReadType.SINGLE | 'platform' | null    | 'kit' || 'seqType-SINGLE-seqType-unspecified-kit'
        'seqType'          | SequencingReadType.PAIRED | 'platform' | 'model' | 'kit' || 'seqType-PAIRED-seqType-model-kit'
    }

    void "createKeyForBamFile, when bamFileSubmissionObject with multiple seqplatform is given, then create expected key #expectedKey"() {
        given:
        SeqType seqType = createSeqType([
                displayName  : 'seqtype',
                libraryLayout: SequencingReadType.PAIRED,
        ])
        Sample sample = createSample()
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit([
                name: 'library'
        ])

        List<SeqTrack> seqTracks = [
                ['seqPlatform2', 'model3'],
                ['seqPlatform1', 'model1'],
                ['seqPlatform2', null],
                ['seqPlatform2', 'model1'],
                ['seqPlatform1', 'model2'],
        ].collect {
            createSeqTrack([
                    seqType              : seqType,
                    sample               : sample,
                    run                  : createRun([
                            seqPlatform: createSeqPlatform([
                                    name                 : it[0],
                                    seqPlatformModelLabel: it[1] ? createSeqPlatformModelLabel([
                                            name: it[1],
                                    ]) : null,
                            ]),
                    ]),
                    libraryPreparationKit: libraryPreparationKit,
            ])
        }

        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject([
                bamFile: createBamFile([
                        seqTracks: seqTracks as Set
                ]),
        ])

        when:
        String key = new EgaFileContentService().createKeyForBamFile(bamFileSubmissionObject)

        then:
        key == 'seqtype-PAIRED-seqPlatform1-model1-seqPlatform1-model2-seqPlatform2-model1-seqPlatform2-model3-seqPlatform2-unspecified-library'
    }

    void "createKeyForBamFile, when bamFileSubmissionObject with multiple lib_prep_kits is given, then create expected key #expectedKey"() {
        given:
        SeqType seqType = createSeqType([
                displayName  : 'seqtype',
                libraryLayout: SequencingReadType.PAIRED,
        ])
        Sample sample = createSample()
        SeqPlatform seqPlatform = createSeqPlatform([
                name                 : 'platform',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model',
                ]),
        ])

        List<SeqTrack> seqTracks = [
                'lib5',
                'lib2',
                null,
                'lib3',
        ].collect {
            createSeqTrack([
                    seqType              : seqType,
                    sample               : sample,
                    run                  : createRun([
                            seqPlatform: seqPlatform,
                    ]),
                    libraryPreparationKit: it ? createLibraryPreparationKit([
                            name: it,
                    ]) : null,
            ])
        }

        BamFileSubmissionObject bamFileSubmissionObject = createBamFileSubmissionObject([
                bamFile: createBamFile([
                        seqTracks: seqTracks as Set
                ]),
        ])

        when:
        String key = new EgaFileContentService().createKeyForBamFile(bamFileSubmissionObject)

        then:
        key == 'seqtype-PAIRED-platform-model-lib2-lib3-lib5-unspecified'
    }

    void "createSingleFastqFileMapping, when ega submission is given, then create expectedMap of file names and file content"() {
        given:
        Sample sample = createSample()
        SeqType seqTypeSingle1 = createSeqType([
                displayName  : 'seqtype1',
                libraryLayout: SequencingReadType.SINGLE,
        ])
        SeqType seqTypeSingle2 = createSeqType([
                displayName  : 'seqtype2',
                libraryLayout: SequencingReadType.SINGLE,
        ])
        SeqType seqTypePaired = createSeqType([
                displayName  : 'seqtype3',
                libraryLayout: SequencingReadType.PAIRED,
        ])
        SeqPlatform seqPlatform1 = createSeqPlatform([
                name                 : 'platform1',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model1',
                ]),
        ])
        SeqPlatform seqPlatform2 = createSeqPlatform([
                name                 : 'platform2',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model2',
                ]),
        ])
        LibraryPreparationKit libraryPreparationKit1 = createLibraryPreparationKit([
                name: 'library1'
        ])
        LibraryPreparationKit libraryPreparationKit2 = createLibraryPreparationKit([
                name: 'library2'
        ])

        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject([
                egaAliasName: 'sampleAlias1',
        ])
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject([
                egaAliasName: 'sampleAlias2',
        ])

        List<DataFileSubmissionObject> dataFileSubmissionObjects = [
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit1, 'fileAlias-111-1', sampleSubmissionObject1,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit1, 'fileAlias-111-2', sampleSubmissionObject1,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit1, 'fileAlias-111-3', sampleSubmissionObject1,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit2, 'fileAlias-112-1', sampleSubmissionObject2,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit2, 'fileAlias-112-2', sampleSubmissionObject2,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit2, 'fileAlias-112-3', sampleSubmissionObject2,],
                [seqTypeSingle1, seqPlatform2, libraryPreparationKit1, 'fileAlias-121-1', sampleSubmissionObject1,],
                [seqTypeSingle2, seqPlatform1, libraryPreparationKit1, 'fileAlias-211-1', sampleSubmissionObject1,],
                [seqTypeSingle2, seqPlatform2, libraryPreparationKit2, 'fileAlias-222-1', sampleSubmissionObject2,],
        ].collect {
            createDataFileSubmissionObject([
                    dataFile              : createDataFile([
                            seqTrack: createSeqTrack([
                                    sample               : sample,
                                    seqType              : it[0],
                                    run                  : createRun([
                                            seqPlatform: it[1],
                                    ]),
                                    libraryPreparationKit: it[2],
                            ]),
                    ]),
                    egaAliasName          : it[3],
                    sampleSubmissionObject: it[4],
            ])
        }

        dataFileSubmissionObjects.add(createDataFileSubmissionObject([
                dataFile: createDataFile([
                        seqTrack: createSeqTrack([
                                sample : sample,
                                seqType: seqTypePaired,
                        ]),
                ]),
        ]))

        EgaSubmission egaSubmission = createEgaSubmission([
                dataFilesToSubmit: dataFileSubmissionObjects as Set,
        ])
        Map expectedMap = [
                ('runs-fastqs-seqtype1-SINGLE-platform1-model1-library1.csv'): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-111-1,,
sampleAlias1,fileAlias-111-2,,
sampleAlias1,fileAlias-111-3,,
''',
                ('runs-fastqs-seqtype1-SINGLE-platform1-model1-library2.csv'): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-112-1,,
sampleAlias2,fileAlias-112-2,,
sampleAlias2,fileAlias-112-3,,
''',
                ('runs-fastqs-seqtype1-SINGLE-platform2-model2-library1.csv'): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-121-1,,
''',
                ('runs-fastqs-seqtype2-SINGLE-platform1-model1-library1.csv'): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-211-1,,
''',
                ('runs-fastqs-seqtype2-SINGLE-platform2-model2-library2.csv'): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-222-1,,
''',
        ]

        when:
        Map map = new EgaFileContentService().createSingleFastqFileMapping(egaSubmission)

        then:
        TestCase.assertContainSame(map, expectedMap)
    }

    void "createPairedFastqFileMapping, when ega submission is given, then create expectedMap of file names and file content"() {
        given:
        Sample sample = createSample()
        SeqType seqTypePaired1 = createSeqType([
                displayName  : 'seqtype1',
                libraryLayout: SequencingReadType.PAIRED,
        ])
        SeqType seqTypePaired2 = createSeqType([
                displayName  : 'seqtype2',
                libraryLayout: SequencingReadType.PAIRED,
        ])
        SeqType seqTypeSingle = createSeqType([
                displayName  : 'seqtype3',
                libraryLayout: SequencingReadType.SINGLE,
        ])
        SeqPlatform seqPlatform1 = createSeqPlatform([
                name                 : 'platform1',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model1',
                ]),
        ])
        SeqPlatform seqPlatform2 = createSeqPlatform([
                name                 : 'platform2',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model2',
                ]),
        ])
        LibraryPreparationKit libraryPreparationKit1 = createLibraryPreparationKit([
                name: 'library1'
        ])
        LibraryPreparationKit libraryPreparationKit2 = createLibraryPreparationKit([
                name: 'library2'
        ])

        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject([
                egaAliasName: 'sampleAlias1',
        ])
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject([
                egaAliasName: 'sampleAlias2',
        ])

        List<DataFileSubmissionObject> dataFileSubmissionObjects = [
                [seqTypePaired1, seqPlatform1, libraryPreparationKit1, 'fileAlias-111-1-r1', 'fileAlias-111-1-r2', sampleSubmissionObject1,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit1, 'fileAlias-111-2-r1', 'fileAlias-111-2-r2', sampleSubmissionObject1,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit1, 'fileAlias-111-3-r1', 'fileAlias-111-3-r2', sampleSubmissionObject1,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit2, 'fileAlias-112-1-r1', 'fileAlias-112-1-r2', sampleSubmissionObject2,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit2, 'fileAlias-112-2-r1', 'fileAlias-112-2-r2', sampleSubmissionObject2,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit2, 'fileAlias-112-3-r1', 'fileAlias-112-3-r2', sampleSubmissionObject2,],
                [seqTypePaired1, seqPlatform2, libraryPreparationKit1, 'fileAlias-121-1-r1', 'fileAlias-121-1-r2', sampleSubmissionObject1,],
                [seqTypePaired2, seqPlatform1, libraryPreparationKit1, 'fileAlias-211-1-r1', 'fileAlias-211-1-r2', sampleSubmissionObject1,],
                [seqTypePaired2, seqPlatform2, libraryPreparationKit2, 'fileAlias-222-1-r1', 'fileAlias-222-1-r2', sampleSubmissionObject2,],
        ].collectMany {
            SeqTrack seqTrack = createSeqTrack([
                    sample               : sample,
                    seqType              : it[0],
                    run                  : createRun([
                            seqPlatform: it[1],
                    ]),
                    libraryPreparationKit: it[2],
            ])
            [
                    createDataFileSubmissionObject([
                            dataFile              : createDataFile([
                                    seqTrack: seqTrack,
                            ]),
                            egaAliasName          : it[3],
                            sampleSubmissionObject: it[5],
                    ]),
                    createDataFileSubmissionObject([
                            dataFile              : createDataFile([
                                    seqTrack: seqTrack,
                            ]),
                            egaAliasName          : it[4],
                            sampleSubmissionObject: it[5],
                    ]),
            ]
        }
        dataFileSubmissionObjects.add(createDataFileSubmissionObject([
                dataFile: createDataFile([
                        seqTrack: createSeqTrack([
                                sample : sample,
                                seqType: seqTypeSingle,
                        ]),
                ]),
        ]))

        EgaSubmission egaSubmission = createEgaSubmission([
                dataFilesToSubmit: dataFileSubmissionObjects as Set,
        ])

        Map expectedMap = [
                ('runs-fastqs-seqtype1-PAIRED-platform1-model1-library1.csv'): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias1,fileAlias-111-1-r1,,,fileAlias-111-1-r2,,
sampleAlias1,fileAlias-111-2-r1,,,fileAlias-111-2-r2,,
sampleAlias1,fileAlias-111-3-r1,,,fileAlias-111-3-r2,,
''',
                ('runs-fastqs-seqtype1-PAIRED-platform1-model1-library2.csv'): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias2,fileAlias-112-1-r1,,,fileAlias-112-1-r2,,
sampleAlias2,fileAlias-112-2-r1,,,fileAlias-112-2-r2,,
sampleAlias2,fileAlias-112-3-r1,,,fileAlias-112-3-r2,,
''',
                ('runs-fastqs-seqtype1-PAIRED-platform2-model2-library1.csv'): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias1,fileAlias-121-1-r1,,,fileAlias-121-1-r2,,
''',
                ('runs-fastqs-seqtype2-PAIRED-platform1-model1-library1.csv'): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias1,fileAlias-211-1-r1,,,fileAlias-211-1-r2,,
''',
                ('runs-fastqs-seqtype2-PAIRED-platform2-model2-library2.csv'): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias2,fileAlias-222-1-r1,,,fileAlias-222-1-r2,,
''',
        ]

        when:
        Map map = new EgaFileContentService().createPairedFastqFileMapping(egaSubmission)

        then:
        TestCase.assertContainSame(map, expectedMap)
    }

    void "createBamFileMapping, when ega submission is given, then create expectedMap of file names and file content"() {
        given:
        SeqType seqTypeSingle1 = createSeqType([
                displayName  : 'seqtypeSingle1',
                libraryLayout: SequencingReadType.SINGLE,
        ])
        SeqType seqTypeSingle2 = createSeqType([
                displayName  : 'seqtypeSingle2',
                libraryLayout: SequencingReadType.SINGLE,
        ])
        SeqType seqTypePaired1 = createSeqType([
                displayName  : 'seqtypePaired1',
                libraryLayout: SequencingReadType.PAIRED,
        ])
        SeqType seqTypePaired2 = createSeqType([
                displayName  : 'seqtypePaired2',
                libraryLayout: SequencingReadType.PAIRED,
        ])

        SeqPlatform seqPlatform1 = createSeqPlatform([
                name                 : 'platform1',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model1',
                ]),
        ])
        SeqPlatform seqPlatform2 = createSeqPlatform([
                name                 : 'platform2',
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        name: 'model2',
                ]),
        ])

        LibraryPreparationKit libraryPreparationKit1 = createLibraryPreparationKit([
                name: 'library1'
        ])
        LibraryPreparationKit libraryPreparationKit2 = createLibraryPreparationKit([
                name: 'library2'
        ])

        SampleSubmissionObject sampleSubmissionObject1 = createSampleSubmissionObject([
                egaAliasName: 'sampleAlias1',
        ])
        SampleSubmissionObject sampleSubmissionObject2 = createSampleSubmissionObject([
                egaAliasName: 'sampleAlias2',
        ])

        List<BamFileSubmissionObject> bamFileSubmissionObjects = [
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit1, 'fileAlias-s111-1', sampleSubmissionObject1,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit1, 'fileAlias-s111-2', sampleSubmissionObject1,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit1, 'fileAlias-s111-3', sampleSubmissionObject1,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit2, 'fileAlias-s112-1', sampleSubmissionObject2,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit2, 'fileAlias-s112-2', sampleSubmissionObject2,],
                [seqTypeSingle1, seqPlatform1, libraryPreparationKit2, 'fileAlias-s112-3', sampleSubmissionObject2,],
                [seqTypeSingle1, seqPlatform2, libraryPreparationKit1, 'fileAlias-s121-1', sampleSubmissionObject1,],
                [seqTypeSingle2, seqPlatform1, libraryPreparationKit1, 'fileAlias-s211-1', sampleSubmissionObject1,],
                [seqTypeSingle2, seqPlatform2, libraryPreparationKit2, 'fileAlias-s222-1', sampleSubmissionObject2,],

                [seqTypePaired1, seqPlatform1, libraryPreparationKit1, 'fileAlias-p111-1', sampleSubmissionObject1,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit1, 'fileAlias-p111-2', sampleSubmissionObject1,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit1, 'fileAlias-p111-3', sampleSubmissionObject1,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit2, 'fileAlias-p112-1', sampleSubmissionObject2,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit2, 'fileAlias-p112-2', sampleSubmissionObject2,],
                [seqTypePaired1, seqPlatform1, libraryPreparationKit2, 'fileAlias-p112-3', sampleSubmissionObject2,],
                [seqTypePaired1, seqPlatform2, libraryPreparationKit1, 'fileAlias-p121-1', sampleSubmissionObject1,],
                [seqTypePaired2, seqPlatform1, libraryPreparationKit1, 'fileAlias-p211-1', sampleSubmissionObject1,],
                [seqTypePaired2, seqPlatform2, libraryPreparationKit2, 'fileAlias-p222-1', sampleSubmissionObject2,],
        ].collect {
            createBamFileSubmissionObject([
                    bamFile               : createBamFile([
                            seqTracks: [
                                    createSeqTrack([
                                            seqType              : it[0],
                                            run                  : createRun([
                                                    seqPlatform: it[1],
                                            ]),
                                            libraryPreparationKit: it[2],
                                    ])
                            ] as Set
                    ]),
                    egaAliasName          : it[3],
                    sampleSubmissionObject: it[4],
            ])
        }

        EgaSubmission egaSubmission = createEgaSubmission([
                bamFilesToSubmit: bamFileSubmissionObjects as Set,
        ])

        Map expectedMap = [
                ('runs-bams-seqtypePaired1-PAIRED-platform1-model1-library1.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-p111-1,,
sampleAlias1,fileAlias-p111-2,,
sampleAlias1,fileAlias-p111-3,,
''',
                ('runs-bams-seqtypePaired1-PAIRED-platform1-model1-library2.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-p112-1,,
sampleAlias2,fileAlias-p112-2,,
sampleAlias2,fileAlias-p112-3,,
''',
                ('runs-bams-seqtypePaired1-PAIRED-platform2-model2-library1.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-p121-1,,
''',
                ('runs-bams-seqtypePaired2-PAIRED-platform1-model1-library1.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-p211-1,,
''',
                ('runs-bams-seqtypePaired2-PAIRED-platform2-model2-library2.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-p222-1,,
''',
                ('runs-bams-seqtypeSingle1-SINGLE-platform1-model1-library1.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-s111-1,,
sampleAlias1,fileAlias-s111-2,,
sampleAlias1,fileAlias-s111-3,,
''',
                ('runs-bams-seqtypeSingle1-SINGLE-platform1-model1-library2.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-s112-1,,
sampleAlias2,fileAlias-s112-2,,
sampleAlias2,fileAlias-s112-3,,
''',
                ('runs-bams-seqtypeSingle1-SINGLE-platform2-model2-library1.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-s121-1,,
''',
                ('runs-bams-seqtypeSingle2-SINGLE-platform1-model1-library1.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-s211-1,,
''',
                ('runs-bams-seqtypeSingle2-SINGLE-platform2-model2-library2.csv'): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-s222-1,,
''',]

        when:
        Map map = new EgaFileContentService().createBamFileMapping(egaSubmission)

        then:
        TestCase.assertContainSame(map, expectedMap)
    }
}
