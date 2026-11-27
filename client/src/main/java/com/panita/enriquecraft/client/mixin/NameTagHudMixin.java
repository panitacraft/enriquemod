package com.panita.enriquecraft.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the name above a head while the HUD is hidden (F1). The game does this only for an entity that is in no team: for one in a
 * team it follows the visibility the team asks for, so the name tags the server draws with a team of their own would stay on screen.
 */
@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public abstract class NameTagHudMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$hideWithTheHud(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> callback) {
        if (Minecraft.getInstance().gui.hud.isHidden()) {
            callback.setReturnValue(false);
        }
    }
}
