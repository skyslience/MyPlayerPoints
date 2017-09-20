package org.black_ixx.playerpoints;

import com.evilmidget38.UUIDFetcher;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import org.black_ixx.playerpoints.commands.Commander;
import org.black_ixx.playerpoints.config.LocalizeConfig;
import org.black_ixx.playerpoints.config.RootConfig;
import org.black_ixx.playerpoints.listeners.PlayerJoin;
import org.black_ixx.playerpoints.listeners.RestrictionListener;
import org.black_ixx.playerpoints.listeners.VotifierListener;
import org.black_ixx.playerpoints.services.IModule;
import org.black_ixx.playerpoints.storage.StorageHandler;
import org.black_ixx.playerpoints.storage.exports.Exporter;
import org.black_ixx.playerpoints.storage.imports.Importer;
import org.black_ixx.playerpoints.update.UpdateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;
import java.util.logging.Level;

/**
 * Main plugin class for PlayerPoints.
 */
public class PlayerPoints extends JavaPlugin implements PluginMessageListener {


    public static PlayerPoints instance;
    private static String serverName;
    @Getter
    private static ConsymenDataManager consuM;
    /**
     * Plugin tag.
     */
    public static final String TAG = "[PlayerPoints]";

    /**
     * API instance.
     */
    private PlayerPointsAPI api;

    private PlayerJoin playerJoin;
    /**
     * Modules.
     */
    private final Map<Class<? extends IModule>, IModule> modules = new HashMap<Class<? extends IModule>, IModule>();

    public static String getServerName() {
        return serverName;
    }

    public static void setServerName(String serverName) {
        PlayerPoints.serverName = serverName;
    }

