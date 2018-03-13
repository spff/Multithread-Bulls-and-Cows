package tw.inspect.forchipright.main

import tw.inspect.forchipright.BasePresenter
import tw.inspect.forchipright.BaseView

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
