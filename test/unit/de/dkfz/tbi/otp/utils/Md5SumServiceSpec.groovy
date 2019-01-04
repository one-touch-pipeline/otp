package de.dkfz.tbi.otp.utils

import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

class Md5SumServiceSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    @Unroll
    void "extractMd5Sum, if valid md5sum, return md5sum value"() {
        given:
        Path md5sumFile = temporaryFolder.newFile().toPath()
        md5sumFile.text = prefix + md5sum + postfix

        when:
        String extractedMd5Sum = new Md5SumService().extractMd5Sum(md5sumFile)

        then:
        extractedMd5Sum == md5sum

        where:
        prefix | postfix
        ''     | ''
        ''     | '\n'
        '\n'   | ''
        '\n'   | '\n'

        md5sum = HelperUtils.randomMd5sum
    }


    void "extractMd5Sum, if md5sum file is null, then throw an assertion"() {
        when:
        new Md5SumService().extractMd5Sum(null)

        then:
        AssertionError e = thrown()
        e.message.contains('Parameter md5Sum is null')
    }

    void "extractMd5Sum, if md5sum file is relative, then throw an assertion"() {
        when:
        new Md5SumService().extractMd5Sum(Paths.get('tmp'))

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not absolute/
    }

    void "extractMd5Sum, if md5sum file does not exist, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.delete()

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' does not exist/
    }

    void "extractMd5Sum, if md5sum file is not a regular file, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFolder()

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not a file/
    }

    void "extractMd5Sum, if md5sum file is not readable, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.text = HelperUtils.randomMd5sum
        file.setReadable(false)

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not readable/
    }

    void "extractMd5Sum, if md5sum file is empty, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.text = ''

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is empty/
    }

    @Unroll
    void "extractMd5Sum, if md5sum file is #name (#input), then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.text = input

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' has not the correct form/

        where:
        name           | input
        'to short'     | '123'
        'to long'      | '1234567890123456789012345678901234567890'
        'invalid char' | '12rtz'
        'with space'   | '123 456'
    }
}
