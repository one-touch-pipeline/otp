package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([Project, SampleType])
@TestFor(SampleTypePerProject)
class SampleTypePerProjectUnitTests {

    void testSaveSampleTypePerProject() {
        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject()
        sampleTypePerProject.project = new Project()
        sampleTypePerProject.sampleType = new SampleType()
        sampleTypePerProject.category = SampleType.Category.DISEASE
        assertTrue(sampleTypePerProject.validate())
    }


    void testSaveSampleTypePerProjectOnlyProject() {
        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject()

        sampleTypePerProject.project = new Project()
        assertFalse(sampleTypePerProject.validate())
    }


    void testSaveSampleTypePerProjectOnlySampleType() {
        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject()

        sampleTypePerProject.sampleType = new SampleType()
        assertFalse(sampleTypePerProject.validate())
    }
}
