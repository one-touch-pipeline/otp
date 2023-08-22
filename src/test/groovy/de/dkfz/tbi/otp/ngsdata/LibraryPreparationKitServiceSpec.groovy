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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.TempDir

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class LibraryPreparationKitServiceSpec extends AbstractMetadataFieldsServiceSpec<LibraryPreparationKit> implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                LibraryPreparationKit,
        ]
    }

    @TempDir
    Path tempDir

    LibraryPreparationKitService libraryPreparationKitService = new LibraryPreparationKitService()

    final static String ADAPTER_FILE = "/file.fa"
    final static String ADAPTER_SEQUENCE = "ATGC"

    def setup() {
        properties = [
                adapterFile                     : ADAPTER_FILE,
                reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
        ]
        otherProperties = [
                adapterFile                     : ADAPTER_FILE,
                reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
        ]
    }

    void "test createLibraryPreparationKit succeeds"() {
        given:
        service.create(OTHER_NAME,
                [
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                ]
        )

        when:
        service.create(name,
                [
                        adapterFile                     : adapterFile,
                        reverseComplementAdapterSequence: adapterSequence,
                ]
        )

        then:
        exactlyOneElement(LibraryPreparationKit.findAllByNameAndAdapterFileAndReverseComplementAdapterSequence(
                OTHER_NAME, ADAPTER_FILE, ADAPTER_SEQUENCE))

        where:
        name  | adapterFile  | adapterSequence
        NAME  | ADAPTER_FILE | ADAPTER_SEQUENCE
        NAME  | null         | ADAPTER_SEQUENCE
        NAME  | ADAPTER_FILE | null
    }

    void "test createLibraryPreparationKit fails with null as argument"() {
        when:
        service.create(null,
                [
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                ]
        )

        then:
        thrown(AssertionError)
    }

    void "test createLibraryPreparationKit fails with existing names"() {
        given:
        service.create(NAME,
                [
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                ]
        )

        when:
        service.create(NAME,
                [
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                ]
        )

        then:
        thrown(AssertionError)
    }

    void "test createLibraryPreparationKit fails with invalid property"() {
        when:
        service.create(NAME,
                [
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                        invalidProperty                 : "invalid",
                ]
        )

        then:
        thrown(AssertionError)
    }

    void "test addAdapterFileToLibraryPreparationKit succeeds"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterFileToLibraryPreparationKit(kit, ADAPTER_FILE)

        then:
        kit.adapterFile == ADAPTER_FILE
    }

    void "test addAdapterFileToLibraryPreparationKit fails"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterFileToLibraryPreparationKit(kit, adapterFile)

        then:
        thrown(AssertionError)

        where:
        adapterFile << [null, ""]
    }

    void "test addAdapterSequenceToLibraryPreparationKit"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterSequenceToLibraryPreparationKit(kit, ADAPTER_SEQUENCE)

        then:
        kit.reverseComplementAdapterSequence == ADAPTER_SEQUENCE
    }

    void "test addAdapterSequenceToLibraryPreparationKit fails"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterSequenceToLibraryPreparationKit(kit, adapterSequence)

        then:
        thrown(AssertionError)

        where:
        adapterSequence << [null, ""]
    }

    void "getAdapterFileContentToRender, returns adapter file content as String"() {
        given:
        setupServiceForAdapterFileReading()

        Path adapterFile = tempDir.resolve("${HelperUtils.uniqueString}_adapterfile.fa")
        adapterFile.text = content

        LibraryPreparationKit kit = createLibraryPreparationKit(adapterFile: adapterFile.toAbsolutePath())
        Realm realm = new Realm()

        service.fileService = new FileService()
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        expect:
        content == service.getAdapterFileContentToRender(kit, realm)

        where:
        content << ["some content", ""]
    }

    void "getAdapterFileContentToRender, throws assertion exception if file exceeds limit"() {
        given:
        setupServiceForAdapterFileReading()

        Path adapterFile = tempDir.resolve("${HelperUtils.uniqueString}_adapterfile.fa")
        new RandomAccessFile(adapterFile.toFile(), "rw").length = 5242880L + 1L

        LibraryPreparationKit kit = createLibraryPreparationKit(adapterFile: adapterFile.toAbsolutePath())
        Realm realm = new Realm()

        service.fileService = new FileService()
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        service.getAdapterFileContentToRender(kit, realm)

        then:
        AssertionError e = thrown(AssertionError)
        e.message =~ "Adapter file is too large to be displayed in the GUI"
    }

    void "getAdapterFileContentToRender, throws exception when file is not readable"() {
        given:
        setupServiceForAdapterFileReading()

        String content = "some content"
        Path adapterFile = tempDir.resolve("${HelperUtils.uniqueString}_adapterfile.fa")
        adapterFile.text = content
        Files.setPosixFilePermissions(adapterFile, [] as Set)

        LibraryPreparationKit kit = createLibraryPreparationKit(adapterFile: adapterFile.toAbsolutePath())
        Realm realm = new Realm()

        service.fileService = new FileService()
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        service.getAdapterFileContentToRender(kit, realm)

        then:
        thrown(AssertionError)
    }

    void setupServiceForAdapterFileReading() {
        service.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem() >> { return FileSystems.default }
        }
        service.fileService = new FileService()
    }

    @Override
    protected AbstractMetadataFieldsService getService() {
        return libraryPreparationKitService
    }
}
