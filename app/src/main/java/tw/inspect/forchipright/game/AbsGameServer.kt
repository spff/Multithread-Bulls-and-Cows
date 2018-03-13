package tw.inspect.forchipright.game

/**
 * Created by spff on 3/10/2018.
 */


abstract class AbsGameServer(private val digitCount: Int) {

    open val secret = mutableListOf<Int>()

    var count = 0
        private set


    /**
     * Should initialize the secret
     *
     * */
    abstract fun generateSecret()

    fun guess(list: List<Int>): Pair<Int, Int> {
        count++
        var a = 0
        var b = 0

        list.forEachIndexed { index, it ->
            when {
                secret[index] == it -> a++
                secret.contains(it) -> b++
            }
        }

        return Pair(a, b)
    }

}
