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
            setOnCheckedChangeListener({ _, checkedId ->
                button_main_start.isEnabled = (checkedId != -1)
            })
        }

        button_main_start.setOnClickListener({
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
        }


        val gameServer = GameServer(digitCount)

        //takes about 12 sec for POOL_SIZE==10&&digitCount==10
        fun guessNumberLongMulti() {
            val lists = List(POOL_SIZE, { mutableListOf<Long>() })

            fun genList(usableNumbers: List<Int>, current: Long, threadNumber: Int) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        lists[threadNumber].add(current + it)
                    } else {
                        genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                                (current + it).shl(4), threadNumber)
                    }
                }
            }

            val threads = mutableListOf<Thread>()
            (0 until POOL_SIZE).toList().forEach {
                Thread {
                    genList((0 until POOL_SIZE).toMutableList().apply { remove(it) }.toList(),
                            it.toLong().shl(4), it)
                }.apply { threads.add(this) }.start()
            }

            threads.forEach { it.join() }
            info { lists.map { it.size }.reduce { a, b -> a + b } }

        }

        //takes about 17 sec for POOL_SIZE==10&&digitCount==10
        fun guessNumberLong() {
            val list = mutableListOf<Long>()

            fun genList(usableNumbers: List<Long>, current: Long) {
                usableNumbers.forEach {
                    if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                        list.add(current + it)
                    } else {
                        genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                                (current + it).shl(4))
                    }
                }
            }
            genList((0 until POOL_SIZE.toLong()).toList(), 0)

            info { list.size }
        }

        fun guessNumber() {
            //the table stores every possible answer
            val bigMatchList :MutableList<Int> by lazy {
                mutableListOf<Int>().apply {

                    fun genList(usableNumbers: List<Int>, current: Int) {
                        usableNumbers.forEach {
                            if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                                this.add(current + it)
                            } else {
                                genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                                        (current + it).shl(4))
                            }
                        }
                    }
                    genList((0 until POOL_SIZE).toList(), 0)

                    info { size }
                }
            }

            //feed to gameServer.guess()
            var guessList = (0 until digitCount).toList()

            var oneTimeStart = elapsedRealtime()

            while (true) loop@ {
                gameServer.guess(guessList).apply {

                    out(StringBuilder().apply {
                        append("guess: ${guessList.map { it.toString() }.reduce { a, b -> a + b }},")
                        append("result: ${first}A${second}B, spent: ")
                        append("${(elapsedRealtime() - oneTimeStart) / 1000.0} sec")
                    }.toString())
                    if (first == digitCount) {
                        return@loop
                    } else {
                        oneTimeStart = elapsedRealtime()
                        bigMatchList.forEach {

                            var a = 0
                            var b = 0

                            for (i in 0 until digitCount) {
                                when {
                                    guessList[i] == bigMatchList[0].and(15.shl(i)) -> a++
                                    guessList.contains(bigMatchList[0].and(15.shl(i))) -> b++
                                }
                            }
                            if (!(a == first && b == second)) {
                                bigMatchList.remove(it)
                            }

                        }

                        guessList = mutableListOf<Int>().apply {
                            for (i in digitCount - 1 downTo 0) {
                                this.add(bigMatchList[0].and(15.shl(i)))
                            }
                        }.toList()
                    }

                }

            }


        }

        out("start time: ${simpleDateFormat.format(overallStart)}")
        if (digitCount > 8) {
            guessNumberLongMulti()
            //guessNumberLong()
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
            append(" : ${(overallStop - overallStart) / 1000.0} sec,")
            append("end time : ${simpleDateFormat.format(overallStop)}")
        }.toString())

    }


    class GameServer(private val digitCount: Int) {

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
                    pool.remove(this)
                }

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
