package tw.inspect.forchipright

import android.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock.elapsedRealtime
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.runOnUiThread
import java.text.SimpleDateFormat
import java.util.*


const val POOL_SIZE = 10
const val MULTI_THREAD_WHEN_LONG = true
const val BETTER_GUESS_WHEN_INT = true

class MainFragment : Fragment(), AnkoLogger {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radio_group_main_digits.apply {
            for (i in 0 until POOL_SIZE) {
                addView(RadioButton(context).apply {
                    tag = i + 1
                    text = (i + 1).toString()
                    setTextColor(resources.getColor(R.color.text))
                    width = 60
                    textSize = 22f
                    gravity = Gravity.CENTER
                    buttonDrawable = null
                    setOnCheckedChangeListener({ _, isChecked ->
                        if (isChecked) {
                            setBackgroundColor(resources.getColor(R.color.background_enabled))
                        } else {
                            setBackgroundColor(Color.TRANSPARENT)
                        }

                    })
                })
            }

            check(getChildAt(0).id)

        }

        button_main_start.setOnClickListener({
            it.isEnabled = false
            text_view_main_output.text = ""
            Thread {
                startJob(view!!.findViewById<RadioButton>(
                        radio_group_main_digits.checkedRadioButtonId).tag as Int)
            }.start()


        })

        text_view_main_output.movementMethod = ScrollingMovementMethod()

    }

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
    private fun startJob(digitCount: Int) {

        val gregorianCalendar = GregorianCalendar()
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            timeZone = gregorianCalendar.timeZone
        }

        val overallStart = GregorianCalendar().timeInMillis


        fun out(string: String) {
            runOnUiThread {
                text_view_main_output.apply {
                    append(string)
                    append("\n")
                }
            }
            info { string }
        }

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
         * Kinds of Different Possible Responses (Discrete Degree) so we may eliminate most
         * impossible candidates from the next response.
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

        fun betterGuess(candidateList: List<Int>): Int {

            var min = Int.MAX_VALUE
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

                    //info {distributedMap}

                }.values.map { it * it }.reduce{a, b -> a+b}.apply {
                    info { "$this $min $nextGuess" }

                    when{
                        this < min -> {
                            min = this
                            bests.clear()
                            bests.add(nextGuess)
                        }
                        this == min -> bests.add(nextGuess)
                    }

                    //info{ bests }

                }
            }

            //return the first element which may lead to most kinds of result
            return bests[0]
        }






        out("start time: ${simpleDateFormat.format(overallStart)}")
        val gameServer = GameServer(digitCount, ::out)
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
                        betterGuess(candidateList)
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

        runOnUiThread {
            button_main_start.isEnabled = true
        }

    }


    class GameServer(private val digitCount: Int, outputter: (String) -> Unit) {

        private val mutableList = mutableListOf<Int>()
        var count = 0
            private set

        init {

            //shorter, but it's O(n^2)
            //(1..9).shuffled().subList(0, digitCount)

            //faster when the list is huge
            val pool = (0..9).toMutableList()
            for (i in 0 until digitCount) {
                Random().nextInt(pool.size).apply {
                    mutableList.add(pool[this])
                    pool.removeAt(this)
                }

            }

            StringBuilder().apply {
                append("THE Number = ")
                mutableList.forEach {
                    append(it)
                }

                outputter(toString())
            }

        }

        fun guess(list: List<Int>): Pair<Int, Int> {
            count++
            var a = 0
            var b = 0

            list.forEachIndexed { index, it ->
                when {
                    mutableList[index] == it -> a++
                    mutableList.contains(it) -> b++
                }
            }

            return Pair(a, b)
        }

    }

}
