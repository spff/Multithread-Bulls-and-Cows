package tw.inspect.forchipright.main

import android.app.Activity
import android.os.Bundle
import org.jetbrains.anko.AnkoLogger
import tw.inspect.forchipright.R


class MainActivity : Activity(), AnkoLogger {


    lateinit var mainPresenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainFragment = fragmentManager.findFragmentById(R.id.for_fragment_main)
                ?: MainFragment().also {


                    fragmentManager.beginTransaction()
                            .replace(R.id.for_fragment_main, it)
                            .commit()

                }

        /*mainPresenter = MainPresenter(
                Injection.provideTasksRepository(applicationContext), mainFragment)*/

    }

}
