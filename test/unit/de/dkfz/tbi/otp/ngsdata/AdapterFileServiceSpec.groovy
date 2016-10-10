package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
        AdapterFile,
        ProcessingOption,
])
class AdapterFileServiceSpec extends Specification {

    AdapterFileService service = new AdapterFileService(
            processingOptionService: new ProcessingOptionService()
    )

    @Rule
    TemporaryFolder temporaryFolder

    void "test create adapter file using AdapterFile"(){
        given:
        String ADAPTER_FILE ="AdapterFile"

        when:
        service.createAdapterFile(ADAPTER_FILE)

        then:
        AdapterFile.findByFileName(ADAPTER_FILE)
    }

    void "test create adapter file using null"(){
        when:
        service.createAdapterFile(null)
        then:
        thrown(AssertionError)
    }

    void "test findByFileName, when name exists, returns object"() {
        given:
        String name = 'name'
        AdapterFile expect = DomainFactory.createAdapterFile(fileName: name)
        DomainFactory.createAdapterFile()

        when:
        AdapterFile found = service.findByFileName(name)

        then:
        expect == found
    }

    void "test findByFileName, when name not exists, returns null"() {
        given:
        String name = 'name'
        DomainFactory.createAdapterFile()

        when:
        AdapterFile found = service.findByFileName(name)

        then:
        null == found
    }

    void "test findByFileName, when name is null, throws exception"() {
        when:
        service.findByFileName(null)

        then:
        AssertionError error = thrown()
        error.message.contains('fileName')
    }

    void "test findByFileName, when name is empty, throws exception"() {
        when:
        service.findByFileName('')

        then:
        AssertionError error = thrown()
        error.message.contains('fileName')
    }

    void "test baseDirectory, when processing option is set to valid absolute path, returns path"() {
        File expectedFile = TestCase.uniqueNonExistentPath
        DomainFactory.createProcessingOptionBaseAdapterFile(expectedFile.path)

        when:
        File file = service.baseDirectory()

        then:
        expectedFile == file
    }

    void "test baseDirectory, when processing option is not set to valid absolute path, throws exception"() {
        String invalidPath = './invalid/path'
        DomainFactory.createProcessingOptionBaseAdapterFile(invalidPath)

        when:
        service.baseDirectory()

        then:
        AssertionError e = thrown()
        e.message.contains(invalidPath)
    }

    void "test baseDirectory, when processing option is not set, throws exception"() {
        when:
        service.baseDirectory()

        then:
        AssertionError e = thrown()
        e.message.contains('Collection contains 0 elements. Expected 1')
    }

    void "test fullPath, when adapter file exists in otp and not on file system and readability is tested, throws exception"() {
        given:
        DomainFactory.createProcessingOptionBaseAdapterFile()
        AdapterFile adapterFile = DomainFactory.createAdapterFile()

        when:
        service.fullPath(adapterFile)

        then:
        AssertionError e = thrown()
        e.message.contains("file.canRead")
    }

    void "test fullPath, when adapter file exists in otp and not on file system and readability is not tested, returns path"() {
        given:
        ProcessingOption option = DomainFactory.createProcessingOptionBaseAdapterFile()
        AdapterFile adapterFile = DomainFactory.createAdapterFile()

        when:
        File file = service.fullPath(adapterFile, false)

        then:
        new File(option.value, adapterFile.fileName) == file
    }

    @Unroll
    void "test fullPath, when adapter file exists in otp and on file system and readability is tested (#readability), returns path"() {
        given:
        File directory = temporaryFolder.newFolder()
        DomainFactory.createProcessingOptionBaseAdapterFile(directory.path)
        AdapterFile adapterFile = DomainFactory.createAdapterFile()
        new File(directory, adapterFile.fileName) << 'something'

        when:
        File file = service.fullPath(adapterFile, readability)

        then:
        new File(directory, adapterFile.fileName) == file

        where:
        readability << [
                true,
                false,
        ]
    }


    void "test fullPath, when adapter file is null, throws exception"() {
        when:
        service.fullPath(null)

        then:
        AssertionError e = thrown()
    }

}
