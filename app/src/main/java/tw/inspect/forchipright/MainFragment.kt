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
import org.jetbrains.anko.info
import org.jetbrains.anko.runOnUiThread


const val POOL_SIZE = 10
const val MULTI_THREAD_WHEN_LONG = true
const val BETTER_GUESS_WHEN_INT = true
const val INTENSIVE = true

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
                    tag = i
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

            check(getChildAt(savedInstanceState?.get("digits") as Int? ?: 0).id)

        }


        button_main_start.setOnClickListener({
            it.isEnabled = false
            text_view_main_output.text = ""
            Thread {

                view!!.findViewById<RadioButton>(
                        radio_group_main_digits.checkedRadioButtonId).tag as Int + 1.also {

                    GuessServer(GameServer(it, ::out),it, ::out, ::guessFinish).startJob()

                }


            }.start()


        })

        text_view_main_output.movementMethod = ScrollingMovementMethod()

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putInt("digits", view!!.findViewById<RadioButton>(
                radio_group_main_digits.checkedRadioButtonId).tag as Int)
        super.onSaveInstanceState(outState)

    }

    private fun out(string: String) {
        runOnUiThread {
            text_view_main_output.apply {
                append(string)
                append("\n")
            }
        }
        info { string }
    }

    private fun guessFinish() {
        runOnUiThread {
            button_main_start.isEnabled = true
        }
    }

}
