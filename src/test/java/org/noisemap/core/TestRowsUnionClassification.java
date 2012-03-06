package org.noisemap.core;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

public class TestRowsUnionClassification extends TestCase  {
    private int GetRandomRowNumber(Random randomizer) {
        return (int)(randomizer.nextDouble() * 100);
    }
    private void checkMerging(RowsUnionClassification mergeTool,List<Integer> correctValues,Long seedValue,int forIndex) {
        Iterator<Integer> it=mergeTool.getRowRanges();
        while(it.hasNext()) {
            int begin=it.next();
            int end=it.next();
            assertTrue("end>begin.Seed is "+seedValue+" for() i="+forIndex,begin<=end);
            for(int val=begin;val<=end;val++) {
                if(!correctValues.contains(val)) {
                    assertTrue(val+" not in expected ranges. Seed is "+seedValue+" for() i="+forIndex,correctValues.contains(val));
                }
            }
        }
        
    }
    public void testRowMergeRandom() {
        Random randomizer = new Random();
        long seedValue = System.nanoTime();
        randomizer.setSeed(seedValue);
        int firstValue = GetRandomRowNumber(randomizer);
        
        RowsUnionClassification mergeTool=new RowsUnionClassification(firstValue);
        List<Integer> correctValues=new ArrayList<Integer>();
        correctValues.add(firstValue);
        
        for(int i=0;i<80;i++) {
            int rndValue = GetRandomRowNumber(randomizer);
            mergeTool.addRow(rndValue);
            correctValues.add(rndValue);
            checkMerging(mergeTool, correctValues, seedValue,i);
        }
        
    }
    public void testRowMerge() {
        long deb=System.nanoTime();
        RowsUnionClassification mergeTool=new RowsUnionClassification(50);

        for(int i=51;i<200;i++) {
            mergeTool.addRow(i);
        }

        for(int i=300;i<400;i++) {
            mergeTool.addRow(i);
        }
        for(int i=450;i<1500;i++) {
            mergeTool.addRow(i);
        }
        for(int i=2000;i<20000;i++) {
            mergeTool.addRow(i);
        }
        for(int i=401;i<450;i++) {
            mergeTool.addRow(i);
        }
        //Add already pushed rows id, the result of intervals must be the same
        mergeTool.addRow(401);
        mergeTool.addRow(410);
        mergeTool.addRow(19999);
        
        
        mergeTool.addRow(400);
        double timeadd=((System.nanoTime()-deb)/1e6);
        //Test if results is correct
        Iterator<Integer> it=mergeTool.getRowRanges();
        System.out.println("Ranges :");
        List<Integer> correctRanges=new ArrayList<Integer>();
        correctRanges.add(50);
        correctRanges.add(199);
        correctRanges.add(300);
        correctRanges.add(1499);
        correctRanges.add(2000);
        correctRanges.add(19999);

        while(it.hasNext()) {
            int begin=it.next();
            int end=it.next();
            System.out.print("["+begin+"-"+end+"]");
            assertTrue(correctRanges.contains(begin));
            assertTrue(correctRanges.contains(end));
            assertTrue(!(300<begin && begin<1499));
            assertTrue(!(300<end && end<1499));
        }
        System.out.println("");
        System.out.println("Merging of rows took :"+timeadd+" ms");

    }
}