    @Override
    public void onEnable() {

        instance = this;
        // Initialize localization
        LocalizeConfig.init(this);
        // Initialize config
        RootConfig rootConfig = new RootConfig(this);
        registerModule(RootConfig.class, rootConfig);
        // Do imports
        Importer importer = new Importer(this);
        importer.checkImport();
        // Do exports
        Exporter exporter = new Exporter(this);
        exporter.checkExport();
        // Intialize storage handler
        registerModule(StorageHandler.class, new StorageHandler(this));
        // Initialize API
        api = new PlayerPointsAPI(this);
        // Initialize updater
        UpdateManager update = new UpdateManager(this);
        update.checkUpdate();
        consuM = new ConsymenDataManager();
        // Register commands
        final Commander commander = new Commander(this);
        if(getDescription().getCommands().containsKey("points")) {
            getCommand("points").setExecutor(commander);
        }
        if(getDescription().getCommands().containsKey("p")) {
            getCommand("p").setExecutor(commander);
        }
        final PluginManager pm = getServer().getPluginManager();
        // Register votifier listener, if applicable
        if(rootConfig.voteEnabled) {
            final Plugin votifier = pm.getPlugin("Votifier");
            if(votifier != null) {
                pm.registerEvents(new VotifierListener(this), this);
            } else {
                getLogger().warning("Could not hook into Votifier!");
            }
        }
        // Vault module
        if(rootConfig.vault) {
            registerModule(PlayerPointsVaultLayer.class,
                    new PlayerPointsVaultLayer(this));
        }
        // Register listeners
        playerJoin = new PlayerJoin(this);
        pm.registerEvents(playerJoin , this);
        pm.registerEvents(new RestrictionListener(this), this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                PlayerPoints.this.getModuleForClass(StorageHandler.class).selectConsume();
            }
        }, 20 , 20*60*30);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {

            new PlayerPointPlaceholder(this).hook();
            System.out.print("注册成功");

        } else {
            throw new RuntimeException("Could not find PlaceholderAPI!! Plugin can not work without it!");
        }
    }

    @Override
    public void onDisable() {
        // Deregister all modules.
        List<Class<? extends IModule>> clazzez = new ArrayList<Class<? extends IModule>>();
        clazzez.addAll(modules.keySet());
        for(Class<? extends IModule> clazz : clazzez) {
            this.deregisterModuleForClass(clazz);
        }
    }

    /**
     * Get the plugin's API.
     * 
     * @return API instance.
     */
    public PlayerPointsAPI getAPI() {
        return api;
    }

    /**
     * Register a module to the API.
     * 
     * @param clazz
     *            - Class of the instance.
     * @param module
     *            - Module instance.
     * @throws IllegalArgumentException
     *             - Thrown if an argument is null.
     */
    public <T extends IModule> void registerModule(Class<T> clazz, T module) {
        // Check arguments.
        if(clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        } else if(module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        } else if(modules.containsKey(clazz)) {
            this.getLogger().warning(
                    "Overwriting module for class: " + clazz.getName());
        }
        // Add module.
        modules.put(clazz, module);
        // Tell module to start.
        module.starting();
    }

    /**
     * Unregister a module from the API.
     * 
     * @param clazz
     *            - Class of the instance.
     * @return Module that was removed from the API. Returns null if no instance
     *         of the module is registered with the API.
     */
    public <T extends IModule> T deregisterModuleForClass(Class<T> clazz) {
        // Check arguments.
        if(clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        // Grab module and tell it its closing.
        T module = clazz.cast(modules.get(clazz));
        if(module != null) {
            module.closing();
        }
        return module;
    }

    /**
     * Retrieve a registered CCModule.
     * 
     * @param clazz
     *            - Class identifier.
     * @return Module instance. Returns null is an instance of the given class
     *         has not been registered with the API.
     */
    public <T extends IModule> T getModuleForClass(Class<T> clazz) {
        return clazz.cast(modules.get(clazz));
    }

    /**
     * Attempts to look up full name based on who's on the server Given a
     * partial name
     * 
     * @author Frigid, edited by Raphfrk and petteyg359
     */
    public String expandName(String name) {
        int m = 0;
        String Result = "";
        final Collection<? extends Player> online = getServer().getOnlinePlayers();
        for(Player player : online) {
            String str = player.getName();
            if(str.matches("(?i).*" + name + ".*")) {
                m++;
                Result = str;
                if(m == 2) {
                    return null;
                }
            }
            if(str.equalsIgnoreCase(name)) {
                return str;
            }
        }
        if(m == 1)
            return Result;
        if(m > 1) {
            return null;
        }
        return name;
    }
    
    /**
     * Attempt to translate a player name into a UUID.
     * @param name - Player name.
     * @return Player UUID. Null if no match found.
     */
    public UUID translateNameToUUID(String name) {
        UUID id = null;
        RootConfig config = getModuleForClass(RootConfig.class);
        if(config.debugUUID) {
        	getLogger().info("translateNameToUUID(" + name + ")");
        }

        if(name == null) {
        	if(config.debugUUID) {
            	getLogger().info("translateNameToUUID() - bad ID");
            }
        	return id;
        }

        // Look through online players first
        if(config.debugUUID) {
        	getLogger().info("translateNameToUUID() - Looking through online players: " + Bukkit.getServer().getOnlinePlayers().size());
        }
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        for(Player p : players) {
            if(p.getName().equalsIgnoreCase(name)) {
                id = p.getUniqueId();
                if(config.debugUUID) {
                	getLogger().info("translateNameToUUID() online player UUID found: " + id.toString());
                }
                break;
            }
        }

        // Last resort, attempt bukkit api lookup
        if(id == null && Bukkit.getServer().getOnlineMode()) {
        	if(config.debugUUID) {
            	getLogger().info("translateNameToUUID() - Attempting online lookup");
            }
        	UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(name));
        	try {
				Map<String, UUID> map = fetcher.call();
				for(Map.Entry<String, UUID> entry : map.entrySet()) {
					if(name.equalsIgnoreCase(entry.getKey())) {
						id = entry.getValue();
						if(config.debugUUID) {
							getLogger().info("translateNameToUUID() web player UUID found: " + ((id == null) ? id : id.toString()));
						}
						break;
					}
				}
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Exception on online UUID fetch", e);
			}
        } else if(id == null && !Bukkit.getServer().getOnlineMode()) {
            //There's nothing we can do but attempt to get the UUID from old method.
            id = Bukkit.getServer().getOfflinePlayer(name).getUniqueId();
            if(config.debugUUID) {
                getLogger().info("translateNameToUUID() offline player UUID found: " + ((id == null) ? id : id.toString()));
            }
        }
        return id;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("GetServer")) {
            setServerName(in.readUTF());
            PlayerJoinEvent.getHandlerList().unregister(playerJoin);
        }
    }


}
