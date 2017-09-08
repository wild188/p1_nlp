import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.*;

class WordCount{
    public String word;
    public int count;
    public double frequency;
    public WordCount(String w, int c){
        this.word = w;
        this.count = c;
    }

    public int compare(WordCount a, WordCount b){
        return a.word.compareTo(b.word);
    }

    //@override
    public String toString(){
        return word + " : " + count + " : " + frequency; 
    }
}

public class ProbEstimator{

    /**
	  * args[0]: source text file
      */
    private static int corpusSize;
    private static ArrayList<Integer> gtList;

    public static void main(String[] args){
        String inputFileName = args[0];
        List<WordCount> vocab;
        gtList = new ArrayList<Integer>();
        corpusSize = 0;
        try{
            vocab = calcFrequency(inputFileName);
            writeVocab(vocab);
            writeff(gtList);
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        
        
    }
    private static void writeff(ArrayList<Integer> list)  throws IOException{
        FileWriter fw = new FileWriter("results/ff.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        System.out.println("GT Length: " + list.size());
        int i = 0;
        for (Integer var : list) {
            bw.write(i + " : " + var.toString());
            bw.newLine();
            i++;
        }
        bw.close();
        fw.close();
        System.out.println("Printed: " + i);
    }

    private static void writeVocab(List<WordCount> vocab) throws IOException{
        FileWriter fw = new FileWriter("results/vocab.txt");
		BufferedWriter bw = new BufferedWriter(fw);
        for (WordCount var : vocab) {
            bw.write(var.toString());
            bw.newLine();
        }
        bw.close();
        fw.close();
    }

    public static List<WordCount> calcFrequency(String filename) throws IOException{

        FileReader fReader = new FileReader(filename);
        
        BufferedReader bReader = new BufferedReader(fReader);
        
        List<WordCount> vocab = new ArrayList<WordCount>();
        Comparator<WordCount> comp = new Comparator<WordCount>() {
            public int compare(WordCount a, WordCount b) {
                return a.word.compareTo(b.word);
            }
        };

        String curWord;

        if((curWord = bReader.readLine()) != null){
            WordCount potential = new WordCount(curWord, 1);
            vocab.add(potential);
        }

        while((curWord = bReader.readLine()) != null){
            WordCount potential = new WordCount(curWord, 1);
            corpusSize++;
            int index = Collections.binarySearch(vocab, potential, comp);
            //System.out.println(curWord);
            if(index >= 0){
                vocab.get(index).count++;
                //return index;
            }else{
                index *= -1;
                vocab.add(index - 1, potential);
                //return index
            }
        }

        int max = 0;
        int x = 0;
        for (WordCount var : vocab) {
            var.frequency = var.count / (double)corpusSize;
            if(var.count > max){
                int diff = var.count - max;
                System.out.printf("Adding %d ",diff);
                int[] ztemp = new int[diff];
                gtList.addAll(new ArrayList<Integer>(Collections.nCopies(diff + 1, 0)));
                max = var.count;
                System.out.printf("%s is a new max at %d\n", var.word, max);
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