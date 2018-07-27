package de.dkfz.tbi.otp.utils

import org.springframework.beans.*
import org.springframework.context.*
import org.springframework.stereotype.*

@Component
class StaticApplicationContextWrapper implements ApplicationContextAware {

    static ApplicationContext context

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext
    }

}
