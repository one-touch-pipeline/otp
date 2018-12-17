package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([Realm, Project, ProjectCategory, SampleType])
@TestFor(SampleTypePerProject)
class SampleTypePerProjectUnitTests {

    @Test
    void testSaveSampleTypePerProject() {
        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject()
        sampleTypePerProject.project = DomainFactory.createProject()
        sampleTypePerProject.sampleType = new SampleType()
        sampleTypePerProject.category = SampleType.Category.DISEASE
        assertTrue(sampleTypePerProject.validate())
    }


    @Test
    void testSaveSampleTypePerProjectOnlyProject() {
        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject()

        sampleTypePerProject.project = DomainFactory.createProject()
        assertFalse(sampleTypePerProject.validate())
    }


    @Test
    void testSaveSampleTypePerProjectOnlySampleType() {
        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject()

        sampleTypePerProject.sampleType = new SampleType()
        assertFalse(sampleTypePerProject.validate())
    }
}
