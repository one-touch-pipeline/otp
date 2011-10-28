
import de.dkfz.tbi.otp.ngsdata.*


class BootStrap {
	
	def init = {
	}		
	

	///////////////////////////////////////////////////////////////////////////
    
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
