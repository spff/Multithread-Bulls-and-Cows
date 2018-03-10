package tw.inspect.forchipright

import java.util.*

/**
 * Created by spff on 3/10/2018.
 */


class GameServer(private val digitCount: Int, out: (String) -> Unit) {

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

            out(toString())
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
