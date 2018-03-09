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
                            setBackgroundColor(resources.getColor(R.color.background))
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


        fun betterGuess(matchList: List<Int>): Int {

            var max = 0
            val candidate = mutableListOf<Int>()

            matchList.forEach { pretendedResponse ->
                mutableMapOf<Pair<Int, Int>, Int>().also { distributedMap ->
                    matchList.forEach {
                        var a = 0
                        var b = 0
                        val list = pretendedResponse.toList()
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
                    when{
                        this > max -> {
                            max = pretendedResponse
                            candidate.clear()
                            candidate.add(pretendedResponse)
                        }
                        this == max -> candidate.add(pretendedResponse)
                    }

                }
            }

            //return the first element which may lead to most kinds of result
            return candidate[0]
        }






        out("start time: ${simpleDateFormat.format(overallStart)}")
        val gameServer = GameServer(digitCount, ::out)
        var perEachStart = elapsedRealtime()


        //takes about 12 sec for POOL_SIZE==10&&digitCount==10 to genList()
        //takes about 16 sec for POOL_SIZE==10&&digitCount==10 as overallTime
        //takes about 9 sec for POOL_SIZE==10&&digitCount==9 as overallTime
        fun guessNumberLongMulti() {
            val bigMatchLists = MutableList(POOL_SIZE, { mutableListOf<Long>() })

            fun genList(usableNumbers: List<Int>, current: Long, threadNumber: Int) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        bigMatchLists[threadNumber].add(current + it)
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
                            if (bigMatchLists.isEmpty()) {
                                return@Thread
                            }
                            bigMatchLists[it] = bigMatchLists[it].filter {

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


                    //find the first non-empty list in big MatchLists and set the guessList to theList[0]
                    var index = 0
                    while (bigMatchLists[index].isEmpty()) {
                        index++
                    }

                    guessList = bigMatchLists[index][0].toList()

                }


            }


        }

        //takes about 17 sec for POOL_SIZE==10&&digitCount==10 to genList()
        //takes about 27 sec for POOL_SIZE==10&&digitCount==10 as overallTime
        //takes about 16 sec for POOL_SIZE==10&&digitCount==9 as overallTime
        fun guessNumberLong() {

            //the table stores all possible answers corresponding to results
            var bigMatchList = mutableListOf<Long>()

            fun genList(usableNumbers: List<Long>, current: Long) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        bigMatchList.add(current + it)
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
                    bigMatchList = bigMatchList.filter {

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


                    guessList = bigMatchList[0].toList()
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

            //the table stores all possible answers corresponding to results
            var bigMatchList = mutableListOf<Int>()

            fun genList(usableNumbers: List<Int>, current: Int) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        bigMatchList.add(current + it)
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
                    bigMatchList = bigMatchList.filter {

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

                    info { bigMatchList }

                    guessList = if (BETTER_GUESS_WHEN_INT) {
                        betterGuess(bigMatchList)
                    } else {
                        bigMatchList[0]
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
