import java.io.*;
import java.util.*;

public class BigramCount implements Serializable{
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