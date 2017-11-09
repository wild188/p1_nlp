import java.io.*;
import java.util.*;

public class Predictor{

    private static ArrayList<String[]> pairedWords;
    private static ArrayList<BigramCount> bigrams;
    private static double[] gtTable;
    private static final String startToken = "<s>";

    private static ArrayList<ArrayList<Integer>> simpleModelErrors;
    private static ArrayList<ArrayList<Integer>> gtErrors;
    private static ArrayList<ArrayList<Integer>> laplacianErrors;

    public static void main(String[] args){
        if(args.length < 2){
            System.out.println("Not enough arguments");
            System.exit(0);
        }
        pairedWords = new ArrayList<String[]>();
        simpleModelErrors = new ArrayList<ArrayList<Integer>>();
        gtErrors = new ArrayList<ArrayList<Integer>>();
        laplacianErrors = new ArrayList<ArrayList<Integer>>();

        String testTokensFile = args[0];
        String predictionsFile = args[1];
        String confusingWordsFile = "data/all_confusingWords.txt";//args[1];
        String bigramsFile = "results/bigrams.ser";
        String gtTableFile = "results/GTTable.ser";

        String simplePredictionsFile = "results/simplePredictions.txt";
        String laplacianPredictionsFile = "results/laplacianPredictions.txt";
        String gtPredictionsFile = "results/GTPredictions.txt";

        try{
            bigrams = readBigrams(bigramsFile);
            gtTable = readGTTable(gtTableFile);
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
            return;
        }
        
        try{
            readConfusingWords(confusingWordsFile);
            // bigrams = readBigrams(bigramsFile);
            // gtTable = readGTTable(gtTableFile);

            processTokens(testTokensFile);
            //writeErrors(errors, predictionsFile);
            writeErrors(simpleModelErrors, simplePredictionsFile);
            writeErrors(laplacianErrors, laplacianPredictionsFile);
            writeErrors(gtErrors, gtPredictionsFile);
            writeErrors(gtErrors, predictionsFile);
        }catch(IOException io){
            System.out.println(io.getMessage());
            return;
        }
        
    }

    private static void writeErrors(ArrayList<ArrayList<Integer>> data, String filename)throws  IOException{
        FileWriter fw = new FileWriter(filename);
		BufferedWriter bw = new BufferedWriter(fw);
        for (ArrayList<Integer> location: data) {
            bw.write(location.get(0) + ":" + (location.get(1) + 1)); //off by one
            if(location.size() > 2){
                for(int i = 2; i < location.size(); i++){
                    bw.write("," + (location.get(i) + 1)); //off by one
                }            
            }
            bw.write(",\n");
            //bw.newLine();
        }
        bw.close();
        fw.close();
    }

    private static void processTokens(String filename) throws IOException{
        FileReader fReader = new FileReader(filename);
        BufferedReader bReader = new BufferedReader(fReader);
        
        Comparator<BigramCount> comp = new Comparator<BigramCount>() {
            public int compare(BigramCount a, BigramCount b) {
                return BigramCount.compare(a, b);
            }
        };

        int sentenceCount = 0;
        int wordCount = 0;

        String lastWord;
        String curWord;
        boolean sentenceError1 = false;
        boolean sentenceError2 = false;
        boolean sentenceError3 = false;
        if((lastWord = bReader.readLine()) == null){
           System.out.println("Not enough tokens.");
            return;
        }
 
        while((curWord = bReader.readLine()) != null){
            if(curWord.equals(startToken)){
                sentenceCount++;
                wordCount = 0;
                sentenceError1 = false;
                sentenceError2 = false;
                sentenceError3 = false;
                lastWord = curWord;
                continue;
            }else{
                String[] pair;
                if((pair = checkConfusedWord(curWord)) != null){
                    sentenceError1 = modelCheck(sentenceCount, wordCount, pair, curWord, lastWord, comp, 1, sentenceError1);
                    sentenceError2 = modelCheck(sentenceCount, wordCount, pair, curWord, lastWord, comp, 2, sentenceError2);
                    sentenceError3 = modelCheck(sentenceCount, wordCount, pair, curWord, lastWord, comp, 3, sentenceError3);
                }
            }
            lastWord = curWord;
            wordCount++;
        }
        return;
    }

