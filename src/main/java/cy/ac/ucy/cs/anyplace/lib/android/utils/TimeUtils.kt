package cy.ac.ucy.cs.anyplace.lib.android.utils

/** Time Utils */
object uTime {

  fun getSecondsRounded(num: Float, maxAllowed: Int): String {
    var rounded = num.toInt() + 1
    if (rounded > maxAllowed) rounded = maxAllowed
    return rounded.toString()
  }

  fun getSecondsPretty(num: Float): String {
    val res = "%.1f".format(num) + "s"
    if (res.length > 4) return "0.0s"
    return res
  }
}