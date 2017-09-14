package org.black_ixx.playerpoints.storage;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.config.RootConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Represents a database type of storage.
 */
public abstract class DatabaseStorage implements IStorage {

    /**
     * Plugin instance.
     */
    protected PlayerPoints plugin;
    /**
     * Query for getting points.
     */
    protected String GET_POINTS = "SELECT points FROM %s WHERE playername=?;";
    /**
     * Query for getting player names.
     */
    protected String GET_PLAYERS = "SELECT playername FROM %s;";
    /**
     * Query for adding a new player.
     */
    protected String INSERT_PLAYER = "INSERT INTO %s (points,playername) VALUES(?,?);";
    /**
     * Query for updating a player's point amount.
     */
    protected String UPDATE_PLAYER = "UPDATE %s SET points=? WHERE playername=?";
    /**
     *
     */
    protected String UPDATE_CONSUME = "INSERT INTO %s (playername,points,info,time,server) VALUES(?,?,?,?,?)";
    /**
     * Query for removing a player.
     */
    protected String REMOVE_PLAYER = "DELETE %s WHERE playername=?";

    /**
     * Constructor.
     * 
     * @param plugin
     *            - PlayerPoints instance.
     */
    public DatabaseStorage(PlayerPoints plugin) {
        this.plugin = plugin;
    }
    
    protected void SetupQueries(String tableName) {
    	GET_POINTS = String.format(GET_POINTS, tableName);
    	GET_PLAYERS = String.format(GET_PLAYERS, tableName);
    	INSERT_PLAYER = String.format(INSERT_PLAYER, tableName);
    	UPDATE_PLAYER = String.format(UPDATE_PLAYER, tableName);
    	REMOVE_PLAYER = String.format(REMOVE_PLAYER, tableName);
    }

    protected void SetupConsume(String consumeTableName){
        UPDATE_CONSUME = String.format(UPDATE_CONSUME , consumeTableName);
    }

    /**
     * Cleanup the given resources.
     * 
     * @param result
     *            - ResultSet to close.
     * @param statement
     *            - Statement to close.
     */
    protected void cleanup(ResultSet result, PreparedStatement statement) {
    	RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(config.debugDatabase) {
        	plugin.getLogger().info("cleanup()");
        }
        if(result != null) {
            try {
                result.close();
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "SQLException on cleanup",
                        e);
            }
        }
        if(statement != null) {
            try {
                statement.close();
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "SQLException on cleanup",
                        e);
            }
        }
    }

}
