/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package popstats;

import Utils.IntCounter;
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
public class GenStatsCounter {
    // Basic stats are organized by chromosome, then by stat string
    private static final Logger log = Logger.getLogger(GenStatsCounter.class.getName());
    private final Map<String, Map<String, Integer>> Variants = new HashMap<>();
    private final Map<String, Map<String, Integer>> VariantClass = new HashMap<>();
    private final Map<String, Map<String, Integer>> VariantEffects = new HashMap<>();
    private final Map<String, Map<String, Integer>> TSTV = new HashMap<>();
    private final Map<String, IntCounter> transitions = new HashMap<>();
    private final Map<String, IntCounter> transversions = new HashMap<>();
    private final String[] VariantTypes = {"SNP", "INDEL"};
    private final String[] VariantClasses = {"HIGH", "LOW", "MODERATE"};
    private final String[] VariantEffectTypes = {"MISSENSE", "NONSENSE", "SILENT", "3_prime_UTR_variant", "frameshift_variant", "5_prime_UTR_variant", "upstream_gene_variant", "downstream_gene_variant", "splice_region_variant"};
    /*
    [output].[population].stats :
        chr snps    INDEL HIGH LOW MODERATE   MISSENSE    NONSENSE    SILENT  3_prime_UTR frameshift  5_prime_UTR downstream splice_acceptor  splice_donator  transitions transversions Ts/Tv
                
    */            
    
    public void incrementStats(String chr, String VariantType, String VariantClass, String VariantEffectType, String Ref, String Alt){
        // Initialize hash with chr if it doesn't exist
        if(!this.Variants.containsKey(chr))
            this.Variants.put(chr, new HashMap<>());
        if(!this.VariantEffects.containsKey(chr))
            this.VariantEffects.put(chr, new HashMap<>());
        if(!this.VariantClass.containsKey(chr))
            this.VariantClass.put(chr, new HashMap<>());
        if(!this.TSTV.containsKey(chr))
            this.TSTV.put(chr, new HashMap<>());
        
        if(!this.transitions.containsKey(chr))
            this.transitions.put(chr, new IntCounter());
        if(!this.transversions.containsKey(chr))
            this.transversions.put(chr, new IntCounter());
        
        if(!this.TSTV.get(chr).containsKey("Transition"))
            this.TSTV.get(chr).put("Transition", 0);
        if(!this.TSTV.get(chr).containsKey("Tranversion"))
            this.TSTV.get(chr).put("Transversion", 0);
        
        // Start counting!
        if(this.ClassExists(VariantType, VariantTypes)){
            if(!this.Variants.get(chr).containsKey(VariantType))
                this.Variants.get(chr).put(VariantType, 0);
            this.Variants.get(chr).put(VariantType, this.Variants.get(chr).get(VariantType) +1);
        }
        
        if(this.ClassExists(VariantClass, VariantClasses)){
            if(!this.VariantClass.get(chr).containsKey(VariantClass))
                this.VariantClass.get(chr).put(VariantClass, 0);
            this.VariantClass.get(chr).put(VariantClass, this.VariantClass.get(chr).get(VariantClass) +1);
        }
        
        if(this.ClassExists(VariantEffectType, VariantEffectTypes)){
            if(!this.VariantEffects.get(chr).containsKey(VariantEffectType))
                this.VariantEffects.get(chr).put(VariantEffectType, 0);
            this.VariantEffects.get(chr).put(VariantEffectType, this.VariantEffects.get(chr).get(VariantEffectType) +1);
        }
        
        if(VariantType.equals("SNP")){
            if(this.TransitionCheck(Ref, Alt)){
                //this.TSTV.get(chr).put("Transition", this.TSTV.get(chr).get("Transition") + 1);
                this.transitions.get(chr).increment();
            }else{
                //this.TSTV.get(chr).put("Transversion", this.TSTV.get(chr).get("Transversion") + 1);
                this.transversions.get(chr).increment();
            }
        }
    }
    
    /*
    [output].[population].stats :
        chr snps    INDEL HIGH LOW MODERATE   MISSENSE    NONSENSE    SILENT  3_prime_UTR frameshift  5_prime_UTR downstream splice_acceptor  splice_donator  transitions transversions Ts/Tv
                
    */
    public void PrintOut(String FileName){
        try(BufferedWriter output = Files.newBufferedWriter(Paths.get(FileName))){
            output.write("chr\tSNP\tINDEL\tHIGH\tLOW\tMODERATE\tMISSENSE\tNONSENSE\tSILENT\t3_prime_UTR\tframeshift\t5_prime_UTR\tdownstream\tsplice_acceptor\tsplice_donator\tTransitions\tTransversions\tTS/TV" + System.lineSeparator());
            for(String chr : this.TSTV.keySet()){
                output.write(chr);
                for(String Vtype : this.VariantTypes){
                    if(this.Variants.containsKey(chr)){
                        if(this.Variants.get(chr).containsKey(Vtype)){
                            output.write("\t" + this.Variants.get(chr).get(Vtype));
                        }else{
                            output.write("\t" + 0);
                        }
                    }else{
                        output.write("\tN/A");
                    }
                }
                
                for(String Vtype : this.VariantClasses){
                    if(this.VariantClass.containsKey(chr)){
                        if(this.VariantClass.get(chr).containsKey(Vtype)){
                            output.write("\t" + this.VariantClass.get(chr).get(Vtype));
                        }else{
                            output.write("\t" + 0);
                        }
                    }else{
                        output.write("\tN/A");
                    }
                }
                
                for(String Vtype : this.VariantEffectTypes){
                    if(this.VariantEffects.containsKey(chr)){
                        if(this.VariantEffects.get(chr).containsKey(Vtype)){
                            output.write("\t" + this.VariantEffects.get(chr).get(Vtype));
                        }else{
                            output.write("\t" + 0);
                        }
                    }else{
                        output.write("\tN/A");
                    }
                }
                
                if(this.TSTV.containsKey(chr)){
                    //int ts = this.TSTV.get(chr).get("Transition");
                    //int tv = this.TSTV.get(chr).get("Transversion");
                    int ts = this.transitions.get(chr).getCount();
                    int tv = this.transversions.get(chr).getCount();
                    double tstv = (double)ts / (double)tv;
                    
                    output.write("\t" + ts + "\t" + tv + "\t" + tstv + System.lineSeparator());
                }else{
                    output.write("\tN/A\tN/A\tN/A" + System.lineSeparator());
                }
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "[GENSTATS] Error writing to output!", ex);
        }
    }
    
    private boolean TransitionCheck(String Ref, String Alt){
        boolean RefPurine = (Ref.equals("A") || Ref.equals("G"));
        boolean AltPurine = (Alt.equals("A") || Alt.equals("G"));
        
        return ((RefPurine && AltPurine) || (!RefPurine && !AltPurine));
    }
    
    private boolean ClassExists(String test, String[] comparators){
        for(String s : comparators){
            if(s.equals(test))
                return true;
        }
        return false;
    }
}
