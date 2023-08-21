/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp

import grails.util.Environment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

import de.dkfz.tbi.otp.config.ConfigService

import java.util.concurrent.ScheduledFuture

@Configuration
@EnableScheduling
class SchedulingConfiguration {

    @Autowired
    ConfigService configService

    @Bean
    TaskScheduler threadPoolTaskScheduler() {
        if (Environment.current == Environment.PRODUCTION || (Environment.current == Environment.DEVELOPMENT && configService.isSchedulerEnabled())) {
            ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler()
            threadPoolTaskScheduler.threadNamePrefix = "ThreadPoolTaskScheduler"
            threadPoolTaskScheduler.poolSize = 10
            threadPoolTaskScheduler.initialize()
            return threadPoolTaskScheduler
        }
        return new TaskScheduler() {
            @Override
            ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
                return null
            }

            @Override
            ScheduledFuture<?> schedule(Runnable task, Date startTime) {
                return null
            }

            @Override
            ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
                return null
            }

            @Override
            ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
                return null
            }

            @Override
            ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
                return null
            }

            @Override
            ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
                return null
            }
        }
    }
}
