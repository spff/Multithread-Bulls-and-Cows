package tw.inspect.forchipright

import android.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.AnkoLogger
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

        val start = gregorianCalendar.timeInMillis


        fun out(string: String) {
            runOnUiThread {
                text_view_main_output.apply {
                    append(string)
                    append("\n")
                }
            }
        }


        val gameServer = GameServer(digitCount)


        val isAOrB = mutableListOf<Int>()
        lateinit var pairAB: Pair<Int, Int>

        fun confirmNumbers() {
            val notSure = (0 until POOL_SIZE).toMutableList()
            val notANorB = mutableListOf<Int>()
            var goodStart = -1

            (0 until POOL_SIZE step digitCount).forEach {

                gameServer.guess((it until it + digitCount).map { it % POOL_SIZE }.toList()).run {
                    when {
                        first + second == digitCount -> {
                            (it until it + digitCount).forEach { isAOrB.add(it) }
                            pairAB = Pair(first, second)
                            return
                        }
                        first + second == 0 ->
                            (it until it + digitCount).forEach { notSure.remove(it);notANorB.add(it) }

                        first + second > digitCount / 2 ->
                            goodStart = it
                    }
                }
            }


        }

        fun permute() {

        }

        out("start time: ${simpleDateFormat.format(start)}")
        confirmNumbers()
        permute()
        val stop = gregorianCalendar.timeInMillis
        out(StringBuilder().apply {
            append("guess ${gameServer.count} time${if (gameServer.count > 1) {
                "s"
            } else {
                ""
            }}, overall time")
            append(" : ${(stop - start) / 1000.0} sec,")
            append("end time : ${simpleDateFormat.format(stop)}")
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
