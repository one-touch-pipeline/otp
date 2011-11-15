import grails.util.Environment
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class BootStrap {
    def grailsApplication

    def init = { servletContext ->
        if (Environment.current != Environment.TEST) {
            List<JobExecutionPlan> plans = JobExecutionPlan.findAllByEnabledAndObsoleted(true, false)
            plans.each { JobExecutionPlan plan ->
                AbstractStartJobImpl startJob = grailsApplication.mainContext.getBean(plan.startJob.bean) as AbstractStartJobImpl
                startJob.setJobExecutionPlan(plan)
            }
        }
    }
    def destroy = {
    }

	///////////////////////////////////////////////////////////////////////////
	
	private void safeSave(def obj) {
		//
		// probably will go to separate static class
		// no formal exception, information only
		//
		
		obj.validate()
		if (obj.hasErrors()) {
			println obj.errors
			return
		}

		if (!obj.save(flush: true))
			println "can not save ${obj}"
	}
		
	///////////////////////////////////////////////////////////////////////////
	
}
