package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*

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
