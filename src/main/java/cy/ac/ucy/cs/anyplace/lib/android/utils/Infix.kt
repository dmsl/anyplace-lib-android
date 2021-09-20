package cy.ac.ucy.cs.anyplace.lib.android.utils.network

// Infix operators

fun String.Companion.isNumber(str: String?) : Boolean =
  if(str.isNullOrEmpty()) false else str.all { Character.isDigit(it) }

