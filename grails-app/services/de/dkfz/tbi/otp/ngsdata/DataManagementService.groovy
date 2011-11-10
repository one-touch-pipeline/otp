package de.dkfz.tbi.otp.ngsdata

class DataManagementService {
    
    /**
     * 
     * This method unpacks run if needed 
     * 
     * @param runid
     */
    
    void UnpackRun(long runId) {
        
        
    }
    
    /**
     * 
     * This method checks if all files associated exists
     * two fields if "DataFile" object are filled:
     * fileExists and file system date
     * 
     * @param runId
     */
    
    void CheckIfFilesExists(long runId) {
        
    }
    
    void CalculateMD5Check(long runId) {
        
    }
    
    
    /**
     * 
     * This function compares MD5 checksum in a metadata 
     * with a checksum calculated and located in a file in the run directory
     * 
     * @param runId
     */
    
    void CheckMD5Sum(long runId) {       
    }
    
    
    void moveToFinalLoaction(long runId) {
        
    }
    
    void createViewByPidStructure(long runId) {
        
    } 
    
    

}
