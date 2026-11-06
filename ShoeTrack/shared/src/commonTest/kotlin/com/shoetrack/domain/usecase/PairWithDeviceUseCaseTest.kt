package com.shoetrack.domain.usecase

import com.shoetrack.domain.fakes.FakeSyncEngine
import com.shoetrack.domain.sync.PairingCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PairWithDeviceUseCaseTest {

    private val engine = FakeSyncEngine()
    private val pairWithDevice = PairWithDeviceUseCase(engine)

    @Test
    fun pairsWithTheDeviceBehindAValidCode() = runTest {
        val result = pairWithDevice("shoetrack:1:192.168.1.30:45872")

        assertEquals(PairingResult.Paired, result)
        assertEquals(listOf(PairingCode("192.168.1.30", 45872)), engine.pairedWith)
    }

    @Test
    fun reportsAnUnreachableDevice() = runTest {
        engine.pairingSucceeds = false

        assertEquals(PairingResult.Unreachable, pairWithDevice("shoetrack:1:192.168.1.30:45872"))
    }

    @Test
    fun doesNotTouchTheEngineForATextThatIsNotACode() = runTest {
        assertEquals(PairingResult.InvalidCode, pairWithDevice("not a code"))
        assertEquals(emptyList(), engine.pairedWith)
    }
}
