package de.dkfz.tbi.otp.ngsdata

//import java.util.*


class LsdfFilesService {

    // will go somewhere
    String basePath = "$ROOT_PATH/project/"


    boolean fileExists(String path) {
        
    }
    
    boolean fileExists(DataFile file) {

        String path = basePath + ""
        
    }
    
    boolean fileExists(long fileId) {

        DataFile file = DataFiele.get(fileId)
        return fileExists(file)
    }
    
    long fileSize(String path) {
        
    }
    
    long fileSize(DataFile file) {
        
    }
    
    long fileSize(long fileId) {
        
    }
    
    Date fileCreationDate(String path) {
        
    }
    
    Date fileCreationDate(DataFile file) {
        
    }
    
    Date fileCreationDate(long fileId) {
        
    }
    
    boolean runInInitialLocation(long runId) {
        
    }
    
    boolean runInFinalLocation(long runId) {
        
    }
    
    
    boolean allFilesExists() {
        
    }
    
    List<String> allFilesNotInMD() {
        
    }
}
