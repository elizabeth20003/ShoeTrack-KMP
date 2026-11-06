package com.shoetrack.domain.usecase

import com.shoetrack.domain.sync.PairingCode
import com.shoetrack.domain.sync.SyncEngine

enum class PairingResult { Paired, Unreachable, InvalidCode }

/** Links this device with the one whose pairing code, as scanned or typed, is given. */
class PairWithDeviceUseCase(private val syncEngine: SyncEngine) {

    suspend operator fun invoke(codeText: String): PairingResult {
        val code = PairingCode.parse(codeText) ?: return PairingResult.InvalidCode
        return if (syncEngine.pairWith(code)) PairingResult.Paired else PairingResult.Unreachable
    }
}
