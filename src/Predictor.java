import java.io.*;
import java.util.*;

public class Predictor{

    private static ArrayList<String[]> pairedWords;
    private static ArrayList<BigramCount> bigrams;
    private static final String endToken = "</s>";

    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("Not enough arguments");
            System.exit(0);
        }
        pairedWords = new ArrayList<String[]>();
        String testTokensFile = args[0];
        String predictionsFile = args[1];
        String confusingWordsFile = "data/all_confusingWords.txt";//args[1];
        String bigramsFile = "results/bigrams.ser";

        try{
            readConfusingWords(confusingWordsFile);
            bigrams = readBigrams(bigramsFile);

            ArrayList<int[]> errors = processTokens(testTokensFile);
            writeErrors(errors, predictionsFile);
        }catch(IOException io){
            System.out.println(io.getMessage());
            return;
        }catch(Exception ex){
            System.out.println(ex.getMessage());
            return;
        }
    }

    private static void writeErrors(ArrayList<int[]> data, String filename)throws  IOException{
        FileWriter fw = new FileWriter(filename);
		BufferedWriter bw = new BufferedWriter(fw);
        for (int[] location: data) {
            bw.write(location[0] + ":" + location[1]);
            bw.newLine();
        }
        bw.close();
        fw.close();
    }

    private static ArrayList<int[]> processTokens(String filename) throws IOException{
        FileReader fReader = new FileReader(filename);
        BufferedReader bReader = new BufferedReader(fReader);
        
        Comparator<BigramCount> comp = new Comparator<BigramCount>() {
            public int compare(BigramCount a, BigramCount b) {
                return BigramCount.compare(a, b);
            }
        };
        ArrayList<int[]> out = new ArrayList<int[]>();

        int sentenceCount = 0;
        int wordCount = 1;

        int lineCount = 2;
        boolean error = false;
        boolean newSeq = false;
        StringBuffer sentence = new StringBuffer();

        String lastWord;
        String curWord;
        if((lastWord = bReader.readLine()) == null){
           System.out.println("Not enough tokens.");
            return out;
        }
 
        while((curWord = bReader.readLine()) != null){
            if(curWord.equals(endToken)){
                sentenceCount++;
                wordCount = -1;
                if(error){
                    if(!newSeq)
                        System.out.println(sentence.toString());
                }
                sentence = new StringBuffer();
                error = false;
                newSeq = false;
            }else{
                String[] pair;
                if((pair = checkConfusedWord(curWord)) != null){
                    error = true;
                    double max = 0.0; //chanceModel(lastWord, curWord);
                    String mostLikely = null; //curWord;
                    //System.out.print("Possible confusion " + lastWord + " -> " + curWord + " : ");
                    for(String word: pair){
                        //if(word.equals(curWord)) continue;
                        double temp = chanceModel(lastWord, word, comp);
                        
                        int dif = Double.compare(max, temp);
                        //System.out.print(word + ": " + temp + " --- " + dif);
                        if(dif < 0 || (dif == 0  && word.equals(curWord))){
                            max = temp;
                            mostLikely = word;
                        }
                    }
                    //System.out.println("-----" + mostLikely + "-----");
                    if(!mostLikely.equals(curWord)){
                        int[] temp = {sentenceCount, wordCount};
                        out.add(temp);
                        System.out.println("Error predicted " + lineCount +"\n");
                        error = false;
                    }
                    if(max == 0.0) newSeq = true;
                }
            }
            lastWord = curWord;
            wordCount++;
            lineCount++;
            sentence.append(" ");
            sentence.append(curWord);
        }
        return out;
    }

    private static double chanceModel(String w1, String w2, Comparator<BigramCount> comp){
        BigramCount potential = new BigramCount(w1, w2, 0);
        int index = Collections.binarySearch(bigrams, potential, comp);
        if(index >= 0){
            return bigrams.get(index).frequency;
        }else{
            return 0.0;
        }
    }

    private static String[] checkConfusedWord(String word){
        for(String[] pair: pairedWords){
            for(int i = 0; i < pair.length; i++){
                if(pair[i].equals(word)){
                    return pair;
                }
            }
        }
        return null;
    }

    private static void readConfusingWords(String filename) throws IOException{
        FileReader fReader = new FileReader(filename);
        BufferedReader bReader = new BufferedReader(fReader);

        String line;
        while((line = bReader.readLine()) != null){
            String[] temp = line.split(":");
            if(temp.length != 2){
                System.out.println("Bad format found in ");
            }
            pairedWords.add(temp);
            //System.out.println(temp[0] + " <-> " + temp[1]);
        }
    }

    private static ArrayList<BigramCount> readBigrams(String filename) throws Exception{
        File objFile = new File(filename);
        FileInputStream fileIn = new FileInputStream(objFile);
        ObjectInputStream objIn = new ObjectInputStream(fileIn);

        //@SuppressWarnings("unchecked");
        Object temp = objIn.readObject();
        ArrayList<BigramCount> out = (ArrayList<BigramCount>)temp;
        // if(temp instanceof ArrayList<BigramCount>){
        //     out = (ArrayList<BigramCount>)temp;
        // }
        //ArrayList<BigramCount> out = temp instanceof ArrayList<BigramCount>? temp.cast(ArrayList<BigramCount>) : null;
        //(temp.isInstance(ArrayList<BigramCount>)) ? temp.cast(ArrayList<BigramCount>) : null;
        System.out.println("Read in " + out.size() + " bigrams");
        System.out.println(out.get(0).toString() + " Through...");
        System.out.println(out.get(out.size() - 1).toString());
        return out;
    }
}