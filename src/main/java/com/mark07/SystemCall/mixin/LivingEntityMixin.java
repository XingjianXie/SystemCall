package com.mark07.SystemCall.mixin;

import com.mark07.SystemCall.SystemCall;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow public abstract boolean hasEffect(MobEffect mobEffect);

    @Inject(at = @At("RETURN"), method = "updateFallFlying()V")
    private void updateFallFlying(CallbackInfo ci) {
        if (this.hasEffect(SystemCall.AERIAL_STREAM) && !this.onGround && !this.level.isClientSide) {
            ((EntityInvoker)this).invokeSetSharedFlag(7, true);
        }
    }
}
