package tw.inspect.forchipright

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        savedInstanceState ?: fragmentManager.beginTransaction()
                .replace(R.id.for_fragment_main, MainFragment())
                .commit()
    }


}
