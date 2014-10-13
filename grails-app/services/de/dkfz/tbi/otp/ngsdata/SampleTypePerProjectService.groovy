package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

class SampleTypePerProjectService {

    /**
     * @return SampleTypePerProject for a given Project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public List<SampleTypePerProject> findByProject(Project project) {
        return SampleTypePerProject.findAllByProject(project)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SampleTypePerProject createOrUpdate(Project project, SampleType sampleType, SampleType.Category category) {
        SampleTypePerProject sampleTypePerProject = SampleTypePerProject.findByProjectAndSampleType(project, sampleType)
        if (sampleTypePerProject) {
            sampleTypePerProject.category = category
        } else {
            sampleTypePerProject = new SampleTypePerProject(
                            project: project,
                            sampleType: sampleType,
                            category: category
                            )
        }
        sampleTypePerProject.save()
        return sampleTypePerProject
    }
}
