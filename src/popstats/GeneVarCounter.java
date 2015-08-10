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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author desktop
 */
public class GeneVarCounter {
    private static final Logger log = Logger.getLogger(GeneVarCounter.class.getName());
    private String chr = null;
    private BufferedWriter handle;
    private Map<String, Map<String, Map<String, Integer>>> counter = new HashMap<>();
    private final String[] types = {"INS", "DEL", "HIGH", "LOW", "MODERATE", "MISSENSE", "NONSENSE", "SILENT"};
    
    /*
    [output].[population].selection:
        chr gene geneid   INS DEL HIGH    LOW MODERATE    MISSENSE    NONSENSE    SILENT dn/ds
    */
    
    public void countStats(String chr, String gene, String geneid, String type){
        if(!chr.equals(this.chr) && this.chr != null){
            this.printOut(this.chr);
            this.counter = new HashMap<>();
            System.gc();
            this.chr = chr;
        }else if(this.chr == null){
            this.chr = chr;
        }
        
        
        if(!this.counter.containsKey(gene))
            this.counter.put(gene, new HashMap<>());
        
        if(!this.counter.get(gene).containsKey(geneid))
            this.counter.get(gene).put(geneid, new HashMap<>());
        
        if(this.ClassExists(type)){
            if(!this.counter.get(gene).get(geneid).containsKey(type))
                this.counter.get(gene).get(geneid).put(type, 0);
            
            this.counter.get(gene).get(geneid).put(type, this.counter.get(gene).get(geneid).get(type) + 1);
        }       
    }
    
    private boolean ClassExists(String test){
        for(String s : this.types){
            if(s.equals(test))
                return true;
        }
        return false;
    }
    
    public void printOut(String chr){
        try{
            for(String gene : this.counter.keySet()){
                for(String geneid : this.counter.get(gene).keySet()){
                    this.handle.write(chr + "\t" + gene + "\t" + geneid);
                    int dn = 0, ds = 0;
                    for(String t : this.types){
                        if(this.counter.get(gene).get(geneid).containsKey(t)){
                            if(t.equals("NONSENSE") || t.equals("MISSENSE"))
                                dn += this.counter.get(gene).get(geneid).get(t);
                            if(t.equals("SILENT"))
                                ds = this.counter.get(gene).get(geneid).get(t);
                            this.handle.write("\t" + this.counter.get(gene).get(geneid).get(t));
                        }else{
                            this.handle.write("\t" + 0);
                        } 
                    }
                    // This is a super simple, and not accurrate measure of DN/DS ratio
                    // I need to estimate the number of reads that span this, or to use the population frequency
                    // Check Morelli et al. http://www.veterinaryresearch.org/content/44/1/12 
                    if(ds > 0){
                        double dnds = dn /(double) ds;
                        this.handle.write("\t" + dnds + System.lineSeparator());
                    }else{
                        this.handle.write("\tN/A" + System.lineSeparator());
                    }
                }                
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "[GENEVAR] Error writing to outfile!", ex);
        }
    }
    
    public void openHandle(String outfile){
        try{
            this.handle = Files.newBufferedWriter(Paths.get(outfile));
            this.handle.write("chr\tgene\tgeneid\tINS\tDEL\tHIGH\tLOW\tMODERATE\tMISSENSE\tNONSENSE\tSILENT\tdn/ds");
            this.handle.write(System.lineSeparator());
        }catch(IOException ex){
            log.log(Level.SEVERE, "[GENEVAR] Error opening outfile!", ex);
        }
    }
    
    public void closeHandle(){
        try {
            // check if this hasn't already been printed out
            this.printOut(this.chr);
            this.handle.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "[GENEVAR] Error closing handle!", ex);
        }
    }
}
