package tw.inspect.bullsandcows.main

import tw.inspect.bullsandcows.BasePresenter
import tw.inspect.bullsandcows.BaseView

/**
 * Created by spff on 3/13/2018.
 */
interface MainContract {

    interface View : BaseView<Presenter> {


        fun showSuccessfullySavedMessage()

    }

    interface Presenter : BasePresenter {


        //fun clearCompletedTasks()
    }
}
