package tw.inspect.forchipright

import android.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * A placeholder fragment containing a simple view.
 */
class MainFragment : Fragment(), AnkoLogger {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radio_group_main_digits.apply {
            for (i in 0 until 10){
                addView(RadioButton(context).apply{
                    tag = i+1
                    text = (i+1).toString()
                    setTextColor(resources.getColor(R.color.text))
                    width = 60
                    textSize = 22f
                    gravity = Gravity.CENTER
                    buttonDrawable = null
                    setOnCheckedChangeListener({_, isChecked ->
                        if(isChecked){
                            setBackgroundColor(resources.getColor(R.color.background))
                        } else{
                            setBackgroundColor(Color.TRANSPARENT)
                        }

                    })
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        info{"onDestroy"}
    }
}
