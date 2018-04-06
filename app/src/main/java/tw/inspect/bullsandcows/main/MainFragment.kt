package tw.inspect.bullsandcows.main

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
import tw.inspect.bullsandcows.R
import tw.inspect.bullsandcows.game.GameServer
import tw.inspect.bullsandcows.game.GuessServer




const val POOL_SIZE = 10

class MainFragment : Fragment(), AnkoLogger {

    companion object {
        const val DIGITS = "DIGITS"

        var guessServer: GuessServer? = null
        var gameServer: GameServer? = null
        var secretString = ""
    }


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

            check(getChildAt(savedInstanceState?.getInt(DIGITS) ?: 0).id)

        }


        button_main_start.apply {
            setOnClickListener({
                it.isEnabled = false
                text_view_main_output.text = ""



                (view!!.findViewById<RadioButton>(
                        radio_group_main_digits.checkedRadioButtonId
                ).tag as Int + 1).also {
                    gameServer = GameServer(it)

                    StringBuilder().apply {
                        append("Secret = ")
                        (gameServer as GameServer).secret.forEach {
                            append(it)
                        }
                        append("\n")
                        secretString = toString()
                        text_view_main_output.text = secretString
                    }

                    guessServer = GuessServer(gameServer as GameServer,
                            it,
                            GuessServer.MultiThreadWhenLong.ON,
                            GuessServer.BetterGuessWhenInt.INTENSIVE,
                            ::out,
                            ::guessFinish
                    )
                    info { guessServer }

                }

                Thread {
                    guessServer!!.startJob()

                }.start()


            })

        }

        text_view_main_output.movementMethod = ScrollingMovementMethod()


    }

    override fun onResume() {
        super.onResume()

        guessServer?.apply {
            text_view_main_output.text = secretString
            threadSafeReconnect(::out, ::guessFinish).apply {
                text_view_main_output.append(first)
                button_main_start.isEnabled = second
            }
        }

    }

    override fun onPause() {
        super.onPause()
        guessServer?.threadSafeLeave()

    }

    override fun onSaveInstanceState(outState: Bundle?) {

        outState!!.apply {

            putInt(DIGITS, view!!.findViewById<RadioButton>(
                    radio_group_main_digits.checkedRadioButtonId).tag as Int)
        }
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
