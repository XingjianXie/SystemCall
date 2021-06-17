package com.mark07.SystemCall.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class SystemCallCommand {
    private static final SuggestionProvider<CommandSourceStack> transferFlowSuggestionProvider = new TransferFlowSuggestionProvider();

    public SystemCallCommand() {
    }

    public static boolean cost(CommandSourceStack commandSourceStack, int level) throws CommandSyntaxException {
        ServerPlayer player = commandSourceStack.getPlayerOrException();
        if (player.isCreative()) {
            return true;
        }
        if (player.experienceLevel >= level) {
            player.setExperienceLevels(player.experienceLevel - level);
            return true;
        }
        commandSourceStack.sendFailure(new TextComponent("No enough sacred level! Required: " + level));
        return false;
    }

    public static int litTargets(CommandSourceStack commandSourceStack, Collection<? extends Entity> collection) throws CommandSyntaxException {
        int j = 0;
        if (cost(commandSourceStack, 4 * collection.size())) {
            for (Entity entity : collection) {
                MobEffectInstance mobEffectInstance = new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false);
                try {
                    if (((LivingEntity)entity).addEffect(mobEffectInstance, commandSourceStack.getEntity())) {
                        ++j;
                    }
                } catch (Exception ignored) {

                }
            }
            return j;
        }
        return 0;
    }

    public static int generateItemElement(CommandSourceStack commandSourceStack, ItemInput itemInput) throws CommandSyntaxException {
        ServerPlayer player = commandSourceStack.getPlayerOrException();
        ItemStack itemStack = itemInput.createItemStack(1, false);
        Rarity r = itemStack.getRarity();
        int cost = 0;
        switch (r) {
            case COMMON -> cost = 80;
            case UNCOMMON -> cost = 120;
            case RARE -> cost = 180;
            case EPIC -> cost = 90000;
        }
        if (cost(commandSourceStack, cost)) {
            boolean bl = player.getInventory().add(itemStack);
            ItemEntity itemEntity;
            if (bl && itemStack.isEmpty()) {
                itemStack.setCount(1);
                itemEntity = player.drop(itemStack, false);
                if (itemEntity != null) {
                    itemEntity.makeFakeItem();
                }

                player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
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
        return 0;
    }

    public static Collection<Mob> getRight(ServerPlayer player) {
        BlockPos blockPos = player.blockPosition();
        Level level = player.level;
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        List<Mob> list = level.getEntitiesOfClass(Mob.class, new AABB((double)i - 7.0D, (double)j - 7.0D, (double)k - 7.0D, (double)i + 7.0D, (double)j + 7.0D, (double)k + 7.0D));
        return list.stream().filter(mob -> mob.getLeashHolder() == player).collect(Collectors.toList());
    }

    public static int transferHumanUnitDurability(CommandSourceStack commandSourceStack, String origin, String target) throws CommandSyntaxException {
        ServerPlayer player = commandSourceStack.getPlayerOrException();
        if ("right".equals(origin) && "self".equals(target)) {
            Collection<Mob> list = getRight(player);
            if (!cost(commandSourceStack, list.size() * 15)) {
                return 0;
            }
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
            if (!cost(commandSourceStack, list.size() * 15)) {
                return 0;
            }
            for (Mob mob : list) {
                float transfer = Math.min(4.0f, player.getHealth());
                player.hurt(DamageSource.OUT_OF_WORLD, transfer);
                player.invulnerableTime = 0;
                mob.heal(transfer);
            }
        } else if ("left".equals(origin) && "self".equals(target)) {
            ItemEntity itemEntity = player.drop(player.getItemBySlot(EquipmentSlot.HEAD), false);
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setOwner(player.getUUID());
            }
            player.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());
            player.getItemBySlot(EquipmentSlot.OFFHAND).setCount(0);
        }
        //commandSourceStack.sendSuccess(new TextComponent("transferHumanUnitDurability! " + origin + " to " + target + "!"), false);
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> inspectEntireCommandList = Commands.literal("inspect").then(Commands.literal("entire").then(Commands.literal("command").then(Commands.literal("list").executes(context -> {
            TextComponent info = new TextComponent("*Entire Command List*");
            commandDispatcher.getSmartUsage(commandDispatcher.getRoot().getChild("system").getChild("call"), context.getSource())
                    .forEach((commandSourceStackCommandNode, s) -> info.append("\n" + commandSourceStackCommandNode.getName() +  ": " + s));
            context.getSource().sendSuccess(info.withStyle(ChatFormatting.RED), false);
            return 1;
        }))));

        final LiteralArgumentBuilder<CommandSourceStack> litTargets = Commands.literal("lit").then(Commands.argument("targets", EntityArgument.entities()).executes(context ->
            litTargets(context.getSource(), EntityArgument.getEntities(context, "targets"))
        ));

        final LiteralArgumentBuilder<CommandSourceStack> generateItemElement = Commands.literal("generate").then(Commands.argument("item", ItemArgument.item()).then(Commands.literal("element").executes(context ->
            generateItemElement(context.getSource(), ItemArgument.getItem(context, "item"))
        )));

        final LiteralArgumentBuilder<CommandSourceStack> transferHumanUnitDurability = Commands.literal("transfer").then(Commands.literal("human").then(Commands.literal("unit").then(Commands.literal("durability")
                .then(Commands.argument("origin", StringArgumentType.word()).suggests(transferFlowSuggestionProvider).then(Commands.literal("to").then(Commands.argument("target", StringArgumentType.word()).suggests(transferFlowSuggestionProvider).executes(context ->
                        transferHumanUnitDurability(context.getSource(), StringArgumentType.getString(context, "origin"), StringArgumentType.getString(context, "target"))
                ))))
        )));

        final LiteralArgumentBuilder<CommandSourceStack> fly = Commands.literal("fly").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            if (!cost(context.getSource(), 80)) {
                return 0;
            }
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            return 1;
        });

        final LiteralArgumentBuilder<CommandSourceStack> changeFieldAttribution = Commands.literal("change").then(Commands.literal("field").then(Commands.literal("attribution").redirect(
                commandDispatcher.getRoot().getChild("gamerule")
        )));

        commandDispatcher.register(Commands.literal("system").then(Commands.literal("call").executes(context -> {
            context.getSource().sendFailure(new TextComponent("No arguments!").withStyle(ChatFormatting.RED));
            return 0;
        })
                .then(inspectEntireCommandList)
                .then(litTargets)
                .then(generateItemElement)
                .then(transferHumanUnitDurability)
                .then(fly)
                .then(changeFieldAttribution)
        ));
    }
}
