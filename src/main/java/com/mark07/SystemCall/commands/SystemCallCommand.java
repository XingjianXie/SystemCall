package com.mark07.SystemCall.commands;

import com.mark07.SystemCall.SystemCall;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SystemCallCommand {
    private static final SuggestionProvider<CommandSourceStack> transferFlowSuggestionProvider =
            new TransferFlowSuggestionProvider();

    public SystemCallCommand() {
    }

    public static void performAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component title = new TextComponent("System Call Triggered");
        Component subtitle = new TextComponent(context.getInput());
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static boolean checkoutLevel(CommandContext<CommandSourceStack> context, int level)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (player.isCreative()) {
            performAnimation(context);
            return false;
        }
        if (player.experienceLevel >= level) {
            player.setExperienceLevels(player.experienceLevel - level);
            performAnimation(context);
            return false;
        }
        context.getSource().sendFailure(new TextComponent("No enough sacred level! Required: " + level));
        return true;
    }

    public static boolean requiredLevel(CommandSourceStack commandSourceStack, int level) {
        try {
            ServerPlayer player = commandSourceStack.getPlayerOrException();
            return player.isCreative() || player.experienceLevel >= level;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    /**
     * Cost 4 * targets count
     */
    public static int litTargets(CommandContext<CommandSourceStack> context, Collection<? extends Entity> collection)
            throws CommandSyntaxException {
        int j = 0;
        if (checkoutLevel(context, 4 * collection.size())) return 0;
        for (Entity entity : collection) {
            MobEffectInstance mobEffectInstance =
                    new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false);
            try {
                if (((LivingEntity)entity).addEffect(mobEffectInstance, context.getSource().getEntity())) {
                    ++j;
                }
            } catch (Exception ignored) {

            }
        }
        return j;

    }

    /**
     * Require permission 2
     */
    public static int generateItemElement(CommandContext<CommandSourceStack> context, ItemInput itemInput)
            throws CommandSyntaxException {
        checkoutLevel(context, 0);
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemStack = itemInput.createItemStack(1, false);
        boolean bl = player.getInventory().add(itemStack);
        ItemEntity itemEntity;
        if (bl && itemStack.isEmpty()) {
            itemStack.setCount(1);
            itemEntity = player.drop(itemStack, false);
            if (itemEntity != null) {
                itemEntity.makeFakeItem();
            }

            player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.containerMenu.broadcastChanges();
        } else {
            itemEntity = player.drop(itemStack, false);
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setOwner(player.getUUID());
            }
        }
        return 1;
    }

    public static Collection<Mob> getRight(ServerPlayer player) {
        BlockPos blockPos = player.blockPosition();
        Level level = player.level;
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        List<Mob> list = level.getEntitiesOfClass(Mob.class,
                new AABB((double)i - 7.0D, (double)j - 7.0D, (double)k - 7.0D,
                        (double)i + 7.0D, (double)j + 7.0D, (double)k + 7.0D));
        return list.stream().filter(mob -> mob.getLeashHolder() == player).collect(Collectors.toList());
    }

    /**
     * Cost 15 * targets count
     */
    public static int transferHumanUnitDurability(CommandContext<CommandSourceStack> context,
                                                  String origin, String target)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if ("right".equals(origin) && "self".equals(target)) {
            Collection<Mob> list = getRight(player);
            if (checkoutLevel(context, list.size() * 15)) return 0;
            for (Mob mob : list) {
                float transfer = Math.min(4.0f, mob.getHealth());
                mob.hurt(DamageSource.OUT_OF_WORLD, transfer);
                if (player.getHealth() == player.getMaxHealth()) {
                    player.getFoodData().eat(((int) transfer * 2), transfer * 3);
                }
                player.heal(transfer);
            }
        } else if ("self".equals(origin) && "right".equals(target)) {
            Collection<Mob> list = getRight(player);
            if (checkoutLevel(context, list.size() * 15)) return 0;
            for (Mob mob : list) {
                float transfer = Math.min(4.0f, player.getHealth());
                player.hurt(DamageSource.OUT_OF_WORLD, transfer);
                player.invulnerableTime = 0;
                mob.heal(transfer);
            }
        } else if ("left".equals(origin) && "self".equals(target)) {
            if (checkoutLevel(context, 15)) return 0;
            ItemEntity itemEntity = player.drop(player.getItemBySlot(EquipmentSlot.HEAD), false);
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setOwner(player.getUUID());
            }
            player.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());
            player.getItemBySlot(EquipmentSlot.OFFHAND).setCount(0);
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack>
    literals(String literals, Function<LiteralArgumentBuilder<CommandSourceStack>, ?> function) {
        String[] literalsList = literals.split(" ");
        if (literalsList.length == 0) {
            throw new IllegalArgumentException();
        }
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(literalsList[literalsList.length - 1]);
        function.apply(literal);
        for (int i = literalsList.length - 2; i >= 0; i--) {
            literal = Commands.literal(literalsList[i]).then(literal);
        }
        return literal;
    }

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {

        final LiteralArgumentBuilder<CommandSourceStack> inspectEntireCommandList =
                literals("inspect entire command list", literal -> literal.executes(context -> {
                    checkoutLevel(context, 0);
                    MutableComponent info = new TextComponent("");
                    info.append(new TextComponent("Entire Command List")
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle(ChatFormatting.BOLD));
                    for (String s : commandDispatcher.getAllUsage(
                            commandDispatcher.getRoot().getChild("system").getChild("call"),
                            context.getSource(),
                            false
                    )) {
                        info.append(
                                new TextComponent("\n" + s)
                                        .withStyle(ChatFormatting.BLUE)
                        );
                    }
                    context.getSource().sendSuccess(info, false);
                    return 1;
                })).requires(commandSourceStack -> requiredLevel(commandSourceStack, 80));

        final LiteralArgumentBuilder<CommandSourceStack> litTargets = Commands.literal("lit")
                .then(Commands.argument("targets", EntityArgument.entities()).executes(
                        context -> litTargets(context, EntityArgument.getEntities(context, "targets"))
                )).requires(commandSourceStack -> requiredLevel(commandSourceStack, 25));

        final LiteralArgumentBuilder<CommandSourceStack> generateItemElement = Commands.literal("generate")
                .then(Commands.argument("item", ItemArgument.item())
                        .then(Commands.literal("element").executes(context ->
                                generateItemElement(context, ItemArgument.getItem(context, "item"))
                        ))
                ).requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        final LiteralArgumentBuilder<CommandSourceStack> transferHumanUnitDurability =
                literals("transfer human unit durability", literal -> literal.then(
                        Commands.argument("origin", StringArgumentType.word())
                                .suggests(transferFlowSuggestionProvider)
                                .then(Commands.literal("to")
                                        .then(Commands.argument("target", StringArgumentType.word())
                                                .suggests(transferFlowSuggestionProvider)
                                                .executes(context ->
                                                        transferHumanUnitDurability(
                                                                context,
                                                                StringArgumentType.getString(context, "origin"),
                                                                StringArgumentType.getString(context, "target")
                                                        )
                                                )
                                        )
                                )
                )).requires(commandSourceStack -> requiredLevel(commandSourceStack, 30));

        final LiteralArgumentBuilder<CommandSourceStack> generateAerialElementStreamShape =
                literals("generate aerial element stream shape", literal -> literal.executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (checkoutLevel(context, 15)) {
                        return 0;
                    }

                    Vec3 vec3 = player.getDeltaMovement().add(
                            -1.4 * Mth.sin(player.getYRot() * 0.017453292F),
                            2,
                            1.4 * Mth.cos(player.getYRot() * 0.017453292F)
                    );
                    player.setDeltaMovement(vec3);
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    player.addEffect(new MobEffectInstance(SystemCall.AERIAL_STREAM, 120 * 20));
                    return 1;
                }).requires(commandSourceStack -> requiredLevel(commandSourceStack, 30)));

        final LiteralArgumentBuilder<CommandSourceStack> generateAerialElementBurstElement =
                literals("generate aerial element burst element", literal -> literal.executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (checkoutLevel(context, 10)) {
                        return 0;
                    }

                    BlockPos blockPos = player.blockPosition();
                    Level level = player.level;
                    int i = blockPos.getX();
                    int j = blockPos.getY();
                    int k = blockPos.getZ();
                    List<Entity> list = level.getEntitiesOfClass(Entity.class,
                            new AABB((double)i - 10.0D, (double)j - 10.0D, (double)k - 10.0D,
                                    (double)i + 10.0D, (double)j + 10.0D, (double)k + 10.0D));

                    for (Entity entity : list) {
                        Vec3 vec3 = entity.getDeltaMovement().add(
                                -3 * Mth.sin(player.getYRot() * 0.017453292F),
                                3,
                                3 * Mth.cos(player.getYRot() * 0.017453292F)
                        );
                        entity.setDeltaMovement(vec3);

                        if (entity instanceof ServerPlayer) {
                            ((ServerPlayer) entity).connection.send(new ClientboundSetEntityMotionPacket(entity));
                        }
                    }
                    MobEffectInstance effectInstance = player.getEffect(MobEffects.SLOW_FALLING);
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                            20 * 9 + (effectInstance != null ? effectInstance.getDuration() : 0), 0,
                            false, false, false));
                    return 1;
                }).requires(commandSourceStack -> requiredLevel(commandSourceStack, 30)));

        final LiteralArgumentBuilder<CommandSourceStack> changeFieldAttribution =
                literals("change field attribution", literal -> literal.redirect(
                        commandDispatcher.getRoot().getChild("gamerule"),
                        context -> {
                            checkoutLevel(context, 0);
                            return context.getSource();
                        }
                )).requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        final LiteralArgumentBuilder<CommandSourceStack> enhanceArmamentReleaseRecollection =
                literals("enhance armament release recollection", literal -> literal.executes(context -> {
                    if (checkoutLevel(context, 25)) return 0;
                    LightningBolt lightningBolt =
                            new LightningBolt(EntityType.LIGHTNING_BOLT, context.getSource().getLevel());
                    lightningBolt.setVisualOnly(true);
                    lightningBolt.setCause(context.getSource().getPlayerOrException());
                    lightningBolt.moveTo(context.getSource().getPosition());
                    context.getSource().getLevel().addFreshEntity(lightningBolt);


                    return 1;
                })).requires(commandSourceStack -> requiredLevel(commandSourceStack, 100));

        commandDispatcher.register(literals("system call", literal -> literal.executes(context -> {
            checkoutLevel(context, 0);
            context.getSource().sendFailure(new TextComponent("No arguments!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        })
                .then(generateAerialElementStreamShape) // Require 30, Cost 15
                .then(generateAerialElementBurstElement) // Require 30, Cost 10
                .then(inspectEntireCommandList) // Require 80, Cost 0
                .then(litTargets) // Require 25, Cost 4 * targets count
                .then(generateItemElement) // Require permission 2, Cost 0
                .then(transferHumanUnitDurability) // Require 30, Cost 15 * targets count
                .then(changeFieldAttribution) // Require permission 2, Cost 0
                .then(enhanceArmamentReleaseRecollection) // Require 50, Cost 25

        ).requires(commandSourceStack -> requiredLevel(commandSourceStack, 15)));
    }
}
