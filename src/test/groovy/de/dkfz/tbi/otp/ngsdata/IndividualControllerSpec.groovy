/*
 * Copyright 2011-2020 The OTP authors
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
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

class IndividualControllerSpec extends Specification implements ControllerUnitTest<IndividualController>, DataTest, UserAndRoles, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                SampleIdentifier,
        ]
    }

    //save is controller method, no domain
    @SuppressWarnings(['ExplicitFlushForSaveRule'])
    void "save, when save is called with valid input, then create the expected objects"() {
        given:
        SampleType sampleType1 = createSampleType()
        SampleType sampleType2 = createSampleType()
        SampleType sampleType3 = createSampleType()
        Project project = createProject()

        controller.individualService = new IndividualService([
                sampleIdentifierService: new SampleIdentifierService(),
        ])

        //work around to get the service in the command object
        controller.params.individualService = controller.individualService

        when:
        controller.request.method = 'POST'
        controller.params.identifier = "Identifier"
        controller.params['individualProject.id'] = project.id
        controller.params.alias = "Alias"
        controller.params.displayedIdentifier = "Displayed Identifier"
        controller.params.internIdentifier = "Intern Identifier"
        controller.params.type = Individual.Type.REAL.toString()

        controller.params["samples[0].sampleType"] = sampleType1.name
        controller.params["samples[0].sampleIdentifiers[0]"] = "SampleIdentifier1"
        controller.params["samples[0].sampleIdentifiers[1]"] = "SampleIdentifier2"

        controller.params["samples[1].sampleType"] = sampleType2.name
        controller.params["samples[1].sampleIdentifiers"] = "SampleIdentifier3"

        controller.params["samples[2].sampleType"] = sampleType3.name
        controller.params["samples[2].sampleIdentifiers"] = ""

        controller.params.checkRedirect = false

        controller.save()

        then:
        controller.flash.message.message == "individual.update.create.success"

        Individual individual = CollectionUtils.exactlyOneElement(Individual.list())
        individual.pid == 'Identifier'
        individual.project == project
        individual.mockPid == 'Alias'
        individual.mockFullName == 'Displayed Identifier'
        individual.internIdentifier == 'Intern Identifier'
        individual.type == Individual.Type.REAL

        controller.response.redirectedUrl == "/individual/insert/${individual.id}"

        Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllBySampleType(sampleType1))
        sample1.individual == individual

        Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllBySampleType(sampleType2))
        sample2.individual == individual

        Sample sample3 = CollectionUtils.exactlyOneElement(Sample.findAllBySampleType(sampleType3))
        sample3.individual == individual

        SampleIdentifier sampleIdentifier1 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("SampleIdentifier1"))
        sampleIdentifier1.sample == sample1

        SampleIdentifier sampleIdentifier2 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("SampleIdentifier2"))
        sampleIdentifier2.sample == sample1

        SampleIdentifier sampleIdentifier3 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("SampleIdentifier3"))
        sampleIdentifier3.sample == sample2

        SampleIdentifier.findAllByName("") == []

        SampleIdentifier.count() == 3
        Sample.count() == 3
    }

    //save is controller method, no domain
    @SuppressWarnings(['ExplicitFlushForSaveRule'])
    void "save, when input for individual is invalid, then do not create any objects"() {
        given:
        Project project = createProject()

        controller.individualService = new IndividualService([
                sampleIdentifierService: new SampleIdentifierService(),
        ])

        //work around to get the service in the command object
        controller.params.individualService = controller.individualService

        when:
        controller.request.method = 'POST'
        controller.params.identifier = ""
        controller.params['project.id'] = project.id
        controller.params.alias = ""
        controller.params.displayedIdentifier = ""
        controller.params.type = Individual.Type.REAL.toString()

        controller.params.checkRedirect = false

        controller.save()

        then:
        controller.flash.message.message == "individual.update.error"

        Individual.list().size() == 0
    }

    //save is controller method, no domain
    @SuppressWarnings(['ExplicitFlushForSaveRule'])
    void "save, when input for sample name is invalid, then show error message"() {
        given:
        Project project = createProject()
        SampleType sampleType = createSampleType()
        SampleIdentifier sampleIdentifier = createSampleIdentifier()

        controller.individualService = new IndividualService([
                sampleIdentifierService: new SampleIdentifierService(),
        ])

        //work around to get the service in the command object
        controller.params.individualService = controller.individualService

        when:
        controller.request.method = 'POST'
        controller.params.identifier = "Identifier"
        controller.params['individualProject.id'] = project.id
        controller.params.alias = "Alias"
        controller.params.displayedIdentifier = "Displayed Identifier"
        controller.params.internIdentifier = "Intern Identifier"
        controller.params.type = Individual.Type.REAL.toString()

        controller.params["samples[0].sampleType"] = sampleType.name
        controller.params["samples[0].sampleIdentifiers[0]"] = sampleIdentifier.name

        controller.params.checkRedirect = false

        controller.save()

        then:
        controller.flash.message.message == "individual.update.create.error"
    }

    void "editNewSampleIdentifier, when it is called with valid input, then do the expected changes"() {
        given:
        Sample sample = createSample()
        SampleIdentifier sampleIdentifier1 = createSampleIdentifier([
                sample: sample,
        ])
        SampleIdentifier sampleIdentifier2 = createSampleIdentifier([
                sample: sample,
        ])
        SampleIdentifier sampleIdentifier3 = createSampleIdentifier([
                sample: sample,
        ])

        controller.sampleIdentifierService = new SampleIdentifierService()

        when:
        controller.request.method = 'POST'

        controller.params['sample.id'] = sample.id

        controller.params["newIdentifiersNames[0]"] = "SampleIdentifier0"
        controller.params["newIdentifiersNames[1]"] = "SampleIdentifier1"
        controller.params["newIdentifiersNames[2]"] = ""
        controller.params["newIdentifiersNames[3]"] = "SampleIdentifier2"

        controller.params["editedSampleIdentifiers[0].sampleIdentifier.id"] = sampleIdentifier1.id
        controller.params["editedSampleIdentifiers[0].name"] = "newName1"

        controller.params["editedSampleIdentifiers[1].sampleIdentifier.id"] = sampleIdentifier2.id
        controller.params["editedSampleIdentifiers[1].name"] = "newName2"

        controller.params["editedSampleIdentifiers[2].sampleIdentifier.id"] = sampleIdentifier3.id
        controller.params["editedSampleIdentifiers[2].delete"] = "true"

        controller.editNewSampleIdentifier()

        then:
        SampleIdentifier.list().size() == 5

        SampleIdentifier newSampleIdentifier0 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("SampleIdentifier0"))
        newSampleIdentifier0.sample == sample

        SampleIdentifier newSampleIdentifier1 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("SampleIdentifier1"))
        newSampleIdentifier1.sample == sample

        SampleIdentifier newSampleIdentifier2 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("SampleIdentifier2"))
        newSampleIdentifier2.sample == sample

        SampleIdentifier newName1 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("newName1"))
        newName1.sample == sample

        SampleIdentifier newName2 = CollectionUtils.exactlyOneElement(SampleIdentifier.findAllByName("newName2"))
        newName2.sample == sample

        controller.flash.message.message == "individual.update.create.success"
    }

    void "editNewSampleIdentifier, when the sample name already exist, then show error message"() {
        given:
        Sample sample = createSample()
        SampleIdentifier sampleIdentifier = createSampleIdentifier()

        controller.sampleIdentifierService = new SampleIdentifierService()

        when:
        controller.request.method = 'POST'

        controller.params['sample.id'] = sample.id
        controller.params["newIdentifiersNames[0]"] = sampleIdentifier

        controller.editNewSampleIdentifier()

        then:
        controller.flash.message.message == "individual.update.error"
    }
}
