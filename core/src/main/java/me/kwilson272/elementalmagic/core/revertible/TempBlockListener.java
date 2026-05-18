package me.kwilson272.elementalmagic.core.revertible;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;

public class TempBlockListener implements Listener {

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        TempBlock.get(event.getBlock()).ifPresent(tb -> {
            ElementalMagicApi.revertibleManager().revert(tb);
            event.setCancelled(true);
        });
    }

    @EventHandler
    private void onBlockBurn(BlockBurnEvent e) {
        if (TempBlock.isTempBlock(e.getBlock()) || (e.getIgnitingBlock() != null
                && TempBlock.isTempBlock(e.getIgnitingBlock()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockDropItem(BlockDropItemEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // Monitor: only revert blocks that actually explode (uncancelled)
    @EventHandler(priority = EventPriority.MONITOR)
    private void onBlockExplode(BlockExplodeEvent event) {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        for (Block block : event.blockList()) {
            TempBlock.get(block).ifPresent(revertManager::revert);
        }
    }

    @EventHandler
    private void onBlockFade(BlockFadeEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    //@EventHandler
    private void onBlockFertilizeEvent(BlockFertilizeEvent event) {
        // TODO: not really sure what the correct thing is to do here,
        // if we revert we are going to have to manage setting the block state
        // to the correct state.
    }

    @EventHandler
    private void onBlockFromTo(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();
        if (TempBlock.isTempBlock(from) || TempBlock.isTouchingAny(from)
                || TempBlock.isTempBlock(to)) {
            // Not sure this is a perfect implementation
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockGrow(BlockGrowEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        if (event.getNewState().isPlaced()) {
            Block growInto = event.getNewState().getBlock();
            if (TempBlock.isTempBlock(growInto)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onBlockForm(BlockFormEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockSpreadEvent(BlockSpreadEvent event) {
        if (TempBlock.isTempBlock(event.getBlock()) ||
                TempBlock.isTempBlock(event.getSource())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockIgnite(BlockIgniteEvent event) {
        Block ignited = event.getBlock();
        Block igniter = event.getIgnitingBlock();
        if (TempBlock.isTempBlock(ignited) ||
                (igniter != null) && TempBlock.isTempBlock(igniter)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockPhysics(BlockPhysicsEvent event) {
        /* PER DOCUMENTATION:
           "Plugins should also note that where possible this event may only
           called for the "root" block of physics updates in order to limit
           event spam. Physics updates that cause other blocks to change their
            state may not result in an event for each of those blocks
            (usually adjacent). If you are concerned about monitoring these
            changes then you should check adjacent blocks yourself."

            If this proves to be an issue, we will likely need to follow advice.
         */
        if (TempBlock.isTempBlock(event.getBlock()) ||
                TempBlock.isTempBlock(event.getSourceBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        // TODO: this seems pretty broad and could likely be refined to
        // feel better while playing
        if (TempBlock.isTempBlock(event.getBlockPlaced())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onFluidLevelChange(FluidLevelChangeEvent event) {
        // This is kind of a poor condition, but it is hard to be specific
        // because the event doesn't give us the Block that caused the change
        if (TempBlock.isTempBlock(event.getBlock()) ||
                TempBlock.isTouchingAny(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onLeavesDecay(LeavesDecayEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onSpongeAbsorb(SpongeAbsorbEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
        }

        // TODO: See if we can prevent absorption of tempblocks via the getBlocks
    }

    @EventHandler
    private void onPlayerHarvestBlockEvent(PlayerHarvestBlockEvent event) {
        if (TempBlock.isTempBlock(event.getHarvestedBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (TempBlock.isTempBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }
}

