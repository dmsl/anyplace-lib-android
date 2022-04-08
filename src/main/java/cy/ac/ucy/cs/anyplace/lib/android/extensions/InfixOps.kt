package cy.ac.ucy.cs.anyplace.lib.android.extensions

// Infix operators (Kotlin feature)
fun String.Companion.isNumber(str: String?) : Boolean =
  if(str.isNullOrEmpty()) false else str.all { Character.isDigit(it) }

