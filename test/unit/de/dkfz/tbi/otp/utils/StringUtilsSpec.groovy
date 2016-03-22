package de.dkfz.tbi.otp.utils

import spock.lang.*

class StringUtilsSpec extends Specification {

    void 'extractDistinguishingCharacter, when only one string is provided, throws IllegalArgumentException'() {
        when:
        StringUtils.extractDistinguishingCharacter(['abc'])

        then:
        thrown(IllegalArgumentException)
    }

    void 'extractDistinguishingCharacter, when strings have different lengths, returns null'() {
        expect:
        StringUtils.extractDistinguishingCharacter(['abc', 'abbc']) == null
    }

    void 'extractDistinguishingCharacter, when strings are empty, returns null'() {
        expect:
        StringUtils.extractDistinguishingCharacter(['', '']) == null
    }

    void 'extractDistinguishingCharacter, when more characters are different, returns null'(List<String> strings) {
        expect:
        StringUtils.extractDistinguishingCharacter(strings) == null

        where:
        strings | _
        ['abc', 'ade'] | _
        ['abd', 'acd', 'ace'] | _
    }

    void 'extractDistinguishingCharacter, when strings are equal, returns null'(List<String> strings) {
        expect:
        StringUtils.extractDistinguishingCharacter(strings) == null

        where:
        strings | _
        ['abc', 'abc'] | _
        ['abd', 'acd', 'acd'] | _
        ['abd', 'acd', 'abd'] | _
    }

    void 'extractDistinguishingCharacter, when strings differ in exactly one character, returns correct mapping'(Map<String, Character> expectedMapping) {
        given:
        List<String> strings = expectedMapping.keySet().toList()

        expect:
        StringUtils.extractDistinguishingCharacter(strings) == expectedMapping

        where:
        expectedMapping                | _
        [abc: 'a', xbc: 'x']           | _
        [abc: 'b', ayc: 'y']           | _
        [abc: 'c', abz: 'z']           | _
        [abc: 'a', ubc: 'u', xbc: 'x'] | _
        [abc: 'b', avc: 'v', ayc: 'y'] | _
        [abc: 'c', abw: 'w', abz: 'z'] | _
    }
}
