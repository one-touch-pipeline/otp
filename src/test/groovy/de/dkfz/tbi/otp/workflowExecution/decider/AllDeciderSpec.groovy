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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.testing.gorm.DataTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.*

class AllDeciderSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return []
    }

    private static class TestDecider implements Decider {
        final String workflowName
        private final List<DeciderCreateWorkflowAction> actions

        TestDecider(String workflowName, List<DeciderCreateWorkflowAction> actions) {
            this.workflowName = workflowName
            this.actions = actions
        }

        @Override
        DeciderResult decide(Collection<WorkflowArtefact> artefacts, Map<String, String> params,
                             Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction) {
            return new DeciderResult()
        }

        @Override
        List<DeciderCreateWorkflowAction> getSupportedActions() {
            return actions
        }
    }

    void "deciders contains all expected workflow decider types"() {
        given:
        AllDecider allDecider = new AllDecider()

        expect:
        allDecider.deciders == [
                FastqcDecider,
                PanCancerDecider,
                WgbsDecider,
                RnaAlignmentDecider,
                CellRangerDecider,
                SnvDecider,
                IndelDecider,
                SophiaDecider,
                AceseqDecider,
        ]
    }

    void "getDeciderClassByName returns the decider class for a known simple name"() {
        given:
        AllDecider allDecider = new AllDecider()

        when:
        Class result = allDecider.getDeciderClassByName("CellRangerDecider")

        then:
        result == CellRangerDecider
    }

    void "getDeciderClassByName returns null for an unknown name"() {
        given:
        AllDecider allDecider = new AllDecider()

        when:
        Class result = allDecider.getDeciderClassByName("UnknownDecider")

        then:
        result == null
    }

    void "getSupportedActions returns all DeciderCreateWorkflowAction values"() {
        given:
        AllDecider allDecider = new AllDecider()

        when:
        List<DeciderCreateWorkflowAction> actions = allDecider.supportedActions

        then:
        actions as Set == DeciderCreateWorkflowAction.values() as Set
    }

    void "getAllDeciderActionsMap returns a map from each decider class to its supported actions"() {
        given:
        List<DeciderCreateWorkflowAction> expectedActions = [DeciderCreateWorkflowAction.CREATE_MISSING, DeciderCreateWorkflowAction.SKIP]
        TestDecider deciderBean = new TestDecider("TestWorkflow", expectedActions)

        AllDecider allDecider = new AllDecider()
        allDecider.deciders = [TestDecider]
        allDecider.applicationContext = Mock(ApplicationContext) {
            getBean(TestDecider) >> deciderBean
        }

        when:
        Map<Class, List<DeciderCreateWorkflowAction>> result = allDecider.allDeciderActionsMap

        then:
        result.size() == 1
        result[TestDecider] == expectedActions
    }

    void "getEnabledWorkflowNames returns workflow names collected from all decider beans"() {
        given:
        String workflowName = "TestWorkflow"
        TestDecider deciderBean = new TestDecider(workflowName, [])

        AllDecider allDecider = new AllDecider()
        allDecider.deciders = [TestDecider]
        allDecider.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(TestDecider) >> ["testDecider": deciderBean]
        }

        when:
        Set<String> names = allDecider.enabledWorkflowNames

        then:
        names == [workflowName] as Set
    }

    void "getEnabledWorkflowNames returns empty set when no decider beans are registered"() {
        given:
        AllDecider allDecider = new AllDecider()
        allDecider.deciders = [TestDecider]
        allDecider.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(TestDecider) >> [:]
        }

        when:
        Set<String> names = allDecider.enabledWorkflowNames

        then:
        names.empty
    }

    void "findAlignableSeqTracks returns only seq tracks with a supported seq type"() {
        given:
        SeqType supportedSeqType = Mock(SeqType)
        SeqType unsupportedSeqType = Mock(SeqType)

        SeqTrack matchingSeqTrack = Mock(SeqTrack) { getSeqType() >> supportedSeqType }
        SeqTrack nonMatchingSeqTrack = Mock(SeqTrack) { getSeqType() >> unsupportedSeqType }

        Workflow workflow = Mock(Workflow)
        WorkflowService workflowService = Mock(WorkflowService) {
            getExactlyOneWorkflow(_) >> workflow
            getSupportedSeqTypesOfVersions(_) >> [supportedSeqType]
        }

        AllDecider allDecider = new AllDecider()
        allDecider.workflowService = workflowService

        when:
        Collection<SeqTrack> result = allDecider.findAlignableSeqTracks([matchingSeqTrack, nonMatchingSeqTrack])

        then:
        result == [matchingSeqTrack]
    }

    void "findAlignableSeqTracks returns empty collection for empty input"() {
        given:
        Workflow workflow = Mock(Workflow)
        WorkflowService workflowService = Mock(WorkflowService) {
            getExactlyOneWorkflow(_) >> workflow
            getSupportedSeqTypesOfVersions(_) >> []
        }

        AllDecider allDecider = new AllDecider()
        allDecider.workflowService = workflowService

        when:
        Collection<SeqTrack> result = allDecider.findAlignableSeqTracks([])

        then:
        result.empty
    }

    void "findAlignableSeqTracks returns empty collection when no seq types are supported"() {
        given:
        SeqType seqType = Mock(SeqType)
        SeqTrack seqTrack = Mock(SeqTrack) { getSeqType() >> seqType }

        Workflow workflow = Mock(Workflow)
        WorkflowService workflowService = Mock(WorkflowService) {
            getExactlyOneWorkflow(_) >> workflow
            getSupportedSeqTypesOfVersions(_) >> []
        }

        AllDecider allDecider = new AllDecider()
        allDecider.workflowService = workflowService

        when:
        Collection<SeqTrack> result = allDecider.findAlignableSeqTracks([seqTrack])

        then:
        result.empty
    }
}
