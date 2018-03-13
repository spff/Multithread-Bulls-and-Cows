package tw.inspect.forchipright

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tw.inspect.forchipright.game.AbsGameServer
import tw.inspect.forchipright.game.GuessServer
import tw.inspect.forchipright.main.POOL_SIZE
import kotlin.math.ceil
import kotlin.math.min

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Benchmark {

    val digitCount = 4

    @Test
    fun count() {


        val threads = arrayListOf<Thread>()
        val intArrays = arrayListOf<IntArray>()


        val secrets = mutableListOf<Int>()

        fun genList(usableNumbers: List<Int>, current: Int) {
            usableNumbers.forEach {
                if (POOL_SIZE - usableNumbers.size == digitCount - 1) {
                    secrets.add(current + it)
                } else {
                    genList(usableNumbers.toMutableList().apply { remove(it) }.toList(),
                            (current + it).shl(4))
                }
            }
        }
        genList((0 until POOL_SIZE).toList(), 0)

        val THREADS = 4
        for (i in 0 until THREADS) {
            val intArray = IntArray(10, { 0 })
            val low = ceil(secrets.size.toFloat() / THREADS).toInt() * i
            val high = min((secrets.size.toFloat() / THREADS).toInt() * (i + 1), secrets.size)

            println("$low $high")

            Thread {
                secrets.subList(low, high).forEach {
                    object : AbsGameServer(digitCount) {

                        override fun generateSecret() {}
                        override val secret = it.toList().toMutableList()

                    }.apply {
                        GuessServer(this, digitCount,
                                GuessServer.MultiThreadWhenLong.ON,
                                GuessServer.BetterGuessWhenInt.INTENSIVE, {}, {}).startJob()
                        if (count == 8) {
                            StringBuilder().apply {
                                append("[")
                                it.toList().forEach {
                                    append(it)
                                    append(" ")
                                }
                                append("]")
                                println(toString())
                            }
                        }
                        intArray[this.count]++
                    }
                }
                intArrays.add(intArray)
            }.apply {
                start()
                threads.add(this)
            }
        }

        threads.forEach { it.join() }

        val newIntArray = IntArray(10, { 0 })
        for (i in 0 until newIntArray.size) {
            intArrays.forEach {
                newIntArray[i] += it[i]
            }
        }
        StringBuilder().apply {
            append("[")
            newIntArray.forEach {
                append(it)
                append(" ")
            }
            append("]")
            println(toString())
        }


    }


    fun Int.toList(): List<Int> {
        val newGuessList = mutableListOf<Int>()
        for (i in digitCount - 1 downTo 0) {
            newGuessList.add(ushr(i * 4).and(15))
        }
        return newGuessList.toList()
    }


}
