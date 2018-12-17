package de.dkfz.tbi.otp.dataprocessing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.ngsdata.Project

class AlignmentDeciderService {

    @Autowired
    ApplicationContext applicationContext

    AlignmentDecider getAlignmentDecider(Project project) {
        return getAlignmentDecider(project.alignmentDeciderBeanName)
    }

    AlignmentDecider getAlignmentDecider(AlignmentDeciderBeanName alignmentDeciderBeanName) {
        return applicationContext.getBean(alignmentDeciderBeanName.beanName, AlignmentDecider)
    }
}
