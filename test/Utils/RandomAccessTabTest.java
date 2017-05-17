/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author dbickhart
 */
public class RandomAccessTabTest {
    private static final RandomAccessTab tester = new RandomAccessTab("basetest");
    
    public RandomAccessTabTest() {
        
    }
    
    @BeforeClass
    public static void setUpClass() {
        String head = "CHR\tPOS\tREF\tALT\tQUAL\tTYPE\tMUTATION\tPRIORITY\tGENE\tAA\tHOUSA000001697572\tHOUSA000002040728\tHOUSA000002103297\tHOUSA000002147486\tHOUSA000002290977\tHOUSA000122358313\tHOCANM000000382748\tHOCANM000000383622\tHOCANM000000392405\tHOCANM000005470579\tHOCANM000006962003\tHOUSAM000001773417\tHOUSAM000002160458\tHOUSAM000002249055\tHOUSAM000002250783\tHOUSAM000002266008\tHOUSAM000002271271\tHOUSAM000060540164\tHOUSAM000060996956\tHOUSAM000061133837\tHOUSAM000061547476\tHOUSAM000120780521\tHOUSAM000123645630\tHOUSAM000128367894\tHOUSAM000128749834\tHOUSAM000130153294\tHOUSAM000130558361\tHOUSAM000134438230\tHOUSAM000135746776\tHOUSAM000207124561\tHOUSAM000207184639\tHOLCANM000005279989\tHOLCANM000006026421\tHOLCANM000100745543\tHOLDEUM000000253642\tHOLGBRM000000598172\tHOLITAM006001001962\tHOLUSAM000002265005\tHOLUSAM000002297473\tHOLUSAM000017129288\tHOLUSAM000017349617\tHOLUSAM000123066734\tHOLUSAM000132973942";
        tester.SetSampleStats(head);
        String line = "Chr19\t123\tG\tC\t27.0278\tSNP\tintergenic_region\tMODIFIER\tENSBTAG00000037903\t\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0";
        tester.ParseLine(line);
        String alt = "Chr19\t1234\tG\tC\t27.0278\tSNP\tintergenic_region\tMODIFIER\tENSBTAG00000037903\tpM.A\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0";
        tester.ParseLine(alt);
    }
    
    @AfterClass
    public static void tearDownClass() {
        tester.close();
    }

    
    /**
     * Test of SetSampleStats method, of class RandomAccessTab.
     */
    @Test
    public void testSetSampleStats() {
        System.out.println("SetSampleStats");
        
        assertEquals(43, tester.getSamples().size());
    }

    /**
     * Test of ParseLine method, of class RandomAccessTab.
     */
    @Test
    public void testParseLine() {
        System.out.println("ParseLine");
        
    }

    /**
     * Test of GetInfoTab method, of class RandomAccessTab.
     */
    @Test
    public void testGetInfoTab() {
        System.out.println("GetInfoTab");
        String chr = "Chr19";
        Integer pos = 123;
        String allele = "C";
        String[] expResult = {"G", "27.0278", "SNP", "intergenic_region", "MODIFIER", "ENSBTAG00000037903", "-"};
        String[] result = tester.GetInfoTab(chr, pos, allele);
        assertArrayEquals(expResult, result);
        
        pos = 1234;
        String[] nextResult = {"G", "27.0278", "SNP", "intergenic_region", "MODIFIER", "ENSBTAG00000037903", "pM.A"};
        result = tester.GetInfoTab(chr, pos, allele);
        
        assertArrayEquals(nextResult, result);
    }

    /**
     * Test of GetGenotypeOutString method, of class RandomAccessTab.
     */
    @Test
    public void testGetGenotypeOutString() {
        System.out.println("GetGenotypeOutString");
        String chr = "Chr19";
        Integer pos = 123;
        String allele = "C";
        String expResult = "0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;1\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0\t0;0";
        String result = tester.GetGenotypeOutString(chr, pos, allele);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCondensedIndexList method, of class RandomAccessTab.
     */
    @Ignore
    @Test
    public void testGetCondensedIndexList() {
        System.out.println("getCondensedIndexList");
        RandomAccessTab instance = null;
        Map<String, Map<Integer, Set<String>>> expResult = null;
        Map<String, Map<Integer, Set<String>>> result = instance.getCondensedIndexList();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of containsKey method, of class RandomAccessTab.
     */
    @Test
    public void testContainsKey() {
        System.out.println("containsKey");
        String chr = "Chr29";
        Integer pos = 12;
        String allele = "H";
        boolean expResult = false;
        boolean result = tester.containsKey(chr, pos, allele);
        assertEquals(expResult, result);
    }
    
}
