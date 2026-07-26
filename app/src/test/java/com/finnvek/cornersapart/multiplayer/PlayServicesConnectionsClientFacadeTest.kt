package com.finnvek.cornersapart.multiplayer

import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import sun.misc.Unsafe

class PlayServicesConnectionsClientFacadeTest {
    @Test
    fun fileAndStreamPayloadsAreClosedAndRejected() {
        listOf(Payload.Type.FILE, Payload.Type.STREAM).forEach { payloadType ->
            val callback = mockk<NearbyPayloadCallback>(relaxed = true)
            val payload = mockk<Payload>(relaxed = true)
            every { payload.type } returns payloadType
            val playServicesCallback = createPlayServicesCallback(callback)

            playServicesCallback.onPayloadReceived("endpoint-1", payload)

            verify(exactly = 1) { payload.close() }
            verify(exactly = 1) { callback.onPayloadFailure("endpoint-1") }
            verify(exactly = 0) { callback.onBytesPayload(any(), any()) }
        }
    }

    private fun createPlayServicesCallback(callback: NearbyPayloadCallback): PayloadCallback {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as Unsafe
        val facade = unsafe.allocateInstance(PlayServicesConnectionsClientFacade::class.java)
        val adapterMethod =
            PlayServicesConnectionsClientFacade::class.java.declaredMethods.single { method ->
                method.name == "toPlayServicesCallback" &&
                    method.parameterTypes.contentEquals(arrayOf(NearbyPayloadCallback::class.java))
            }
        adapterMethod.isAccessible = true
        return adapterMethod.invoke(facade, callback) as PayloadCallback
    }
}
