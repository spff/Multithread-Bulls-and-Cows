package tw.inspect.forchipright

import java.util.*

/**
 * Created by spff on 3/10/2018.
 */


class GameServer(private val digitCount: Int, private val out: (String) -> Unit):AbsGameServer(digitCount) {

    override val secret = mutableListOf<Int>()

    init {
        generateSecret()
    }

    override fun generateSecret() {

        //shorter, but it's O(n^2)
        //(1..9).shuffled().subList(0, digitCount)

        //faster when the list is huge
        val pool = (0..9).toMutableList()
        for (i in 0 until digitCount) {
            Random().nextInt(pool.size).apply {
                secret.add(pool[this])
                pool.removeAt(this)
            }

        }

        StringBuilder().apply {
            append("THE Number = ")
            secret.forEach {
                append(it)
            }

            out(toString())
        }

    }

}
