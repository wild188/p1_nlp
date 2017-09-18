import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.*;
import java.lang.Object;
//import jars.*;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.*;

//import com.sun.xml.internal.fastinfoset.vocab.Vocabulary;

class BigramCount implements Serializable{
    public String word1;
    public String word2;
    public int count;
    public double frequency;
    public BigramCount(String w, int c){
        this.word1 = w;
        this.word2 = null;
        this.count = c;
    }

    public BigramCount(String w1, String w2, int c){
        this.word1 = w1;
        this.word2 = w2;
        this.count = c;
    }

    public static int compare(BigramCount a, BigramCount b){
        int first = a.word1.compareTo(b.word1);
        if(first != 0) return first;
        if(a.word2 == null){
            if(b.word2 == null) return 0;
            else return 1;
        }
        return a.word2.compareTo(b.word2);
    }

    //@override
    public String toString(){
        return word1 + " -> " + word2 + " : " + count + " : " + frequency; 
    }
}

public class ProbEstimator{

    /**
	  * args[0]: source text file
      */
    private static int corpusSize;
    private static ArrayList<Integer> gtList;
    private static SimpleRegression linearR;

    public static void main(String[] args){
        String inputFileName = args[0];
        List<BigramCount> vocab;
        gtList = new ArrayList<Integer>();
        linearR = new SimpleRegression();
        corpusSize = 0;
        try{
            vocab = calcFrequency(inputFileName);
            writeVocab(vocab);

            ArrayList<Integer> zeros = new ArrayList<Integer>();
            for(int i = 0; i < gtList.size(); i++) {
                if(gtList.get(i) == 0) 
                    zeros.add(i);
                else
                    linearR.addData(i, gtList.get(i));
            }
            for(Integer var : zeros){
                gtList.add(var, (int)Math.round(linearR.predict(var)));
            }

            calcBigrams(vocab);
            
            writeff(gtList);
            writeGT(gtList, 10);
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        
        
    }

    public static void calcBigrams(List<BigramCount> master) throws IOException{
        FileWriter fw = new FileWriter("results/bigrams.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        
        for (BigramCount var : master) {
            double cStar = (var.count + 1) * (gtList.get(var.count + 1) / (double)gtList.get(var.count));
            var.frequency = cStar / (double)master.size();
            bw.write(var.toString());
            bw.newLine();
        }

        try {
            File objFile = new File("results/bigrams.ser");
            objFile.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(objFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(master);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in /results/bigrams.ser");
         }catch(IOException i) {
            i.printStackTrace();
         }

        bw.close();
        fw.close();
    }
    
    private static void writeff(ArrayList<Integer> list)  throws IOException{
        FileWriter fw = new FileWriter("results/ff.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        System.out.println("GT Length: " + list.size());

        int i = 0;
        for (Integer var : list) {
            if(var == 0){
                var = (int)Math.round(linearR.predict(i));
            }
            bw.write(i + " : " + var.toString());
            bw.newLine();
            i++;
        }
        bw.close();
        fw.close();

        try {
            File objFile = new File("results/ff.ser");
            objFile.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(objFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(list);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in /results/ff.ser");
         }catch(IOException ex) {
            ex.printStackTrace();
         }

        System.out.println("Printed: " + i);
    }

    private static void writeVocab(List<BigramCount> vocab) throws IOException{
        FileWriter fw = new FileWriter("results/vocab.txt");
		BufferedWriter bw = new BufferedWriter(fw);
        for (BigramCount var : vocab) {
            bw.write(var.toString());
            bw.newLine();
        }
        bw.close();
        fw.close();
    }

    private static void writeGT(ArrayList<Integer> list, int k) throws IOException{
        FileWriter fw = new FileWriter("results/GTTable.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        System.out.println("GT Length: " + k);
        
        double[] cStars = new double[k];
        for(int i = 0; i < k; i++){
            System.out.printf((i + 1) + "*" + (list.get(i + 1) + "/" + (double)list.get(i)) + "\n");
            cStars[i] = (i + 1) * (list.get(i + 1) / (double)list.get(i));
            bw.write(i + " : " + cStars[i]);
            bw.newLine();
        }

        bw.close();
        fw.close();

        try {
            File objFile = new File("results/GTTable.ser");
            objFile.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(objFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(cStars);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in /results/GTTable.ser");
         }catch(IOException ex) {
            ex.printStackTrace();
         }

        //System.out.println("Printed: " + i);
    }

    public static List<BigramCount> calcFrequency(String filename) throws IOException{

        FileReader fReader = new FileReader(filename);
        
        BufferedReader bReader = new BufferedReader(fReader);
        
        List<BigramCount> vocab = new ArrayList<BigramCount>();
        Comparator<BigramCount> comp = new Comparator<BigramCount>() {
            public int compare(BigramCount a, BigramCount b) {
                return BigramCount.compare(a, b);
            }
        };

        String lastWord;
        String curWord;

        if((curWord = bReader.readLine()) != null){
            BigramCount potential = new BigramCount(curWord, 1);
            vocab.add(potential);
        }
        lastWord = curWord;
        while((curWord = bReader.readLine()) != null){
            BigramCount potential = new BigramCount(lastWord, curWord, 1);
            corpusSize++;
            int index = Collections.binarySearch(vocab, potential, comp);
            //System.out.println(lastWord + "->" +curWord);
            if(index >= 0){
                vocab.get(index).count++;
                //System.out.println("Incrementing " + vocab.get(index).toString());
            }else{
                index *= -1;
                vocab.add(index - 1, potential);
                //System.out.println("Adding " + vocab.get(index - 1).toString());
            }
            lastWord = curWord;
        }

        int max = 0;
        int x = 0;
        for (BigramCount var : vocab) {
            //var.frequency = var.count / (double)corpusSize;
            if(var.count > max){
                int diff = var.count - max;
                System.out.printf("Adding %d ",diff);
                int[] ztemp = new int[diff];
                gtList.addAll(new ArrayList<Integer>(Collections.nCopies(diff + 1, 0)));
                max = var.count;
                System.out.printf("%s is a new max at %d\n", var.toString(), max);
                gtList.set(max, 1);
            }else{
                gtList.set(var.count, gtList.get(var.count) + 1);
            }
        }

        return vocab;
    }

    // private static int newWord(List<WordCount> v, String word){
    //     int index = Collections.binarySearch(vocab);
    //     if(index > 1){
    //         v[index].count++;
    //         return index;
    //     }
    // }
}