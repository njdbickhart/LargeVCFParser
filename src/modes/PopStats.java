/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
import Utils.IntCounter;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext.Type;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import popstats.GenStatsCounter;
import popstats.GeneVarCounter;
import popstats.RegulatoryCounter;

/**
 *
 * @author desktop
 */
public class PopStats {
    private static final Logger log = Logger.getLogger(PopStats.class.getName());
    private File VcfFile;
    private Path PopFile;
    private final String outbase;
    private final Map<String, String> annotationConverter = new HashMap<>();
    
    /* Output file formats
        [output].[population].stats :
        chr snps    INS DEL HIGH LOW MODERATE   MISSENSE    NONSENSE    SILENT  3_prime_UTR frameshift  5_prime_UTR downstream splice_acceptor  splice_donator  transitions transversions Ts/Tv
    
        [output].[population].pvcf
        chr pos dbsnp   ref alt qual filter info    format  popfreq vcffreq
    
        [output].[population].selection:
        chr gene   geneid INS DEL HIGH    LOW MODERATE    MISSENSE    NONSENSE    SILENT dn/ds
    
        [output].[population].regulatory
        chr pos gene    ref alt popfreq vcffreq 
    */
    
    public PopStats(SimpleModeCmdLineParser cmd){
        this.VcfFile = new File(cmd.GetValue("vcf"));
        if(!this.VcfFile.canRead()){
            log.log(Level.SEVERE, "[POPSTATS] Could not find vcf file! Exiting...");
            System.exit(-1);
        }
        this.PopFile = Paths.get(cmd.GetValue("populations"));
        if(!this.PopFile.toFile().canRead()){
            log.log(Level.SEVERE, "[POPSTATS] Could not open population file! Exiting...");
            System.exit(-1);
        }
        this.outbase = cmd.GetValue("output");
        this.annotationConverter.put("stop_gained", "NONSENSE");
        this.annotationConverter.put("stop_lost", "MISSENSE");
        this.annotationConverter.put("start_lost", "MISSENSE");
        this.annotationConverter.put("inframe_deletion", "DEL");
        this.annotationConverter.put("inframe_insertion", "INS");
        this.annotationConverter.put("disruptive_inframe_deletion", "DEL");
        this.annotationConverter.put("disruptive_inframe_insertion", "INS");
        this.annotationConverter.put("missense_variant", "MISSENSE");
        this.annotationConverter.put("synonymous_variant", "SILENT");
    }
    
