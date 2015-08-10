/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package popstats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author desktop
 */
public class RegulatoryCounter {
    private static final Logger log = Logger.getLogger(RegulatoryCounter.class.getName());
    private BufferedWriter handle;
    private int popfreq = 0;
    
    /*
    [output].[population].regulatory
        chr pos gene    ref alt popfreq vcffreq
    */
    
    public void printOut(String chr, int pos, String gene, String ref, String alt, double vcffreq){
        try {
            this.handle.write(chr + "\t" + pos + "\t" + gene + "\t" + ref + "\t" + alt + "\t" + this.popfreq + "\t" + vcffreq);
            this.handle.write(System.lineSeparator());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "[REGCOUNT] Error writing to open regulatory handle!", ex);
        }
        this.popfreq = 0;
    }
    
    public void incPop(){
        this.popfreq++;
    }
    
    public void closeHandle(){
        try {
            this.handle.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "[REGCOUNT] Error closing handle!", ex);
        }
    }
    
    public void openHandle(String outfile){
        try{
            this.handle = Files.newBufferedWriter(Paths.get(outfile));
        }catch(IOException ex){
            log.log(Level.SEVERE, "[REGCOUNT] Error opening outfile!", ex);
        }
    }
}
