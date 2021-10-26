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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper

class LsdfFileServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqImportInstance,
                Realm,
                SeqTrack,
        ]
    }

    LsdfFilesService service

    void setup() {
        service = new LsdfFilesService()
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    void "test ensureFileIsReadableAndNotEmpty"() {
        given:
        File file = tempFolder.newFile()
        file << "content"

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        noExceptionThrown()
    }

    void "test ensureFileIsReadableAndNotEmpty, when path is not absolute, should fail"() {
        given:
        File file = new File("testFile.txt")

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)absolute/
    }

    void "test ensureFileIsReadableAndNotEmpty, when does not exist, should fail"() {
        given:
        //file must be absolute to make sure that the test fails the 'exists?' assertion
        File file = new File(tempFolder.newFolder(), "testFile.txt")

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)on local filesystem is not accessible or does not exist\./
    }

    void "test ensureFileIsReadableAndNotEmpty, when file is not a regular file, should fail"() {
        given:
        File file = tempFolder.newFolder()

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isRegularFile/
    }

    void "test ensureFileIsReadableAndNotEmpty, when file isn't readable, should fail"() {
        given:
        File file = tempFolder.newFile()
        file << "content"
        file.readable = false

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isReadable/

        cleanup:
        file.readable = true
    }

    void "test ensureFileIsReadableAndNotEmpty, when file is empty, should fail"() {
        given:
        File file = tempFolder.newFile()

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)size/
    }

    void "test deleteFilesRecursive"() {
        given:
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        List<File> files = [
                tempFolder.newFolder(),
                tempFolder.newFile(),
        ]

        when:
        service.deleteFilesRecursive(realm, files)

        then:
        files.each {
            assert !it.exists()
        }
    }

    void "test deleteFilesRecursive, when filesOrDirectories is empty"() {
        given:
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    assert false: 'Should not be called'
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        expect:
        service.deleteFilesRecursive(realm, [])
    }

    void "test deleteFilesRecursive, when realm is null, should fail"() {
        when:
        service.deleteFilesRecursive(null, [tempFolder.newFolder()])

        then:
        def e = thrown(AssertionError)
        e.message.contains('realm may not be null.')
    }

    void "test deleteFilesRecursive, when filesOrDirectories is null, should fail"() {
        given:
        Realm realm = DomainFactory.createRealm()

        when:
        service.deleteFilesRecursive(realm, null)

        then:
        def e = thrown(AssertionError)
        e.message.contains('filesOrDirectories may not be null.')
    }

    void "test deleteFilesRecursive, when deletion fails, should fail"() {
        given:
        final String MESSAGE = HelperUtils.uniqueString
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    assert false: MESSAGE
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        List<File> files = [
                tempFolder.newFolder(),
                tempFolder.newFile(),
        ]

        when:
        service.deleteFilesRecursive(realm, files)

        then:
        def e = thrown(AssertionError)
        e.message.contains(MESSAGE)
        files.each {
            assert it.exists()
        }
    }

    private Map<String, ?> setUpViewByPidTests(String antiBody, String well, String sampleType, String sampleTypeDirPart) {
        new TestConfigService()

        SeqType seqType = createSeqType([
                hasAntibodyTarget: antiBody as boolean,
                singleCell       : well as boolean,
        ])
        AntibodyTarget antibodyTarget = antiBody ? createAntibodyTarget([
                name: antiBody,
        ]) : null
        DataFile dataFile = createDataFile([
                seqTrack: createSeqTrack([
                        seqType            : seqType,
                        sample             : createSample([
                                sampleType: createSampleType([
                                        name: sampleType,
                                ]),
                        ]),
                        antibodyTarget     : antibodyTarget,
                        singleCellWellLabel: well,
                ]),
        ])

        String expected = [
                dataFile.project.projectSequencingDirectory.path,
                seqType.dirName,
                "view-by-pid",
                dataFile.individual.pid,
                sampleTypeDirPart,
                seqType.libraryLayoutDirName,
                "run${dataFile.run.name}",
                dataFile.fileType.vbpPath,
                dataFile.vbpFileName,
        ].join('/')

        return [
                dataFile: dataFile,
                expected: expected,
        ]
    }

    @Unroll
    void "getFileViewByPidPath, when antibody is '#antiBody' and single cell well is '#well', then path part is '#sampleTypePart'"() {
        given:
        Map<String, ?> data = setUpViewByPidTests(antiBody, well, sampleType, sampleTypePart)

        when:
        String path = service.getFileViewByPidPath(data.dataFile)

        then:
        data.expected == path

        where:
        sampleType | antiBody    | well   || sampleTypePart
        'control'  | null        | null   || 'control'
        'Control'  | null        | null   || 'control'
        'CONTROL'  | null        | null   || 'control'
        'CONTROL'  | 'anti-body' | null   || 'control-anti-body'
        'CONTROL'  | null        | 'well' || 'control/well'
        'CONTROL'  | 'anti-body' | 'well' || 'control-anti-body/well'
    }

    @Unroll
    void "getWellAllFileViewByPidPath, when antibody is '#antiBody' and single cell well is '#well', then path part is '#sampleTypePart'"() {
        given:
        Map<String, ?> data = setUpViewByPidTests(antiBody, well, sampleType, sampleTypePart)

        when:
        String path = service.getWellAllFileViewByPidPath(data.dataFile)

        then:
        data.expected == path

        where:
        sampleType | antiBody    | well   || sampleTypePart
        'CONTROL'  | null        | null   || 'control'
        'CONTROL'  | 'anti-body' | null   || 'control-anti-body'
        'CONTROL'  | null        | 'well' || 'control/0_all'
        'CONTROL'  | 'anti-body' | 'well' || 'control-anti-body/0_all'
    }

    void "getFileViewByPidPath, when datafile is an unaligned single cell bam file, then return expected path"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        DataFile dataFile = createDataFile([
                seqTrack    : null,
                alignmentLog: DomainFactory.createAlignmentLog([
                        seqTrack: seqTrack,
                ]),
        ])

        String expected = [
                seqTrack.project.projectSequencingDirectory.path,
                seqTrack.seqType.dirName,
                "view-by-pid",
                seqTrack.individual.pid,
                seqTrack.sampleType.dirName,
                seqTrack.seqType.libraryLayoutDirName,
                "run${seqTrack.run.name}",
                dataFile.fileType.vbpPath,
                dataFile.vbpFileName,
        ].join('/')

        when:
        String path = service.getFileViewByPidPath(dataFile)

        then:
        expected == path
    }
}
