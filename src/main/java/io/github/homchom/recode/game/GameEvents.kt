package io.github.homchom.recode.game

import io.github.homchom.recode.event.CustomHook
import io.github.homchom.recode.event.ValidatedHook
import io.github.homchom.recode.event.createHook
import net.minecraft.network.protocol.game.ClientboundSoundPacket

object PlaySoundEvent :
    CustomHook<ClientboundSoundPacket, Boolean> by createHook(),
    ValidatedHook<ClientboundSoundPacket>