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
package de.dkfz.tbi.otp.monitor

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class MonitorOutputCollectorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobExecutionPlan,
                Process,
                ProcessingOption,
        ]
    }

    void "leftShift, added value appear in output"() {
        given:
        String someText = TestCase.createUniqueString()
        MonitorOutputCollector collector = new MonitorOutputCollector()

        when:
        collector << someText

        then:
        someText == collector.output
    }

    void "leftShift, multiple added value appear in the added order in the output"() {
        given:
        String someText1 = TestCase.createUniqueString()
        String someText2 = TestCase.createUniqueString()
        String someText3 = TestCase.createUniqueString()
        MonitorOutputCollector collector = new MonitorOutputCollector()

        String expected = [
                someText1,
                someText2,
                someText3,
        ].join('\n')

        when:
        collector << someText1 << someText2 << someText3

        then:
        expected == collector.output
    }

    void "prefix, every line of text is prefixed with the given prefix"() {
        given:
        String someText1 = TestCase.createUniqueString()
        String someText2 = TestCase.createUniqueString()
        String someText3 = TestCase.createUniqueString()
        String prefix = TestCase.createUniqueString()
        String text = "${someText1}\n${someText2}\n${someText3}"
        String expectedText = "${prefix}${someText1}\n${prefix}${someText2}\n${prefix}${someText3}"

        MonitorOutputCollector collector = new MonitorOutputCollector()

        when:
        String receivedText = collector.prefix(text, prefix)

        then:
        expectedText == receivedText
    }

    @Unroll
    void "objectsToStrings, given list is converted to strings and sorted"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()

        when:
        List<String> sortedListOfStrings = collector.objectsToStrings(list, closure)

        then:
        expectedList == sortedListOfStrings

        where:
        list           | closure                             || expectedList
        [1, 2, 3]      | { it }                              || ['1', '2', '3']
        [3, 2, 1]      | { it }                              || ['1', '2', '3']
        ['abcdef']     | { String it -> it.substring(2, 4) } || ['cd']
        ['ace', 'bcd'] | { String it -> it.substring(1, 2) } || ['c', 'c']
    }

    void "showWorkflow without slot information, check that it is added with newlines before"() {
        given:
        String workflowName = TestCase.createUniqueString()
        MonitorOutputCollector collector = new MonitorOutputCollector()
        String expected = "\n\n${workflowName}\n"

        when:
        collector.showWorkflowOldSystem(workflowName, false)

        then:
        expected == collector.output
    }

    void "showWorkflow with slot information, check that it is added with newlines before"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan()
        DomainFactory.createProcessingOptionLazy([name: ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS, type: plan.name, value: 50])
        DomainFactory.createProcessingOptionLazy([name: ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, type: plan.name, value: 20])

        and: 'jobs already finished'
        3.times {
            DomainFactory.createProcess([jobExecutionPlan: plan, finished: true])
        }

        and: 'running jobs'
        7.times {
            DomainFactory.createProcess([jobExecutionPlan: plan, finished: false])
        }
        String expected = "\n\n${plan.name}\n    Used Slots: 7, Normal priority slots: 30, additional fasttrack slots: 20\n"

        when:
        collector.showWorkflowOldSystem(plan.name)

        then:
        expected == collector.output
    }

    @Unroll
    void "showList, add list name and values of list as expected (#listName)"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()

        when:
        collector.showList(listName, list, closure)
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        expected == output

        where:
        listName    | list            | closure                             || expected
        'list zero' | []              | { it }                              || ''
        'lista'     | [1, 2, 3]       | { it }                              || 'lista (3):\n1\n2\n3\n'
        'listb'     | [3, 2, 1]       | { it }                              || 'listb (3):\n1\n2\n3\n'
        'listc'     | [1, 2, 1, 3, 1] | { it }                              || 'listc (5):\n1\n1\n1\n2\n3\n'
        'listd'     | ['abcdef']      | { String it -> it.substring(2, 4) } || 'listd (1):\ncd\n'
        'liste'     | ['ace', 'bcd']  | { String it -> it.substring(1, 2) } || 'liste (2):\nc\nc\n'
    }

    @Unroll
    void "showUniqueList, add list name and values of list as expected (#listName)"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()

        when:
        collector.showUniqueList(listName, list, closure)
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        expected == output

        where:
        listName    | list            | closure                             || expected
        'list zero' | []              | { it }                              || ''
        'lista'     | [1, 2, 3]       | { it }                              || 'lista (3):\n1\n2\n3\n'
        'listb'     | [3, 2, 1]       | { it }                              || 'listb (3):\n1\n2\n3\n'
        'listc'     | [1, 2, 1, 3, 1] | { it }                              || 'listc (3):\n1  (count: 3)\n2\n3\n'
        'listd'     | ['abcdef']      | { String it -> it.substring(2, 4) } || 'listd (1):\ncd\n'
        'liste'     | ['ace', 'bcd']  | { String it -> it.substring(1, 2) } || 'liste (1):\nc  (count: 2)\n'
    }

    void "showNotTriggered, create list with expected entries"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()
        String expected = "${MonitorOutputCollector.HEADER_NOT_TRIGGERED} (4):\n2\n6\n8\n8\n"

        when:
        collector.showNotTriggered([4, 3, 4, 1], { 2 * it })
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        expected == output
    }

    void "showShouldStart, create list with expected entries"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()
        String expected = "${MonitorOutputCollector.HEADER_SHOULD_START} (4):\n2\n6\n8\n8\n"

        when:
        collector.showShouldStart([4, 3, 4, 1], { 2 * it })
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        expected == output
    }

    void "showWaiting, create list with expected entries"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector()
        String expected = "${MonitorOutputCollector.HEADER_WAITING} (4):\n2\n6\n8\n8\n"

        when:
        collector.showWaiting([4, 3, 4, 1], { 2 * it })
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        expected == output
    }

    @Unroll
    void "showFinished, create list with expected entries (#showFinished)"() {
        given:
        MonitorOutputCollector collector = new MonitorOutputCollector(showFinished)

        when:
        collector.showFinished([4, 3, 4, 1], { 2 * it })
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        expected == output

        where:
        showFinished || expected
        true         || "${MonitorOutputCollector.HEADER_FINISHED} (4):\n2\n6\n8\n8\n"
        false        || ''
    }

    void "showRunning, create list with expected entries"() {
        given:
        String workflowName = TestCase.createUniqueString()
        String errorText = TestCase.createUniqueString()
        Object domainObject = [id: 1]
        MonitorOutputCollector collector = Spy(MonitorOutputCollector)
        String expected = "${MonitorOutputCollector.HEADER_RUNNING} (1):\n1\n\n${errorText}"

        when:
        collector.showRunning(workflowName, [domainObject], { it.id }, { it })
        String output = collector.output.split('\n')*.trim().join('\n')

        then:
        1 * collector.showList(_, _, _)

        then:
        1 * collector.addInfoAboutProcessErrors(_, _, _, _) >> { String workflow, Collection<Object> objects, Closure valueToShow, Closure objectToCheck ->
            assert workflowName == workflow
            collector << errorText
            return
        }

        then:
        expected == output
    }
}
