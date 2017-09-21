import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.*;
import java.util.*;
import java.lang.Object;
import org.apache.commons.math3.stat.regression.SimpleRegression;
//import jars.*;
//import custom.BigramCount;

//import com.sun.xml.internal.fastinfoset.vocab.Vocabulary;

public class ProbEstimator{

    /**
	  * args[0]: source text file
      */
    private static long corpusSize;
    private static long vocabSize;
    private static ArrayList<Integer> ffList;
    private static SimpleRegression linearR;

    public static void main(String[] args){
        String inputFileName = args[0];
        List<BigramCount> bigrams;
        ffList = new ArrayList<Integer>();
        linearR = new SimpleRegression();
        corpusSize = 0;
        try{
            bigrams = calcFrequency(inputFileName);
            writeVocab(bigrams);

            System.out.println("|V| = " + vocabSize + " Bigrams sceen = " + bigrams.size());
            System.out.println("N0 with |v| = " + (Math.pow(vocabSize, 2) - bigrams.size()));
            // ArrayList<Integer> zeros = new ArrayList<Integer>();
            // for(int i = 0; i < gtList.size(); i++) {
            //     if(gtList.get(i) == 0) 
            //         zeros.add(i);
            //     else
            //         linearR.addData(Math.log(i), Math.log(gtList.get(i)));
            // }
            // for(Integer var : zeros){
            //     gtList.set(var, (int)Math.round(Math.exp(linearR.predict(var)))); //un log10???
            // }
            calcBigrams(bigrams);
            writeff(ffList);

            System.out.println("N = " + corpusSize + " N^2= " + Math.pow(corpusSize, 2) + " N1 hello = " + ffList.get(1) + " N0 estimate = " + ((corpusSize * corpusSize) / ffList.get(1)));
            ffList.set(0, (int)((corpusSize * corpusSize) / ffList.get(1)));
            writeGT(ffList, 10);
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        
        
    }

    public static void calcBigrams(List<BigramCount> master) throws IOException{
        FileWriter fw = new FileWriter("results/bigrams.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        
        for (BigramCount var : master) {
            double cStar = (var.count + 1) * (ffList.get(var.count + 1) / (double)ffList.get(var.count));
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
                //var = (int)Math.round(linearR.predict(i));
                i++;
                continue;
            }
            bw.write(i + "                 :                   " + var.toString());
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

        System.out.println("Printed: " + k);
    }

    public static List<BigramCount> calcFrequency(String filename) throws IOException{

        FileReader fReader = new FileReader(filename);
        
        BufferedReader bReader = new BufferedReader(fReader);
        
        List<BigramCount> bigrams = new ArrayList<BigramCount>();
        ArrayList<String> vocab = new ArrayList<String>();
        Comparator<BigramCount> comp = new Comparator<BigramCount>() {
            public int compare(BigramCount a, BigramCount b) {
                return BigramCount.compare(a, b);
            }
        };

        String lastWord;
        String curWord;

        if((curWord = bReader.readLine()) != null){
            BigramCount potential = new BigramCount(curWord, 1);
            bigrams.add(potential);
        }
        lastWord = curWord;
        while((curWord = bReader.readLine()) != null){
            BigramCount potential = new BigramCount(lastWord, curWord, 1);
            corpusSize++;
            addVocab(curWord, vocab);
            int index = Collections.binarySearch(bigrams, potential, comp);
            //System.out.println(lastWord + "->" +curWord);
            if(index >= 0){
                bigrams.get(index).count++;
                //System.out.println("Incrementing " + vocab.get(index).toString());
            }else{
                index *= -1;
                bigrams.add(index - 1, potential);
                //System.out.println("Adding " + vocab.get(index - 1).toString());
            }
            lastWord = curWord;
        }

        vocabSize = vocab.size();

        int max = 0;
        int x = 0;
        for (BigramCount var : bigrams) {
            //var.frequency = var.count / (double)corpusSize;
            if(var.count > max){
                int diff = var.count - max;
                System.out.printf("Adding %d ",diff);
                int[] ztemp = new int[diff];
                ffList.addAll(new ArrayList<Integer>(Collections.nCopies(diff + 1, 0)));
                max = var.count;
                System.out.printf("%s is a new max at %d\n", var.toString(), max);
                ffList.set(max, 1);
            }else{
                ffList.set(var.count, ffList.get(var.count) + 1);
            }
        }

        return bigrams;
    }

    private static void addVocab(String word, ArrayList<String>vocab){
        int index = Collections.binarySearch(vocab, word);
        if(index < 0){
            index *= -1;
            vocab.add(index - 1, word);
            //System.out.println("Adding " + vocab.get(index - 1).toString());
        }
    }
}