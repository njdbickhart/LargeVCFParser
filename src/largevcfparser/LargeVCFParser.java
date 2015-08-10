/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package largevcfparser;

import GetCmdOpt.SimpleModeCmdLineParser;
import StrUtils.StrArray;
import Utils.ConsoleFormat;
import Utils.LogFormat;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author desktop
 */
public class LargeVCFParser {
    private static final Logger log = Logger.getLogger(LargeVCFParser.class.getName());
    private static String version = "0.0.1";
    
    /**
     * @param args the command line arguments
     */    
    public static void main(String[] args) {
        SimpleModeCmdLineParser cmd = GetParser();
        cmd.GetAndCheckMode(args);
        
        setFileHandler(cmd.CurrentMode, args, cmd.GetValue("debug").equals("true"));
        log.log(Level.INFO, "LargeVCFParser\tversion: " + version);
        log.log(Level.INFO, "Mode: " + cmd.CurrentMode);
        log.log(Level.INFO, "Cmd line options: " + StrArray.Join(args, " "));
        
        switch(cmd.CurrentMode){
            case "popstats" :
                
        }
    }
    
    private static String loggerDate(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    private static void setFileHandler(String type, String[] args, boolean debug) {
        // Create a log file and set levels for use with debugger
        FileHandler handler = null;
        ConsoleHandler console = null;
        String datestr = loggerDate();
        try {
            handler = new FileHandler("LVCFParse." + type + "." + datestr + ".%u.%g.log");
            handler.setFormatter(new LogFormat());
            console = new ConsoleHandler();
            console.setFormatter(new ConsoleFormat());
            
            if(debug){
                handler.setLevel(Level.ALL);
                console.setLevel(Level.INFO);
            }else{
                handler.setLevel(Level.INFO);
                console.setLevel(Level.WARNING);
            }
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(LargeVCFParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger mainLog = Logger.getLogger("");
        // This will display all messages, but the handlers should filter the rest
        mainLog.setLevel(Level.ALL);
        for(Handler h : mainLog.getHandlers()){
            mainLog.removeHandler(h);
        }
        mainLog.addHandler(handler);
        mainLog.addHandler(console);
        
        // Log input arguments
        log.log(Level.INFO, "[MAIN] Command line arguments supplied: ");
        log.log(Level.INFO, StrUtils.StrArray.Join(args, " "));
        log.log(Level.INFO, "[MAIN] Debug flag set to: " + debug);
    }
    
    private static SimpleModeCmdLineParser GetParser(){
        String nl = System.lineSeparator();
        SimpleModeCmdLineParser cmd = new SimpleModeCmdLineParser("LargeVCFParser\tA tool to analyze large VCF files" + nl
                + "Version: " + version + nl
            + "Usage: java -jar LargeVCFParser.jar [mode] [mode specific options]" + nl
                + "Modes:" + nl
                + "\tpopstats\tCalculates SNPEff statistics per population" + nl,
                "popstats"
        );
        
        cmd.AddMode("popstats", 
                "LargeVCFParser popstats mode: " + nl
                + "usage: java -jar LargeVCFParser.jar popstats [options] " + nl
                + "Required options: " + nl
                + "\t-p\tTab delimited list of samples with population assignments (samples without pop are labelled [default]) " + nl
                + "\t-v\tInput VCF file " + nl
                + "\t-o\tOutput file base name " + nl + nl
                + "Output files: " + nl
                + "\t[output].[population].stats\tTab delimited summary statistics for each population " + nl
                + "\t[output].[population].pvcf\tTab delimited pseudo-vcf with population frequency estimates " + nl
                + "\t[output].[population].selection\tTab delimited list of genes and dN/dS ratios with functional effect counts " + nl
                + "\t[output].[population].regulatory\tTab delimited listing of upstream gene mutations with gene information " + nl, 
                "p:v:o:d|", 
                "pvo", 
                "pvod", 
                "populations", "vcf", "output", "debug");
        
        return cmd;
    }
}
