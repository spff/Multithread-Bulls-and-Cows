# multithread-bulls-and-cows

auto solve, need at most 8 times to guess the secret with (digit=4 and the first guess=0123)

## Program flow

1. Start GameServer  

>1.1. determine which function to call based on "digits"  
  1.1.1 If digits > 8 determine whether to do in multi-thread up to user's choice  

2. Generate candidateList which store all possible permute  
3. Guess 0123 or whatever, doesn't matter  
4. Get the result, if correct, end.  
5. Update the candidateList  
6. Determine next guess(might either choose the first or via betterGuess  
   from the candidateList depends on user's choice)  

>6.1. If betterGuess is chosen, determine whether to do in multi-thread, and how DEEP to guess  
      when there are multiple choices with the same VALUE(See the document below for detail)  

7. Guess, and loop back to step 4.  



## algorithm for betterGuess()



 Beside choosing the first from the candidateList, we calculate the Discrete Degree  
 of each candidate in the candidateList.  

 Take digits = 3 for example, every choice may get 0A0B 0A1B 0A2B 0A3B 1A0B 1A1B 1A2B 2A0B  
 2A1B and 3A0B, all 9 kinds of possible responses  

 A+B less or equal 3
 -> A+B+LEFT = 3  
 -> [ A ][ + ][ B ][ + ][ LEFT ] put 3 [1]s into [ A ] or [ B ] or [ LEFT ]  
 -> 3*3 = 9  


 ### The strategy is

 Whenever the Secret is, we should choose the one which may get the most  
 Kinds of Different Possible Responses (Discrete Degree relative to 0) so we may  
 AVERAGELY eliminate most impossible candidates from the next response.  

 To do so, we're using a nested loop, the outer stands for Next Guess, the inner stands  
 for if it is the Secret and what might the response according to Next Guess be. after  
 doing statistic for all Next Guesses, we get the best Next Guess(es)  

 Now digit = 2, the possible responses = 3*2 = 6, and pretend after the previous guess,  
 after updating candidateList (via removing the impossible candidates), and we got
 6 candidates left.     
 We'd better choose the one, which Possible Kinds of Different Responses is 6, so after  
 next response, we can remove the other candidates which might give either of the other 5  
 responses according to the one we just chose to guess.  

 ### More practical explanation

 we found candidate[3] will get response as shown below   
 
 | response             | 0A0B |  0A1B  |  0A2B  |  1A0B  |  1A1B  |  2A0B                                     |
 | -------------------- | ---- | ------ | ------ | ------ | ------ | ----------------------------------------- |
 | count                |  1   |   1    |   1    |   1    |   1    |  1                                        |
 | (index in candidate,</br>for example)         |  0   |   1    |   2    |   4    |   5    | 3 (if we guess candidate[3] and get 2A0B</br>the index of 2A0B should be 3)         |

 so if we guess candidate[3], and get a 0A2B response, after the candidateList updated,  
 there will be only one candidate left, old candidate[2], and this should be the Secret.  


 The second question is Which to choose between the below two situations and How  

 case1  
 
 | response | 0A0B | 0A1B | 0A2B | 1A0B | 1A1B | 2A0B |
 | -------- | ---- | ---- | ---- | ---- | ---- | ---- |
 | count    |  0   |  1   |  1   |  6   |  1   |  1   |

 case2  
 
 | response | 0A0B | 0A1B | 0A2B | 1A0B | 1A1B | 2A0B |
 | -------- | ---- | ---- | ---- | ---- | ---- | ---- |
 | count    |  0   |  0   |  3   |  3   |  3   |  1   |


 It's the choice between average case and worst case.  
 I go worst case, and I'm doing 1+1+36+1+1 > 9+9+9+1 , and I'll choose case2  


 ### What if the value calculated the same

 Just pick the first, maybe we can do some calculate recursively, but not for now.
 
 ### Result
 
 By running the Benchmark.java in the unit test, If we first guess 0123
 
 with all of the 5040 secrets(4 digits)
 we can guess the secret with 1~8times  
 [ 1 13 109 645 2055 1923 291 3 ]
 
