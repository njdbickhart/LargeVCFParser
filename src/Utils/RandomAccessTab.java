/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class RandomAccessTab {
    private static final Logger log = Logger.getLogger(RandomAccessTab.class.getName());
    private RandomAccessFile temp = null;
    private int SampleCount = 0;
    private final List<String> Samples = new ArrayList<>();
    private final Map<String, Map<Integer, Map<String, Long>>> Index = new HashMap<>();
    
    public RandomAccessTab(String outbase){
        Random rand = new Random();
        Path tempFile = Paths.get(outbase + rand.nextInt(5000) + ".temp");
        try {
            this.temp = new RandomAccessFile(tempFile.toFile(), "rw");
        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, "Could not create temporary file!", ex);
        }
    }
    
    public void SetSampleStats(String head){
        String[] segs = head.split("\t");
        for(int x = 10; x < segs.length; x++){
            Samples.add(segs[x]);
            SampleCount++;
        }
        log.log(Level.INFO, "Read " + SampleCount + " samples from file.");
    }
    
    public synchronized void ParseLine(String line){
        String[] segs = line.split("\t");
        try{
            if(!Index.containsKey(segs[0]))
                Index.put(segs[0], new HashMap<>());
            if(!Index.get(segs[0]).containsKey(Integer.parseInt(segs[1])))
                Index.get(segs[0]).put(Integer.parseInt(segs[1]), new HashMap());
            if(!Index.get(segs[0]).get(Integer.parseInt(segs[1])).containsKey(segs[3]))
                Index.get(segs[0]).get(Integer.parseInt(segs[1])).put(segs[3], this.temp.getFilePointer());
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error accessing temp file in line parser!", ex);
        }
        
        for(int x = 10; x < segs.length; x++){
            try {
                this.temp.write(segs[x].getBytes());
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Was not able to encode " + segs[x] + " genotype in line: " + line, ex);
            }
        } 
        
    }
    
    public synchronized String GetGenotypeOutString(String chr, Integer pos, String allele){
        StringBuilder outstr = new StringBuilder();
        if(Index.containsKey(chr)){
            if(Index.get(chr).containsKey(pos)){
                if(Index.get(chr).get(pos).containsKey(allele)){
                    
                }
            }
        }
        
        // We didn't have the key in this file. Printing empty characters
        for(int x = 0; x < this.SampleCount; x++){
            outstr.append("\t").append("./.");
        }
        
        return outstr.toString();
    }
    
    private String[] ConvertBinaryGenotypes(long index){
        String[] data = null;
        try{
            this.temp.seek(index);
            byte[] block = new byte[SampleCount * 3];
            this.temp.read(block);
            
            String convert = new String(block, Charset.defaultCharset());
            data = convert.split("(?<=\\G...)");
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading from temp file!", ex);
        }
        return data;
    }
}
