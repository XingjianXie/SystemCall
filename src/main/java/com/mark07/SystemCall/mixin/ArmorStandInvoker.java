package com.mark07.SystemCall.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandInvoker {
    @Invoker void invokeSetSmall(boolean b);
    @Invoker void invokeSetShowArms(boolean b);
    @Invoker void invokeSetNoBasePlate(boolean b);
    @Invoker void invokeSetMarker(boolean b);
}
