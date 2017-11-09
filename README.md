I have checked that all the source code compiles and runs on the sunlab. The project has worked for all of my tests. Hopefully 
you find the same. When compiling the predictor.java file there is a warning. This is due to an implicit case to an object read
from a serialized file. I couldn't figure out how to remove the warning but it runs just fine. 

I have created a bigram object that is serialized as a sorted array to improve parsing performance. I also create three different 
prediction files. One for the Good Turing model, one for Laplacian Smoothing, and another that just simply uses the bigram count. For the spellchecking application there is no need to keep track of the count of the preceeding word as it is a constant when comparing the count of the possible confused second word. 