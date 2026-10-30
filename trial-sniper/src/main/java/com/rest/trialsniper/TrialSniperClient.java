package com.rest.trialsniper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TrialSniperClient {
    private static final int SEARCH_RADIUS = 5;
    private static final long RETRY_COOLDOWN_TICKS = 4L;
    private static final long NO_KEY_MESSAGE_COOLDOWN_TICKS = 40L;

    private static BlockPos lastAttemptPos;
    private static long lastAttemptTick = Long.MIN_VALUE / 4;
    private static long lastNoKeyMessageTick = Long.MIN_VALUE / 4;
    private static boolean acquisitionMessageShown;

    public static void onClientTick(Minecraft client) {
        if (!ready(client)) {
            acquisitionMessageShown = false;
            lastAttemptPos = null;
            return;
        }

        LocalPlayer player = client.player;
        if (hasHeavyCore(player)) {
            if (!acquisitionMessageShown) {
                acquisitionMessageShown = true;
                player.sendSystemMessage(Component.literal("§6§lTRIAL SNIPER §8» §a§lHEAVY CORE ACQUIRED!"));
            }
            return;
        }

        acquisitionMessageShown = false;
        scanInteractionRange(client);
    }

    public static void onBlockEntityUpdate(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (!ready(client) || hasHeavyCore(client.player)) {
            return;
        }
        trySnipe(client, pos);
    }

    private static boolean ready(Minecraft client) {
        return client != null
            && client.player != null
            && client.level != null
            && client.gameMode != null
            && client.player.isAlive();
    }

    private static void scanInteractionRange(Minecraft client) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    double distance = player.position().distanceToSqr(Vec3.atCenterOf(pos));
                    if (distance >= bestDistance) continue;

                    BlockState state = client.level.getBlockState(pos);
                    if (state.getBlock() != Blocks.VAULT || !state.getValue(VaultBlock.OMINOUS)) continue;
                    if (!player.isWithinBlockInteractionRange(pos, 0.0)) continue;

                    BlockEntity blockEntity = client.level.getBlockEntity(pos);
                    if (!(blockEntity instanceof VaultBlockEntity vault)) continue;

                    ItemStack displayed = vault.getSharedData().getDisplayItem();
                    if (!displayed.is(Items.HEAVY_CORE)) continue;

                    bestPos = pos.immutable();
                    bestDistance = distance;
                }
            }
        }

        if (bestPos != null) trySnipe(client, bestPos);
    }

    private static boolean trySnipe(Minecraft client, BlockPos pos) {
        LocalPlayer player = client.player;
        long now = client.level.getGameTime();

        if (lastAttemptPos != null && lastAttemptPos.equals(pos) && now - lastAttemptTick < RETRY_COOLDOWN_TICKS) {
            return false;
        }

        BlockState state = client.level.getBlockState(pos);
        if (state.getBlock() != Blocks.VAULT || !state.getValue(VaultBlock.OMINOUS)) return false;
        if (!player.isWithinBlockInteractionRange(pos, 0.0)) return false;

        BlockEntity blockEntity = client.level.getBlockEntity(pos);
        if (!(blockEntity instanceof VaultBlockEntity vault)) return false;

        ItemStack displayed = vault.getSharedData().getDisplayItem();
        if (!displayed.is(Items.HEAVY_CORE)) return false;

        InteractionHand hand;
        if (player.getOffhandItem().is(Items.OMINOUS_TRIAL_KEY)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            int keySlot = findHotbarSlot(player, Items.OMINOUS_TRIAL_KEY);
            if (keySlot < 0) {
                if (now - lastNoKeyMessageTick >= NO_KEY_MESSAGE_COOLDOWN_TICKS) {
                    lastNoKeyMessageTick = now;
                    player.sendSystemMessage(Component.literal("§6§lTRIAL SNIPER §8» §cPut an Ominous Trial Key in your hotbar/offhand."));
                }
                return false;
            }

            if (player.getInventory().getSelectedSlot() != keySlot) {
                player.getInventory().setSelectedSlot(keySlot);
            }
            hand = InteractionHand.MAIN_HAND;
        }

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        lastAttemptPos = pos.immutable();
        lastAttemptTick = now;

        client.gameMode.useItemOn(player, hand, hit);
        player.swing(hand);
        player.sendSystemMessage(Component.literal("§6§lTRIAL SNIPER §8» §eHeavy Core shown §8→ §aCLICKED"));
        return true;
    }

    private static int findHotbarSlot(LocalPlayer player, Item target) {
        int selected = player.getInventory().getSelectedSlot();
        if (player.getInventory().getSelectedItem().is(target)) return selected;
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(target)) return slot;
        }
        return -1;
    }

    private static boolean hasHeavyCore(LocalPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.HEAVY_CORE)) return true;
        }
        return player.getOffhandItem().is(Items.HEAVY_CORE);
    }

    private TrialSniperClient() {}
}
