/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
import Utils.RandomAccessTab;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;

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
        // Process each file into random access temp files
        RandomAccessTab firstRand = new RandomAccessTab("FirstTemp.out.");
        try(BufferedReader reader = Files.newBufferedReader(firstFile)){
            // Get header for the file
            String head = reader.readLine();
            firstRand.SetSampleStats(head);
            
            String line;
            while((line = reader.readLine()) != null){
                line = line.trim();
                firstRand.ParseLine(line);
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Could not read from first file: " + firstFile.toString(), ex);
        }
        
        RandomAccessTab secondRand = new RandomAccessTab("SecondTemp.out.");
        try(BufferedReader reader = Files.newBufferedReader(secondFile)){
            // Get header for the file
            String head = reader.readLine();
            secondRand.SetSampleStats(head);
            
            String line;
            while((line = reader.readLine()) != null){
                line = line.trim();
                secondRand.ParseLine(line);
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Could not read from second file: " + secondFile.toString(), ex);
        }
        
        log.log(Level.INFO, "Loaded both files. Proceeding to process data.");
        
        // Get list of condensed variant locations
        Map<String, Map<Integer, Set<String>>> variantSites = firstRand.getCondensedIndexList();
        Map<String, Map<Integer, Set<String>>> tempSecondSites = secondRand.getCondensedIndexList();
        
        for(String chr : tempSecondSites.keySet()){
            if(!variantSites.containsKey(chr))
                variantSites.put(chr, new HashMap<>());
            for(Integer pos : tempSecondSites.get(chr).keySet()){
                if(!variantSites.get(chr).containsKey(pos))
                    variantSites.get(chr).put(pos, new HashSet<>());
                for(String allele : tempSecondSites.get(chr).get(pos)){
                    variantSites.get(chr).get(pos).add(allele);
                }
            }
        }
        tempSecondSites = null;
        
        // print out header
        BufferedWriter output = null;
        try{
            output = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        }catch(IOException ex){
            log.log(Level.SEVERE, "Could not open output file: " + outputFile.toString(), ex);
        }
        
        // print out header
        StringBuilder header = new StringBuilder();
        header.append("CHR\tPOS\tREF\tALT\tQUAL\tTYPE\tMUTATION\tPRIORITY\tGENE\tAA");
        for(String sample : firstRand.getSamples()){
            header.append("\t").append(sample);
        }
        for(String sample : secondRand.getSamples()){
            header.append("\t").append(sample);
        }
        header.append(System.lineSeparator());
        try{
            output.write(header.toString());
        }catch(IOException ex){
            log.log(Level.SEVERE, "error writing header to output file!", ex);
        }
        
        // Get ordered list of position coordinates and start printing output
        List<String> sortedChrs = variantSites.entrySet().stream()
                .map(Entry::getKey).sorted().collect(toList());
        
        int linecount = 0, firstInfo = 0, secondInfo = 0, firstOnly = 0, secondOnly = 0, both = 0;
        
        try{
            for(String chr : sortedChrs){
                List<Integer> sortedPos = variantSites.get(chr).entrySet().stream()
                        .map(Entry::getKey).sorted().collect(toList());
                for(Integer pos : sortedPos){
                    List<String> sortedAlleles = variantSites.get(chr).get(pos).stream()
                            .sorted().collect(toList());
                    for(String allele : sortedAlleles){
                        StringBuilder outStr = new StringBuilder();
                        String[] info;
                        // Preferentially draw info from first file
                        if(firstRand.containsKey(chr, pos, allele)){
                            info = firstRand.GetInfoTab(chr, pos, allele);
                            firstInfo++;
                        }else{
                            info = secondRand.GetInfoTab(chr, pos, allele);
                            secondInfo++;
                        }

                        outStr.append(chr).append("\t").append(pos).append("\t")
                                .append(info[0]).append("\t").append(allele);

                        for(int x = 1; x < info.length; x++){
                            outStr.append("\t").append(info[x]);                                
                        }
                        
                        if(firstRand.containsKey(chr, pos, allele) && secondRand.containsKey(chr, pos, allele))
                            both++;
                        else if(firstRand.containsKey(chr, pos, allele))
                            firstOnly++;
                        else
                            secondOnly++;

                        outStr.append(firstRand.GetGenotypeOutString(chr, pos, allele));
                        outStr.append(secondRand.GetGenotypeOutString(chr, pos, allele)).append(System.lineSeparator());
                        output.write(outStr.toString());
                        linecount++;
                    }
                }
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error writing to file output!", ex);
        }finally{
            try {
                firstRand.close();
                secondRand.close();
                output.close();
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Error closing output file!", ex);
            }
        }
        
        log.log(Level.INFO, "Completed merger. Printed " + linecount + " lines");
        log.log(Level.INFO, "Both: " + both);
        log.log(Level.INFO, "first only: " + firstOnly);
        log.log(Level.INFO, "second only: " + secondOnly);
        log.log(Level.INFO, "Infograb, first: " + firstInfo + " second: " + secondInfo);
    }
}
