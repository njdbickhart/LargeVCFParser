/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class MergeTabVCFs {
    private static final Logger log = Logger.getLogger(MergeTabVCFs.class.getName());
    private final Path firstFile;
    private final Path secondFile;
    private final Path outputFile;
    
    public MergeTabVCFs(SimpleModeCmdLineParser cmd){
        firstFile = Paths.get(cmd.GetValue("first"));
        secondFile = Paths.get(cmd.GetValue("second"));
        outputFile = Paths.get(cmd.GetValue("output"));
    }
    
    public void run(){
        // Generate condensed list of variant sites
    }
}