    public void run(){
        // Read in population stats 
        // Lookup tool and list of populations to consider
        Map<String, String> popLookup = new HashMap<>();
        Set<String> pops = new HashSet<>();
        Map<String, Integer> popcounts = new HashMap<>();
        
        
        try(BufferedReader input = Files.newBufferedReader(PopFile, Charset.defaultCharset())){
            String line = null;
            while((line = input.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                if(segs.length != 2)
                    throw new Exception("population file not properly formatted! " + line);
                popLookup.put(segs[0], segs[1]);
                pops.add(segs[1]);
                
                if(!popcounts.containsKey(segs[1]))
                    popcounts.put(segs[1], 0);
                // NOTE: + 2 assumes that the population is diploid!!
                popcounts.put(segs[1], popcounts.get(segs[1]) + 2);
            }
        }catch(Exception ex){
            log.log(Level.SEVERE, "[POPSTATS] Error reading population file!", ex);
        }
        
        // Now, create container classes in hashes
        final Map<String, GenStatsCounter> genStats = pops.stream()
                .collect(Collectors.toMap(s -> s, s -> {return new GenStatsCounter();}));
        
        final Map<String, GeneVarCounter> geneCount = pops.stream()
                .collect(Collectors.toMap(s -> s, s -> {
                    GeneVarCounter temp = new GeneVarCounter();
                    temp.openHandle(outbase + "." + s + ".selection");
                    return temp;}));
        
        final Map<String, RegulatoryCounter> regCount = pops.stream()
                .collect(Collectors.toMap(s -> s, s -> {
                    RegulatoryCounter temp = new RegulatoryCounter();
                    temp.openHandle(outbase + "." + s + ".regulatory");
                    return temp;}));
        
        // Now get the samples from the VCF file
        VCFFileReader reader = new VCFFileReader(this.VcfFile, false);
        VCFHeader head = reader.getFileHeader();
        List<String> samples = head.getGenotypeSamples();
        
        log.log(Level.INFO, "[POPSTATS] VCF sample count: " + samples.size());
        
        // Now open pVCF output files for population subsamples
        final Map<String, BufferedWriter> pVCFs = pops.stream()
                .collect(Collectors.toMap(s -> s, s -> {
                    BufferedWriter out = null;
                    try {
                        out = Files.newBufferedWriter(Paths.get(this.outbase + "." + s + ".pvcf"));
                    } catch (IOException ex) {
                        Logger.getLogger(PopStats.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return out;
                }));
        
        // Generate reusable population variant counter
        final Map<String, IntCounter> genotypePCount = pops.stream()
                .collect(Collectors.toMap(s -> s, (s) -> {return new IntCounter();}));
        
        // Iterate through VCF to parse data and output pseudo vcfs
        reader.iterator().forEachRemaining((vcf) -> {
            // Get all the basic information set
            
            String chr = vcf.getContig();
            int pos = vcf.getStart();
            String variant = vcf.getType().name();
            String annotation = vcf.getAttributeAsString("ANN", "");
            String ref = vcf.getReference().getBaseString();
            // NOTE: I'm assuming biallelic to save time here! This should be modified for multiple alleles
            String alt = vcf.getAlternateAllele(0).getBaseString();
            double qual = vcf.getPhredScaledQual();
            ArrayList<String> temp = new ArrayList<>();
            temp.addAll(vcf.getFilters());
            String filter = StrUtils.StrArray.Join(temp, ";");
            String originalInfo = vcf.getAttributes().entrySet().stream()
                    .map(p -> p.getKey() + "=" + vcf.getAttributeAsString(p.getKey(), ""))
                    .reduce("", (a, b) -> a + ";" + b);
            
            String[] ansegs = annotation.split("\\|");
            String[] vEffects = ansegs[1].split("&");
            
            
            final boolean extraINS = (ref.length() < alt.length());
            final boolean extraDEL = (ref.length() > alt.length());
            final boolean regulatory = isRegulatoryRegion(vEffects);
            
            // Now, loop through the genotypes and count if the variant is present
            if(ansegs.length > 3){
            vcf.getGenotypesOrderedByName().forEach((genotype) -> {
                if(popLookup.containsKey(genotype.getSampleName())){
                    String pop = popLookup.get(genotype.getSampleName());
                    if(genotype.isHet()){
                        genotypePCount.get(pop).increment();
                    }else if(genotype.isHomVar()){
                        for(int x = 0; x < 2; x++){
                            genotypePCount.get(pop).increment();
                        }
                    }
                    boolean INDEL = false;
                    for(String v : vEffects){                        
                        if(this.annotationConverter.containsKey(v))
                            v = this.annotationConverter.get(v);
                        if(v.equals("INS") || v.equals("DEL"))
                            INDEL = true;
                        if(genotype.isHet()){
                            genStats.get(pop).incrementStats(chr, variant, ansegs[2], v, ref, alt);
                            if(regulatory)
                                regCount.get(pop).incPop();
                        }else if(genotype.isHomVar()){
                            for(int x = 0; x < 2; x++){
                                genStats.get(pop).incrementStats(chr, variant, ansegs[2], v, ref, alt);
                                if(regulatory)
                                    regCount.get(pop).incPop();
                            }
                        }
                    }
                }
            });
            
            int totalSamples = 0;
            for(Genotype g : vcf.getGenotypesOrderedByName()){
                totalSamples += 2;
            }
            
            // Check if we're in a gene
            
            if(!ansegs[3].isEmpty() && !ansegs[4].isEmpty() && !ansegs[5].equals("intergenic_region")){
                genotypePCount.keySet().stream()
                        .filter((s) -> genotypePCount.get(s).getCount() > 0)
                        .forEach((s) -> {
                            for(String v : vEffects){
                                if(this.annotationConverter.containsKey(v))
                                    v = this.annotationConverter.get(v);
                                geneCount.get(s).countStats(chr, ansegs[3], ansegs[4], v);
                            }
                        });
            }
            
            
            /* See if we can print out the data to a pVCF
                [output].[population].pvcf
                chr pos dbsnp   ref alt qual filter info    format  popfreq vcffreq
            */
            final int totvariantcount = genotypePCount.keySet().stream()
                    .map(s -> genotypePCount.get(s).getCount())
                    .reduce(0, (a, b) -> a + b);
            
            final double vcffreq = totvariantcount / (double) totalSamples;
            
            genotypePCount.keySet().stream()
                    .filter((s) -> genotypePCount.get(s).getCount() > 0)
                    .forEach((s) -> {
                        BufferedWriter out = pVCFs.get(s);
                        double popfreq = genotypePCount.get(s).getCount() / (double)popcounts.get(s);
                        try{
                            out.write(chr + "\t" + pos + "\t" + "-" + "\t" + ref + "\t" + alt + "\t" + qual + "\t" + filter + "\t" + originalInfo + "\t" + popfreq + "\t" + vcffreq);
                            out.write(System.lineSeparator());
                        }catch(IOException ex){
                            log.log(Level.SEVERE, "[POPSTATS] Error writting to pVCF!", ex);
                        }
                        
                        if(regulatory)
                            regCount.get(s).printOut(chr, pos, ansegs[4], ref, alt, vcffreq);
                        
                        // Zero out the frequency count
                        genotypePCount.get(s).setCount(0);
                    });
            }
        });
        
        reader.close();
        
        // We're done, so close up and print out everything
        genStats.keySet().stream()
                .forEach((s) -> {
                    genStats.get(s).PrintOut(outbase + "." + s + ".stats");
                });
        

        pVCFs.keySet().stream()
                .forEach((s) -> {
                    try {
                        pVCFs.get(s).close();
                    } catch (IOException ex) {
                        log.log(Level.SEVERE, "[POPSTATS] Error closing pvcf file!", ex);
                    }
                });
        
        geneCount.keySet().stream()
                .forEach((s) -> {
                    geneCount.get(s).closeHandle();
                });

        regCount.keySet().stream()
                .forEach((s) -> {
                    regCount.get(s).closeHandle();
                });
        
    }
    
    private boolean isRegulatoryRegion(String[] vEffects){
        boolean regulatory = false;
        for(String v : vEffects){
            if(v.equals("regulatory_region_variant") || v.equals("TF_binding_site_variant") || v.equals("upstream_gene_variant") || v.equals("5_prime_UTR_variant") || v.equals("3_prime_UTR_variant")){
                regulatory = true;
            }
        }
        return regulatory;
    }
}
