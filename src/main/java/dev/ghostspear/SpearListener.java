package dev.ghostspear;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class SpearListener implements Listener {

    private final GhostSpearPlugin plugin;

    public SpearListener(GhostSpearPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------ swing = toggle ghost mode

    /** Right-clicking a block (door, chest...) also swings the arm - remember it so that doesn't toggle. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.playerState().markBlockUse(event.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.spearItem().is(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (!player.hasPermission("ghostspear.use") || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.isHandRaised()) {
            return; // charging, not swinging
        }

        Settings s = plugin.settings();
        PlayerState state = plugin.playerState();
        GhostModeManager ghosts = plugin.ghostModes();
        UUID id = player.getUniqueId();
        int now = Bukkit.getCurrentTick();

        // A swing that was really an attack or a door click shouldn't toggle anything
        if (state.attackedRecently(id, now) || state.usedBlockRecently(id, now)) {
            return;
        }

        int left = state.toggleCooldownLeft(id, now, s.toggleCooldownTicks);
        if (left > 0) {
            String seconds = String.format("%.1f", left / 20.0);
            player.sendActionBar(MiniMessage.miniMessage().deserialize(
                    s.msgCooldown, Placeholder.unparsed("seconds", seconds)));
            return;
        }

        if (ghosts.isGhosted(id)) {
            ghosts.deactivate(player, GhostModeManager.EndReason.TOGGLE);
        } else {
            ghosts.activate(player);
        }
    }

    // ------------------------------------------------ attacking reveals you

    /** Remember every attack attempt (even blocked ones) so the swing that comes with it is ignored. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttackAttempt(EntityDamageByEntityEvent event) {
        Player attacker = attackerOf(event.getDamager());
        if (attacker != null) {
            plugin.playerState().markAttack(attacker.getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!plugin.settings().endOnAttack) {
            return;
        }
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null || !plugin.ghostModes().isGhosted(attacker.getUniqueId())) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity) || plugin.ghostSpawner().isGhost(event.getEntity())) {
            return;
        }
        plugin.ghostModes().deactivate(attacker, GhostModeManager.EndReason.ATTACK);
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    // ------------------------------------------------ the decoy never really takes damage

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDecoyDamaged(EntityDamageEvent event) {
        Entity decoy = event.getEntity();
        if (!plugin.ghostSpawner().isGhost(decoy)) {
            return;
        }
        event.setCancelled(true);

        if (!(event instanceof EntityDamageByEntityEvent byEntity) || !(decoy instanceof LivingEntity living)) {
            return;
        }
        Settings s = plugin.settings();

        // Act like a real player getting hit
        if (s.decoyReactToHits) {
            Location from = byEntity.getDamager().getLocation();
            Location to = living.getLocation();
            living.playHurtAnimation(0f);
            living.getWorld().playSound(to, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            double dx = from.getX() - to.getX();
            double dz = from.getZ() - to.getZ();
            if (dx * dx + dz * dz > 1.0E-4) {
                living.knockback(0.4, dx, dz);
            }
        }

        if (s.decoyRevealWhenHit) {
            UUID ownerId = plugin.ghostModes().ownerOfDecoy(decoy);
            Player owner = ownerId == null ? null : Bukkit.getPlayer(ownerId);
            Player attacker = attackerOf(byEntity.getDamager());
            if (owner != null && !owner.equals(attacker)) {
                plugin.ghostModes().deactivate(owner, GhostModeManager.EndReason.DECOY_HIT);
            }
        }
    }

    // ------------------------------------------------ join / quit / death

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.ghostModes().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.ghostModes().deactivate(player, GhostModeManager.EndReason.QUIT);
        plugin.playerState().clear(player.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.ghostModes().deactivate(event.getEntity(), GhostModeManager.EndReason.DEATH);
    }
}
