package com.rest.trialsniper.mixin;

import com.rest.trialsniper.TrialSniperClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleBlockEntityData", at = @At("TAIL"))
    private void trialSniper$afterBlockEntityData(ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        TrialSniperClient.onBlockEntityUpdate(packet.getPos());
    }
}
