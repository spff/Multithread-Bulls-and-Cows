package tw.inspect.forchipright

import android.os.SystemClock.elapsedRealtime
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by spff on 3/10/2018.
 */

class GuessServer(private val gameServer: GameServer,
                  private val digitCount: Int,
                  private val out: (String) -> Unit,
                  private val guessFinish: () -> Unit) : AnkoLogger {


    /**
     * 1. Start GameServer
     *
     * 1.1. determine which function to call based on "digits"
     * 1.1.1 If digits > 8 determine whether to do in multi-thread up to user's choice
     *
     * 2. Generate candidateList which store all possible permute
     * 3. Guess 0123 or whatever, doesn't matter
     * 4. Get the result, if correct, end.
     * 5. Update the candidateList
     * 6. Determine next guess(might either choose the first or via betterGuess
     *    from the candidateList depends on user's choice)
     *
     * 6.1. If betterGuess is chosen, determine whether to do in multi-thread, and how DEEP to guess
     *      when there are multiple choices with the same VALUE(See the document below for detail)
     *
     * 7. Guess, and loop back to step 4.
     *
     * */
    fun startJob() {

        val gregorianCalendar = GregorianCalendar()
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            timeZone = gregorianCalendar.timeZone
        }

        val overallStart = GregorianCalendar().timeInMillis


        fun Long.toList(): List<Int> {
            val newGuessList = mutableListOf<Int>()
            for (i in digitCount - 1 downTo 0) {
                newGuessList.add(ushr(i * 4).and(15).toInt())
            }
            return newGuessList.toList()
        }

        fun Int.toList(): List<Int> {
            val newGuessList = mutableListOf<Int>()
            for (i in digitCount - 1 downTo 0) {
                newGuessList.add(ushr(i * 4).and(15))
            }
            return newGuessList.toList()
        }


        /**
         * Beside just choose the first from the candidateList, we calculate the Discrete Degree
         * of each candidate in the candidateList.
         *
         * Take digits = 3 for example, every choice may get 0A0B 0A1B 0A2B 0A3B 1A0B 1A1B 1A2B 2A0B
         * 2A1B and 3A0B, all 9 kinds of possible responses
         *
         * A+B less or equal 3
         * -> A+B+LEFT = 3
         * -> [ A ][ + ][ B ][ + ][ LEFT ] put 3 [1]s into [ A ] or [ B ] or [ LEFT ]
         * -> 3*3 = 9
         *
         *
         * The strategy is
         *
         * Whenever the Secret is, we should choose the one which may get the most
         * Kinds of Different Possible Responses (Discrete Degree relative to 0) so we may
         * AVERAGELY eliminate most impossible candidates from the next response.
         *
         * To do so, we're using a nested loop, the outer stands for Next Guess, the inner stands
         * for if it is the Secret and what might the response according to Next Guess be. after
         * doing statistic for all Next Guesses, we get the best Next Guess(es)
         *
         * Now digit = 2, the possible responses = 3*2 = 6, and pretend after the previous guess,
         * after updating candidateList (via removing the impossible candidates), and we got
         * 6 candidates left.
         * We'd better choose the one, which Possible Kinds of Different Responses is 6, so after
         * next response, we can remove the other candidates which might give either of the other 5
         * responses according to the one we just chose to guess.
         *
         * More practical explanation
         *
         * we found candidate[3] will get response as shown below
         *
         * response           0A0B 0A1B 0A2B 1A0B 1A1B 2A0B
         * count                1    1    1    1    1    1
         * (index in candidate,                           (if we guess candidate[3] and get 2A0B
         * for example)         0    1    2    4    5    3 the index of 2A0B should be 3)
         *
         * so if we guess candidate[3], and get a 0A2B response, after the candidateList updated,
         * there will be only one candidate left, old candidate[2], and this should be the Secret.
         *
         *
         * The second question is Which to choose between the below two situations and How
         *
         * case1
         * response           0A0B 0A1B 0A2B 1A0B 1A1B 2A0B
         * count                0    1    1    6    1    1
         *
         * case2
         * response           0A0B 0A1B 0A2B 1A0B 1A1B 2A0B
         * count                0    0    3    3    3    1
         *
         * It's the choice between average case and worst case.
         * I go worst case, and I'm doing 1+1+36+1+1 > 9+9+9+1 , and I'll choose case2
         *
         *
         * What if the value calculated the same
         *
         * Just pick the first, maybe we can do some calculate recursively, but not for now.
         *
         * */


        /**
         * This method sums square of number in each class as our Discrete Degree
         *
         * */


        fun betterGuessIntensive(candidateList: List<Int>): Int {

            var min = Int.MAX_VALUE
            var minLong = Long.MAX_VALUE

            val bests = mutableListOf<Int>()

            //46341^2 will overflow
            //and the biggest case would be 0 ... + 1^2 + 46340^2
            //so 46341 is OK but 46342 might not
            val mapLong = (candidateList.size > 46341)
            info { "mapLong $mapLong" }

            candidateList.forEach { nextGuess ->
                mutableMapOf<Pair<Int, Int>, Int>().also { distributedMap ->
                    candidateList.forEach {
                        var a = 0
                        var b = 0
                        val list = nextGuess.toList()
                        for (i in 0 until digitCount) {
                            when {
                                list[i] == it.ushr((digitCount - i - 1) * 4).and(15) -> a++
                                list.contains(it.ushr((digitCount - i - 1) * 4).and(15)) -> b++
                            }
                        }
                        distributedMap.apply {
                            Pair(a, b).also {
                                put(it, getOrDefault(it, 0) + 1)
                            }
                        }

                    }


                }.values.apply {
                    if (mapLong) {
                        map {
                            it.toLong() * it
                        }.reduce { a, b -> a + b }.apply {
                            //info { "$this $minLong $nextGuess" }

                            when {
                                this < minLong -> {
                                    minLong = this
                                    bests.clear()
                                    bests.add(nextGuess)
                                }
                                this == minLong -> bests.add(nextGuess)
                            }

                        }
                    } else {
                        map {
                            it * it
                        }.reduce { a, b -> a + b }.apply {
                            //info { "$this $min $nextGuess" }

                            when {
                                this < min -> {
                                    min = this
                                    bests.clear()
                                    bests.add(nextGuess)
                                }
                                this == min -> bests.add(nextGuess)
                            }

                        }
                    }
                }
            }

            //return the first element which may lead to most kinds of result
            return bests[0]
        }

        /**This method count non-zero classes instead of summing square of number in each class as
         * our Discrete Degree
         *
         *
         * */
        fun betterGuess(candidateList: List<Int>): Int {

            var max = 0
            val bests = mutableListOf<Int>()

            candidateList.forEach { nextGuess ->
                mutableMapOf<Pair<Int, Int>, Int>().also { distributedMap ->
                    candidateList.forEach {
                        var a = 0
                        var b = 0
                        val list = nextGuess.toList()
                        for (i in 0 until digitCount) {
                            when {
                                list[i] == it.ushr((digitCount - i - 1) * 4).and(15) -> a++
                                list.contains(it.ushr((digitCount - i - 1) * 4).and(15)) -> b++
                            }
                        }
                        distributedMap.apply {
                            Pair(a, b).also {
                                put(it, getOrDefault(it, 0) + 1)
                            }
                        }

                    }

                }.values.filter { it > 0 }.size.apply {
                    //info { "$this $max $nextGuess" }

                    when {
                        this > max -> {
                            max = this
                            bests.clear()
                            bests.add(nextGuess)
                        }
                        this == max -> bests.add(nextGuess)
                    }

                }
            }

            //return the first element which may lead to most kinds of result
            return bests[0]
        }






        out("start time: ${simpleDateFormat.format(overallStart)}")
        var perEachStart = elapsedRealtime()


        //takes about 12 sec for POOL_SIZE==10&&digitCount==10 to genList()
        //takes about 16 sec for POOL_SIZE==10&&digitCount==10 as overallTime
        //takes about 9 sec for POOL_SIZE==10&&digitCount==9 as overallTime
        fun guessNumberLongMulti() {
            val candidateLists = MutableList(POOL_SIZE, { mutableListOf<Long>() })

            fun genList(usableNumbers: List<Int>, current: Long, threadNumber: Int) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        candidateLists[threadNumber].add(current + it)
                    } else {
                        genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                                (current + it).shl(4), threadNumber)
                    }
                }
            }

            val threads0 = mutableListOf<Thread>()
            (0 until POOL_SIZE).toList().forEach {
                Thread {
                    genList((0 until POOL_SIZE).toMutableList().apply { remove(it) }.toList(),
                            it.toLong().shl(4), it)
                }.apply { threads0.add(this) }.start()
            }

            threads0.forEach { it.join() }


            //feed to gameServer.guess()
            var guessList = (0 until digitCount).toList()

            while (true) {
                val pair = gameServer.guess(guessList)

                out(StringBuilder().apply {
                    append("guess: ${guessList.map { it.toString() }.reduce { a, b -> a + b }}, ")
                    append("result: ${pair.first}A${pair.second}B, spent: ")
                    append("${(elapsedRealtime() - perEachStart) / 1000.0} sec")
                }.toString())
                if (pair.first == digitCount) {
                    return
                } else {
                    perEachStart = elapsedRealtime()
                    val threads1 = mutableListOf<Thread>()


                    (0 until POOL_SIZE).toList().forEach {
                        Thread {
                            if (candidateLists.isEmpty()) {
                                return@Thread
                            }
                            candidateLists[it] = candidateLists[it].filter {

                                var a = 0
                                var b = 0

                                for (i in 0 until digitCount) {
                                    when {
                                        guessList[i] == it.ushr((digitCount - i - 1) * 4).and(15).toInt() -> a++
                                        guessList.contains(it.ushr((digitCount - i - 1) * 4).and(15).toInt()) -> b++
                                    }
                                }
                                a == pair.first && b == pair.second

                            }.toMutableList()


                        }.apply { threads1.add(this) }.start()
                    }

                    threads1.forEach { it.join() }


                    //find the first non-empty list in candidateLists and set the guessList to theList[0]
                    var index = 0
                    while (candidateLists[index].isEmpty()) {
                        index++
                    }

                    guessList = candidateLists[index][0].toList()

                }


            }


        }

        //takes about 17 sec for POOL_SIZE==10&&digitCount==10 to genList()
        //takes about 27 sec for POOL_SIZE==10&&digitCount==10 as overallTime
        //takes about 16 sec for POOL_SIZE==10&&digitCount==9 as overallTime
        fun guessNumberLong() {

            //the table stores all possible Secrets
            //and will be updated each time getting response.
            var candidateList = mutableListOf<Long>()

            fun genList(usableNumbers: List<Long>, current: Long) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        candidateList.add(current + it)
                    } else {
                        genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                                (current + it).shl(4))
                    }
                }
            }
            genList((0 until POOL_SIZE.toLong()).toList(), 0)


            //feed to gameServer.guess()
            var guessList = (0 until digitCount).toList()

            while (true) {
                val pair = gameServer.guess(guessList)

                out(StringBuilder().apply {
                    append("guess: ${guessList.map { it.toString() }.reduce { a, b -> a + b }}, ")
                    append("result: ${pair.first}A${pair.second}B, spent: ")
                    append("${(elapsedRealtime() - perEachStart) / 1000.0} sec")
                }.toString())
                if (pair.first == digitCount) {
                    return
                } else {
                    perEachStart = elapsedRealtime()
                    candidateList = candidateList.filter {

                        var a = 0
                        var b = 0

                        for (i in 0 until digitCount) {
                            when {
                                guessList[i] == it.ushr((digitCount - i - 1) * 4).and(15).toInt() -> a++
                                guessList.contains(it.ushr((digitCount - i - 1) * 4).and(15).toInt()) -> b++
                            }
                        }
                        a == pair.first && b == pair.second

                    }.toMutableList()


                    guessList = candidateList[0].toList()
                }


            }
        }

        /**for digitCount <= 8, we use a 32-bit Int to store one guess
         *
         * for example
         *
         * [2,3,4,5] will be
         * 0x0 0x0 0x0 0x0 0x2 0x3 0x4 0x5
         *
         * */
        fun guessNumber() {

            //the table stores all possible Secrets
            //and will be updated each time getting response.
            var candidateList = mutableListOf<Int>()

            fun genList(usableNumbers: List<Int>, current: Int) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        candidateList.add(current + it)
                    } else {
                        genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                                (current + it).shl(4))
                    }
                }
            }
            genList((0 until POOL_SIZE).toList(), 0)


            //feed to gameServer.guess()
            var guessList = (0 until digitCount).toList()

            while (true) {
                val pair = gameServer.guess(guessList)

                out(StringBuilder().apply {
                    append("guess: ${guessList.map { it.toString() }.reduce { a, b -> a + b }}, ")
                    append("result: ${pair.first}A${pair.second}B, spent: ")
                    append("${(elapsedRealtime() - perEachStart) / 1000.0} sec")
                }.toString())
                if (pair.first == digitCount) {
                    return
                } else {
                    perEachStart = elapsedRealtime()
                    candidateList = candidateList.filter {

                        var a = 0
                        var b = 0

                        for (i in 0 until digitCount) {
                            when {
                                guessList[i] == it.ushr((digitCount - i - 1) * 4).and(15) -> a++
                                guessList.contains(it.ushr((digitCount - i - 1) * 4).and(15)) -> b++
                            }
                        }
                        a == pair.first && b == pair.second

                    }.toMutableList()

                    info { candidateList }

                    guessList = if (BETTER_GUESS_WHEN_INT) {
                        if (INTENSIVE) {
                            betterGuessIntensive(candidateList)
                        } else {
                            betterGuess(candidateList)
                        }
                    } else {
                        candidateList[0]
                    }.toList()
                }


            }


        }

        if (digitCount > 8) {
            if (MULTI_THREAD_WHEN_LONG) {
                guessNumberLongMulti()
            } else {
                guessNumberLong()
            }
        } else {
            guessNumber()
        }
        val overallStop = GregorianCalendar().timeInMillis
        out(StringBuilder().apply {
            append("guess ${gameServer.count} time${if (gameServer.count > 1) {
                "s"
            } else {
                ""
            }}, overall time")
            append(" : ${(overallStop - overallStart) / 1000.0} sec, ")
            append("end time : ${simpleDateFormat.format(overallStop)}")
        }.toString())

        guessFinish()


    }


}