package com.example.lockpay

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONObject

object Verifier {
    /**
     * Rebuilds the exact bytes the server signed:
     * json.dumps(payload, sort_keys=True, separators=(",", ":"))
     * Keys sorted: action, device_id, expires_at, id, issued_at, nonce.
     */
    fun verify(p: JSONObject, signatureB64: String): Boolean = try {
        val msg = ("{\"action\":\"${p.getString("action")}\"," +
            "\"device_id\":${p.getLong("device_id")}," +
            "\"expires_at\":${p.getLong("expires_at")}," +
            "\"id\":${p.getLong("id")}," +
            "\"issued_at\":${p.getLong("issued_at")}," +
            "\"nonce\":\"${p.getString("nonce")}\"}").toByteArray(Charsets.UTF_8)
        val pub = Base64.decode(BuildConfig.PUBLIC_KEY, Base64.DEFAULT)
        val signer = Ed25519Signer()
        signer.init(false, Ed25519PublicKeyParameters(pub, 0))
        signer.update(msg, 0, msg.size)
        signer.verifySignature(Base64.decode(signatureB64, Base64.DEFAULT))
    } catch (e: Exception) {
        false
    }
}
