package dev.ghostspear;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GhostSpearPlugin extends JavaPlugin {

    private NamespacedKey spearKey;
    private NamespacedKey ghostKey;
    private NamespacedKey ghostModeKey;
    private Settings settings;
    private SpearItem spearItem;
    private GhostSpawner ghostSpawner;
    private GhostModeManager ghostModes;
    private PlayerState playerState;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        spearKey = new NamespacedKey(this, "ghost_spear");
        ghostKey = new NamespacedKey(this, "ghost_decoy");
        ghostModeKey = new NamespacedKey(this, "ghost_mode_active");

        reloadSettings();

        playerState = new PlayerState();
        spearItem = new SpearItem(this);
        ghostSpawner = new GhostSpawner(this);
        ghostModes = new GhostModeManager(this);

        getServer().getPluginManager().registerEvents(new SpearListener(this), this);
        new ChargeTask(this).runTaskTimer(this, 1L, 1L);

        GhostSpearCommand command = new GhostSpearCommand(this);
        PluginCommand pluginCommand = getCommand("ghostspear");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // In case of /reload: tidy up anyone left marked as a ghost
        getServer().getOnlinePlayers().forEach(ghostModes::handleJoin);

        getLogger().info("GhostSpear enabled. Spear material: " + settings.material
                + ", invisibility mode: " + (settings.hideMode ? "HIDE" : "POTION"));
    }

    @Override
    public void onDisable() {
        if (ghostModes != null) {
            ghostModes.deactivateAll(GhostModeManager.EndReason.SHUTDOWN);
        }
    }

    public void reloadSettings() {
        reloadConfig();
        settings = new Settings(getConfig(), getLogger());
    }

    public NamespacedKey spearKey() { return spearKey; }
    public NamespacedKey ghostKey() { return ghostKey; }
    public NamespacedKey ghostModeKey() { return ghostModeKey; }
    public Settings settings() { return settings; }
    public SpearItem spearItem() { return spearItem; }
    public GhostSpawner ghostSpawner() { return ghostSpawner; }
    public GhostModeManager ghostModes() { return ghostModes; }
    public PlayerState playerState() { return playerState; }
}
