package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.validation.ValidationException
import org.junit.*

class SampleTypePerProjectTests {

    @Test(expected= ValidationException)
    void testUniqueContraint() {
        Project project = new Project(
                        name: "project",
                        dirName: "/dirName/",
                        realmName: "DKFZ"
                        )
        project.save()

        SampleType sampleType = new SampleType(
                        name: "BLOOD"
                        )
        sampleType.save()

        SampleTypePerProject sampleTypePerProject = new SampleTypePerProject(
                        project: project,
                        sampleType: sampleType
                        )
        sampleTypePerProject.save(flush: true)

        SampleTypePerProject sampleTypePerProject1 = new SampleTypePerProject(
                        project: project,
                        sampleType: sampleType
                        )
        sampleTypePerProject1.save(flush: true)
    }
}
