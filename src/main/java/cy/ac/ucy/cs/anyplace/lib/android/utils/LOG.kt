package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.util.Log
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD

class DBG {
  companion object {
    const val LEVEL = 2

    const val D1 = LEVEL >= 1
    const val D2 = LEVEL >= 2
    const val D3 = LEVEL >= 3
    const val D4 = LEVEL >= 4
    const val D5 = LEVEL >= 5

    const val CO5 = true
    const val CTR = true

    const val CVM = false

    const val uim= false

    const val FLD = false
    const val BFnt45 = false
    const val LCLG = false
    const val BN11c = false
  }
}

/**
 * EXPERIMENT MODE
 */
class EXP {
  companion object {
    const val LOCALIZATION = true
  }
}

class LOG {
  companion object {
    const val TAG = "anyplace"
    private fun _tag(tag: String ) : String = "$TAG/${tag}"

    @JvmStatic fun I(tag: String, message: String) = Log.i(_tag(tag), message)
    @JvmStatic fun I(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.i(TAG, message) }
    @JvmStatic fun I(message: String) = I(TAG, message)

    @JvmStatic fun E(message: String) = E(TAG, message)
    @JvmStatic fun E(tag: String, message: String) = Log.e(_tag(tag), message)
    @JvmStatic fun E(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.e(TAG, message) }

    @JvmStatic fun E(e: Exception) = E(TAG, "ERROR:" + e.javaClass + ": " + e.cause + ": " + e.message)
    @JvmStatic fun E(tag: String, msg: String, e: Exception) = E(_tag(tag), "ERROR:"  + msg + ": " + e.javaClass + ": " + e.cause + ": " + e.message)
    @JvmStatic fun E(tag: String, e: Exception) = E(_tag(tag), "ERROR:" + e.javaClass + ": " + e.cause + ":\n" + e.message)

    @JvmStatic fun W(tag: String, message: String) = Log.w(_tag(tag), message)
    @JvmStatic fun W(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.w(TAG, message) }
    @JvmStatic fun W(message: String) = W(TAG, message)

    @JvmStatic fun D(tag: String, message: String) = Log.d(_tag(tag), message)
    @JvmStatic fun D(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.d(TAG, message) }
    @JvmStatic fun D(message: String) = D(TAG, message)

    @JvmStatic fun V(tag: String, message: String) = Log.v(_tag(tag), message)
    @JvmStatic fun V(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.v(TAG, message) }
    @JvmStatic fun V(message: String) = V(TAG, message)

    @JvmStatic fun D1(tag: String, message: String) { if (DBG.D1) Log.d(_tag(tag), message) }
    @JvmStatic fun D2(tag: String, message: String) { if (DBG.D2) Log.d(_tag(tag), message) }
    @JvmStatic fun D3(tag: String, message: String) { if (DBG.D3) Log.d(_tag(tag), message) }
    @JvmStatic fun D4(tag: String, message: String) { if (DBG.D4) Log.d(_tag(tag), message) }
    @JvmStatic fun D5(tag: String, message: String) { if (DBG.D5) Log.d(_tag(tag), message) }

    @JvmStatic fun V1(tag: String, message: String) { if (DBG.D1) Log.v(_tag(tag), message) }
    @JvmStatic fun V2(tag: String, message: String) { if (DBG.D2) Log.v(_tag(tag), message) }
    @JvmStatic fun V3(tag: String, message: String) { if (DBG.D3) Log.v(_tag(tag), message) }
    @JvmStatic fun V4(tag: String, message: String) { if (DBG.D4) Log.v(_tag(tag), message) }
    @JvmStatic fun V5(tag: String, message: String) { if (DBG.D5) Log.v(_tag(tag), message) }

    @JvmStatic fun D1(message: String) = D1(TAG, message)
    @JvmStatic fun D2(message: String) = D2(TAG, message)
    @JvmStatic fun D3(message: String) = D3(TAG, message)
    @JvmStatic fun D4(message: String) = D4(TAG, message)
    @JvmStatic fun D5(message: String) = D5(TAG, message)

    @JvmStatic fun V1(message: String) = V1(TAG, message)
    @JvmStatic fun V2(message: String) = V2(TAG, message)
    @JvmStatic fun V3(message: String) = V3(TAG, message)
    @JvmStatic fun V4(message: String) = V4(TAG, message)
    @JvmStatic fun V5(message: String) = V5(TAG, message)

    /** Logs just the TAG and the METHOD name: example: anyplace/<CLASS>/<METHOD> */
    @JvmStatic fun E() = E(TAG_METHOD, "")
    @JvmStatic fun W() = W(TAG_METHOD, "")
    @JvmStatic fun I() = I(TAG_METHOD, "")
    @JvmStatic fun D() = D(TAG_METHOD, "")
    @JvmStatic fun V() = V(TAG_METHOD, "")

    @JvmStatic fun D1() { if (DBG.D1) D() }
    @JvmStatic fun D2() { if (DBG.D2) D() }
    @JvmStatic fun D3() { if (DBG.D3) D() }
    @JvmStatic fun D4() { if (DBG.D4) D() }
    @JvmStatic fun D5() { if (DBG.D5) D() }

    @JvmStatic fun V1() { if (DBG.D1) V() }
    @JvmStatic fun V2() { if (DBG.D2) V() }
    @JvmStatic fun V3() { if (DBG.D3) V() }
    @JvmStatic fun V4() { if (DBG.D4) V() }
    @JvmStatic fun V5() { if (DBG.D5) V() }
  }
}
