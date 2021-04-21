package cy.ac.ucy.cs.anyplace.lib.android

import android.util.Log

class DBG {
  companion object {
    const val LEVEL = 2

    const val D1 = LEVEL >= 1
    const val D2 = LEVEL >= 2
    const val D3 = LEVEL >= 3
    const val D4 = LEVEL >= 4
    const val D5 = LEVEL >= 5

    const val CALLBACK = false

    // TODO put settings from AnyplaceDebug
  }
}

class LOG {
  companion object {
    const val TAG = "anyplace"

    @JvmStatic fun I(message: String) = I(TAG, message)
    @JvmStatic fun I(tag: String, message: String) = Log.i(tag, message)
    @JvmStatic fun I(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.i(TAG, message) }

    @JvmStatic fun E(message: String) = E(TAG, message)
    @JvmStatic fun E(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.e(TAG, message) }
    @JvmStatic fun E(tag: String, message: String) = Log.e(tag, message)

    @JvmStatic fun W(tag: String, message: String) = Log.w(tag, message)
    @JvmStatic fun W(message: String) = W(TAG, message)
    @JvmStatic fun W(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.w(TAG, message) }

    @JvmStatic fun D(tag: String, message: String) = Log.d(tag, message)
    @JvmStatic fun D(message: String) = D(TAG, message)
    @JvmStatic fun D(lvl: Int, message: String): () -> Unit = { if (lvl == DBG.LEVEL) Log.d(TAG, message) }

    @JvmStatic fun D1(tag: String, message: String) { if (DBG.D1) Log.d(tag, message) }
    @JvmStatic fun D2(tag: String, message: String) { if (DBG.D2) Log.d(tag, message) }
    @JvmStatic fun D3(tag: String, message: String) { if (DBG.D3) Log.d(tag, message) }
    @JvmStatic fun D4(tag: String, message: String) { if (DBG.D4) Log.d(tag, message) }
    @JvmStatic fun D5(tag: String, message: String) { if (DBG.D5) Log.d(tag, message) }

    @JvmStatic fun D1(message: String) = D1(TAG, message)
    @JvmStatic fun D2(message: String) = D2(TAG, message)
    @JvmStatic fun D3(message: String) = D3(TAG, message)
    @JvmStatic fun D4(message: String) = D4(TAG, message)
    @JvmStatic fun D5(message: String) = D5(TAG, message)
  }
}
