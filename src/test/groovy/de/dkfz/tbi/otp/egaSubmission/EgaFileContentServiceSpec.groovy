/*
 * Copyright 2011-2026 The OTP authors
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
        return [
                BamFileSubmissionObject,
                RawSequenceFileSubmissionObject,
                EgaSubmission,
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
        ]
    }

    @Unroll
    void "createKeyForFastq, when submission object is given, then create expected key #expectedKeyTemplate"() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = seqPlatformModelLabelName ? createSeqPlatformModelLabel([
                name: seqPlatformModelLabelName,
        ]) : null
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitName ? createLibraryPreparationKit([
                name: libraryPreparationKitName,
        ]) : null

        RawSequenceFileSubmissionObject submissionObject = createRawSequenceFileSubmissionObject([
                sequenceFile: createFastqFile([
                        seqTrack: createSeqTrack([
                                seqType              : createSeqType([
                                        displayName  : seqTypeDisplayName,
                                        libraryLayout: libraryLayout,
                                ]),
                                run                  : createRun([
                                        seqPlatform: createSeqPlatform([
                                                name                 : seqPlatformName,
                                                seqPlatformModelLabel: seqPlatformModelLabel,
                                        ]),
                                ]),
                                libraryPreparationKit: libraryPreparationKit,
                        ]),
                ]),
        ])

        String expectedKey = expectedKeyTemplate.
                replaceAll('#1', seqPlatformModelLabel?.id?.toString() ?: '').
                replaceAll('#2', libraryPreparationKit?.id?.toString() ?: '')

        when:
        String key = new EgaFileContentService().createKeyForFastq(submissionObject)

        then:
        key == expectedKey

        where:
        seqTypeDisplayName | libraryLayout             | seqPlatformName | seqPlatformModelLabelName | libraryPreparationKitName      || expectedKeyTemplate
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit'                          || 'seqType-SINGLE-platform-model-#1-kit-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit with space'               || 'seqType-SINGLE-platform-model-#1-kit_with_space-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit (round brackets)'         || 'seqType-SINGLE-platform-model-#1-kit__round_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit [corner brackets]'        || 'seqType-SINGLE-platform-model-#1-kit__corner_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit <angle brackets>'         || 'seqType-SINGLE-platform-model-#1-kit__angle_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit {curly brackets}'         || 'seqType-SINGLE-platform-model-#1-kit__curly_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit special: !§$%&/?,;:*~"\'' || 'seqType-SINGLE-platform-model-#1-kit_special________________-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | '& special at start'           || 'seqType-SINGLE-platform-model-#1-__special_at_start-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'special & in % the $ middle'  || 'seqType-SINGLE-platform-model-#1-special___in___the___middle-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | null                           || 'seqType-SINGLE-platform-model-#1-unspecified'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | null                      | 'kit'                          || 'seqType-SINGLE-platform-unspecified-kit-#2'
        'seqType'          | SequencingReadType.PAIRED | 'platform'      | 'model'                   | 'kit'                          || 'seqType-PAIRED-platform-model-#1-kit-#2'
    }

    @Unroll
    void "createKeyForBamFile, when bamFileSubmissionObject is given, then create expected key #expectedKeyTemplate"() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = seqPlatformModelLabelName ? createSeqPlatformModelLabel([
                name: seqPlatformModelLabelName,
        ]) : null
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitName ? createLibraryPreparationKit([
                name: libraryPreparationKitName,
        ]) : null

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
                                                        name                 : seqPlatformName,
                                                        seqPlatformModelLabel: seqPlatformModelLabel,
                                                ]),
                                        ]),
                                        libraryPreparationKit: libraryPreparationKit,
                                ]),
                        ] as Set
                ]),
        ])

        String expectedKey = expectedKeyTemplate.
                replaceAll('#1', seqPlatformModelLabel?.id?.toString() ?: '').
                replaceAll('#2', libraryPreparationKit?.id?.toString() ?: '')

        when:
        String key = new EgaFileContentService().createKeyForBamFile(bamFileSubmissionObject)

        then:
        key == expectedKey

        where:
        seqTypeDisplayName | libraryLayout             | seqPlatformName | seqPlatformModelLabelName | libraryPreparationKitName      || expectedKeyTemplate
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit'                          || 'seqType-SINGLE-platform-model-#1-kit-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit with space'               || 'seqType-SINGLE-platform-model-#1-kit_with_space-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit (round brackets)'         || 'seqType-SINGLE-platform-model-#1-kit__round_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit [corner brackets]'        || 'seqType-SINGLE-platform-model-#1-kit__corner_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit <angle brackets>'         || 'seqType-SINGLE-platform-model-#1-kit__angle_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit {curly brackets}'         || 'seqType-SINGLE-platform-model-#1-kit__curly_brackets_-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'kit special: !§$%&/?,;:*~"\'' || 'seqType-SINGLE-platform-model-#1-kit_special________________-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | '& special at start'           || 'seqType-SINGLE-platform-model-#1-__special_at_start-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | 'special & in % the $ middle'  || 'seqType-SINGLE-platform-model-#1-special___in___the___middle-#2'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | 'model'                   | null                           || 'seqType-SINGLE-platform-model-#1-unspecified'
        'seqType'          | SequencingReadType.SINGLE | 'platform'      | null                      | 'kit'                          || 'seqType-SINGLE-platform-unspecified-kit-#2'
        'seqType'          | SequencingReadType.PAIRED | 'platform'      | 'model'                   | 'kit'                          || 'seqType-PAIRED-platform-model-#1-kit-#2'
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

        String seqPlatformNames = [
                "seqPlatform1-model1-${seqTracks[1].seqPlatform.seqPlatformModelLabel.id}",
                "seqPlatform1-model2-${seqTracks[4].seqPlatform.seqPlatformModelLabel.id}",
                "seqPlatform2-model1-${seqTracks[3].seqPlatform.seqPlatformModelLabel.id}",
                "seqPlatform2-model3-${seqTracks[0].seqPlatform.seqPlatformModelLabel.id}",
                "seqPlatform2-unspecified",
        ].join('-')
        String libPrepKitNames = "library-${libraryPreparationKit.id}"
        String expected = "seqtype-PAIRED-${seqPlatformNames}-${libPrepKitNames}"

        when:
        String key = new EgaFileContentService().createKeyForBamFile(bamFileSubmissionObject)

        then:
        key == expected
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

        String seqPlatformNames = "platform-model-${seqPlatform.seqPlatformModelLabel.id}"
        String libPrepKitNames = [
                "lib2-${seqTracks[1].libraryPreparationKit.id}",
                "lib3-${seqTracks[3].libraryPreparationKit.id}",
                "lib5-${seqTracks[0].libraryPreparationKit.id}",
                "unspecified",
        ].join('-')
        String expected = "seqtype-PAIRED-${seqPlatformNames}-${libPrepKitNames}"

        when:
        String key = new EgaFileContentService().createKeyForBamFile(bamFileSubmissionObject)

        then:
        key == expected
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

        List<RawSequenceFileSubmissionObject> submissionObjects = [
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
            createRawSequenceFileSubmissionObject([
                    sequenceFile          : createFastqFile([
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

        submissionObjects.add(createRawSequenceFileSubmissionObject([
                sequenceFile: createFastqFile([
                        seqTrack: createSeqTrack([
                                sample : sample,
                                seqType: seqTypePaired,
                        ]),
                ]),
        ]))

        EgaSubmission egaSubmission = createEgaSubmission([
                rawSequenceFilesToSubmit: submissionObjects as Set,
        ])
        Map expectedMap = [
                ("runs-fastqs-seqtype1-SINGLE-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-111-1,,
sampleAlias1,fileAlias-111-2,,
sampleAlias1,fileAlias-111-3,,
''',
                ("runs-fastqs-seqtype1-SINGLE-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-112-1,,
sampleAlias2,fileAlias-112-2,,
sampleAlias2,fileAlias-112-3,,
''',
                ("runs-fastqs-seqtype1-SINGLE-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-121-1,,
''',
                ("runs-fastqs-seqtype2-SINGLE-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,Fastq File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-211-1,,
''',
                ("runs-fastqs-seqtype2-SINGLE-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
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

        List<RawSequenceFileSubmissionObject> submissionObjects = [
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
                    createRawSequenceFileSubmissionObject([
                            sequenceFile          : createFastqFile([
                                    seqTrack: seqTrack,
                            ]),
                            egaAliasName          : it[3],
                            sampleSubmissionObject: it[5],
                    ]),
                    createRawSequenceFileSubmissionObject([
                            sequenceFile          : createFastqFile([
                                    seqTrack: seqTrack,
                            ]),
                            egaAliasName          : it[4],
                            sampleSubmissionObject: it[5],
                    ]),
            ]
        }
        submissionObjects.add(createRawSequenceFileSubmissionObject([
                sequenceFile: createFastqFile([
                        seqTrack: createSeqTrack([
                                sample : sample,
                                seqType: seqTypeSingle,
                        ]),
                ]),
        ]))

        EgaSubmission egaSubmission = createEgaSubmission([
                rawSequenceFilesToSubmit: submissionObjects as Set,
        ])

        Map expectedMap = [
                ("runs-fastqs-seqtype1-PAIRED-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias1,fileAlias-111-1-r1,,,fileAlias-111-1-r2,,
sampleAlias1,fileAlias-111-2-r1,,,fileAlias-111-2-r2,,
sampleAlias1,fileAlias-111-3-r1,,,fileAlias-111-3-r2,,
''',
                ("runs-fastqs-seqtype1-PAIRED-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias2,fileAlias-112-1-r1,,,fileAlias-112-1-r2,,
sampleAlias2,fileAlias-112-2-r1,,,fileAlias-112-2-r2,,
sampleAlias2,fileAlias-112-3-r1,,,fileAlias-112-3-r2,,
''',
                ("runs-fastqs-seqtype1-PAIRED-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias1,fileAlias-121-1-r1,,,fileAlias-121-1-r2,,
''',
                ("runs-fastqs-seqtype2-PAIRED-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,First Fastq File,First Checksum,First Unencrypted checksum,Second Fastq File,Second Checksum,Second Unencrypted checksum
sampleAlias1,fileAlias-211-1-r1,,,fileAlias-211-1-r2,,
''',
                ("runs-fastqs-seqtype2-PAIRED-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
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
                ("runs-bams-seqtypePaired1-PAIRED-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-p111-1,,
sampleAlias1,fileAlias-p111-2,,
sampleAlias1,fileAlias-p111-3,,
''',
                ("runs-bams-seqtypePaired1-PAIRED-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-p112-1,,
sampleAlias2,fileAlias-p112-2,,
sampleAlias2,fileAlias-p112-3,,
''',
                ("runs-bams-seqtypePaired1-PAIRED-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-p121-1,,
''',
                ("runs-bams-seqtypePaired2-PAIRED-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-p211-1,,
''',
                ("runs-bams-seqtypePaired2-PAIRED-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-p222-1,,
''',
                ("runs-bams-seqtypeSingle1-SINGLE-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-s111-1,,
sampleAlias1,fileAlias-s111-2,,
sampleAlias1,fileAlias-s111-3,,
''',
                ("runs-bams-seqtypeSingle1-SINGLE-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-s112-1,,
sampleAlias2,fileAlias-s112-2,,
sampleAlias2,fileAlias-s112-3,,
''',
                ("runs-bams-seqtypeSingle1-SINGLE-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-s121-1,,
''',
                ("runs-bams-seqtypeSingle2-SINGLE-platform1-model1-${seqPlatform1.seqPlatformModelLabel.id}-library1-${libraryPreparationKit1.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias1,fileAlias-s211-1,,
''',
                ("runs-bams-seqtypeSingle2-SINGLE-platform2-model2-${seqPlatform2.seqPlatformModelLabel.id}-library2-${libraryPreparationKit2.id}.csv".toString()): '''\
Sample alias,BAM File,Checksum,Unencrypted checksum
sampleAlias2,fileAlias-s222-1,,
''',]

        when:
        Map map = new EgaFileContentService().createBamFileMapping(egaSubmission)

        then:
        TestCase.assertContainSame(map, expectedMap)
    }
}