    private static boolean modelCheck(int sentenceCount, int wordCount, String[] pair, String curWord, String lastWord, Comparator<BigramCount> comp, int modelFlag, boolean addFlag){
        double max = 0.0; //chanceModel(lastWord, curWord);
        String mostLikely = null; //curWord;
        for(String word: pair){
            //if(word.equals(curWord)) continue;
            double temp = 0;
            if(modelFlag == 1){
                temp = laplacianModel(lastWord, word, comp);
            }else if(modelFlag == 2){
                temp = gtModel(lastWord, word, comp);
            }else{
                temp = simpleModel(lastWord, word, comp);
            }
            
            int dif = Double.compare(max, temp);
            //System.out.print(word + ": " + temp + " --- " + dif);
            if(dif < 0 || (dif == 0  && word.equals(curWord))){
                max = temp;
                mostLikely = word;
            }
        }
        //System.out.println("-----" + mostLikely + "-----");
        if(!mostLikely.equals(curWord)){
            if(!addFlag){
                ArrayList<Integer> temp = new ArrayList<Integer>(); //{sentenceCount, wordCount};
                temp.add(sentenceCount);
                temp.add(wordCount);
                if(modelFlag == 1){
                    laplacianErrors.add(temp);
                }else if(modelFlag == 2){
                    gtErrors.add(temp);
                }else if(modelFlag == 3){
                    simpleModelErrors.add(temp);
                }
                return true;
            }else{
                if(modelFlag == 1){
                    laplacianErrors.get(laplacianErrors.size() - 1).add(wordCount);
                }else if(modelFlag == 2){
                    gtErrors.get(gtErrors.size() - 1).add(wordCount);
                }else if(modelFlag == 3){
                    simpleModelErrors.get(simpleModelErrors.size() - 1).add(wordCount);
                }
            }
        }
        return addFlag;
    }

    //Uses laplacian smoothing but ignored teh constant probability of the first word
    private static double laplacianModel(String w1, String w2, Comparator<BigramCount> comp){
        BigramCount potential = new BigramCount(w1, w2, 0);
        int index = Collections.binarySearch(bigrams, potential, comp);
        if(index >= 0){
            return bigrams.get(index).laplacianProb;      //frequency;
        }else{
            return 0.0;
        }
    }

    private static int simpleModel(String w1, String w2, Comparator<BigramCount> comp){
        BigramCount potential = new BigramCount(w1, w2, 0);
        int index = Collections.binarySearch(bigrams, potential, comp);
        if(index >= 0){
            return bigrams.get(index).count;      //frequency;
        }else{
            return 0;
        }
    }

    //Uses Good Turing smoothing but ignored teh constant probability of the first word
    private static double gtModel(String w1, String w2, Comparator<BigramCount> comp){
        BigramCount potential = new BigramCount(w1, w2, 0);
        int index = Collections.binarySearch(bigrams, potential, comp);
        if(index >= 0){
            int count = bigrams.get(index).count;
            if(count >= gtTable.length) return count; // unsmoothed c > k
            return gtTable[count]; //smoothed c < k
        }else{
            return gtTable[0];
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

    private static double[] readGTTable(String filename) throws Exception{
        File objFile = new File(filename);
        FileInputStream fileIn = new FileInputStream(objFile);
        ObjectInputStream objIn = new ObjectInputStream(fileIn);

        //@SuppressWarnings("unchecked");
        Object temp = objIn.readObject();
        double[] out = (double[])temp;
        System.out.println("Read in " + out.length + " Good Turing table entries");
        return out;
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