/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class RandomAccessTab {
    private static final Logger log = Logger.getLogger(RandomAccessTab.class.getName());
    private String tempBase;
    private RandomAccessFile temp = null;
    private RandomAccessFile InfoTemp = null;
    private int SampleCount = 0;
    private final List<String> Samples = new ArrayList<>();
    private final Map<String, Map<Integer, Map<String, Long>>> Index = new HashMap<>();
    private final Map<String, Map<Integer, Map<String, Long>>> InfoIndex = new HashMap<>();
    
    public RandomAccessTab(String outbase){
        Random rand = new Random();
        tempBase = outbase + rand.nextInt(5000);
        try {
            File gFile = new File(tempBase + ".gtype.temp");
            File iFile = new File(tempBase + ".info.temp");
            gFile.deleteOnExit();
            iFile.deleteOnExit();
            this.temp = new RandomAccessFile(gFile, "rw");
            this.InfoTemp = new RandomAccessFile(iFile, "rw");
        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, "Could not create temporary file!", ex);
        }
    }
    public void close(){
        try{
            this.temp.close();
            this.InfoTemp.close();
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error closing temp random files!", ex);
        }
    }
    
    public List<String> getSamples(){
        return Samples;
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
        if(!StrUtils.NumericCheck.isNumeric(segs[1])){
            log.log(Level.WARNING, "Skipping line without position number: " + line);
            return;
        }
        if(segs[3].contains(","))
            return; // Ignores multiple allele entries for now
        try{
            if(!Index.containsKey(segs[0])){
                Index.put(segs[0], new HashMap<>());
                InfoIndex.put(segs[0], new HashMap<>());
            }
            if(!Index.get(segs[0]).containsKey(Integer.parseInt(segs[1]))){
                Index.get(segs[0]).put(Integer.parseInt(segs[1]), new HashMap());
                InfoIndex.get(segs[0]).put(Integer.parseInt(segs[1]), new HashMap());
            }
            if(!Index.get(segs[0]).get(Integer.parseInt(segs[1])).containsKey(segs[3])){
                Index.get(segs[0]).get(Integer.parseInt(segs[1])).put(segs[3], this.temp.getFilePointer());
                InfoIndex.get(segs[0]).get(Integer.parseInt(segs[1])).put(segs[3], this.InfoTemp.getFilePointer());
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error accessing temp file in line parser!", ex);
        }
        
        // Encoding info tag as a long tab delimited string in random access file
        StringBuilder tempOut = new StringBuilder();
        if(segs[9].equals("") || segs[9].length() < 1)
            segs[9] = "-"; // ensuring that there are no blank entries
        tempOut.append(segs[2]).append("\t").append(segs[4]).append("\t")
                .append(segs[5]).append("\t").append(segs[6]).append("\t")
                .append(segs[7]).append("\t").append(segs[8]).append("\t")
                .append(segs[9]);
        
        try{
            byte[] infoblock = tempOut.toString().getBytes();
            byte len = (byte)infoblock.length;
            this.InfoTemp.write(len);
            this.InfoTemp.write(infoblock);
        }catch(IOException ex){
            log.log(Level.SEVERE, "Was not able to encode infotab: " + line, ex);
        }
        
        for(int x = 10; x < segs.length; x++){
            try {
                this.temp.write(segs[x].getBytes());
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Was not able to encode " + segs[x] + " genotype in line: " + line, ex);
            }
        } 
        
    }
    
    public synchronized String[] GetInfoTab(String chr, Integer pos, String allele){
        if(this.containsKey(chr, pos, allele)){
            return ConvertBinaryInfo(this.InfoIndex.get(chr).get(pos).get(allele));
        }
        return null;
    }
    
    public synchronized String GetGenotypeOutString(String chr, Integer pos, String allele){
        StringBuilder outstr = new StringBuilder();
        if(this.containsKey(chr, pos, allele)){            
            String[] gtypes = ConvertBinaryGenotypes(Index.get(chr).get(pos).get(allele));
            outstr.append("\t").append(StrUtils.StrArray.Join(gtypes, "\t"));
            return outstr.toString();               
        }
        
        // We didn't have the key in this file. Printing empty characters
        for(int x = 0; x < this.SampleCount; x++){
            outstr.append("\t").append("./.");
        }
        
        return outstr.toString();
    }
    
    public Map<String, Map<Integer, Set<String>>> getCondensedIndexList(){
        Map<String, Map<Integer, Set<String>>> varList = new HashMap<>();
        for(String chr : Index.keySet()){
            if(!varList.containsKey(chr))
                varList.put(chr, new HashMap<>());
            for(Integer pos : Index.get(chr).keySet()){
                if(!varList.get(chr).containsKey(pos))
                    varList.get(chr).put(pos, new HashSet<>());
                for(String allele : Index.get(chr).get(pos).keySet()){
                    varList.get(chr).get(pos).add(allele);
                }
            }
        }
        return varList;
    }
    
    public boolean containsKey(String chr, Integer pos, String allele){
        if(Index.containsKey(chr)){
            if(Index.get(chr).containsKey(pos)){
                if(Index.get(chr).get(pos).containsKey(allele)){
                    return true;
                }
            }
        }
        return false;
    }
    
    private String[] ConvertBinaryInfo(long index){
        String[] data = null;
        try{
            this.InfoTemp.seek(index);
            byte len = this.InfoTemp.readByte();
            byte[] block = new byte[Byte.toUnsignedInt(len)];
            this.InfoTemp.read(block);
            
            String convert = new String(block, Charset.defaultCharset());
            return convert.split("\t");
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading from temp info file!", ex);
        }
        return data;
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
