package cy.ac.ucy.cs.anyplace.lib.android.utils.network

class NetUtils {

  companion object {
    const val MAX_PORT_NUMBER = 65534

    fun isValidPort(value: String?): Boolean {
      if (String.isNumber(value)) {
        val num = value!!.toInt()
        return num in 1..MAX_PORT_NUMBER
      }
      return false
    }

    fun isValidProtocol(value: String?): Boolean {
      value.let {
        return when(value) {
          "https" -> true
          "http" -> true
          else -> false
        }
      }
    }

  }
}