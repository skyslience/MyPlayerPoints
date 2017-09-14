package org.black_ixx.playerpoints.storage.models;

import lib.PatPeter.SQLibrary.MySQL;
import org.black_ixx.playerpoints.ConsumeData;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.config.RootConfig;
import org.black_ixx.playerpoints.storage.DatabaseStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Storage handler for MySQL source.
 * 
 * @author Mitsugaru
 */
public class MySQLStorage extends DatabaseStorage {

    /**
     * MYSQL reference.
     */
    private MySQL mysql;
    /**
     * Number of attempts to reconnect before completely failing an operation.
     */
    private int retryLimit = 10;
    /**
     * The table name to use.
     */
    private String tableName;

    /**
     *
     */
    private String consumeTable;
    /**
     * Current retry count.
     */
    private int retryCount = 0;
    /**
     * Skip operation flag.
     */
    private boolean skip = false;

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public MySQLStorage(PlayerPoints plugin) {
        super(plugin);
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(config.debugDatabase) {
        	plugin.getLogger().info("Constructor");
        }
        retryLimit = config.retryLimit;
        //setup table name and strings
        tableName = config.table;
        consumeTable = config.consumetable;
        SetupQueries(tableName);
        SetupConsume(consumeTable);
        //Connect
        connect();
        if(!mysql.isTable(tableName) || !mysql.isTable(consumeTable)) {
            build();
        }
    }

    @Override
    public int getPoints(String id) {
        int points = 0;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(id == null || id.equals("")) {
            if(config.debugDatabase) {
            	plugin.getLogger().info("getPoints() - bad ID");
            }
            return points;
        }
        if(config.debugDatabase) {
        	plugin.getLogger().info("getPoints(" + id + ")");
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            statement = mysql.prepare(GET_POINTS);
            statement.setString(1, id);
            result = mysql.query(statement);
            if(result != null && result.next()) {
                points = result.getInt("points");
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create getter statement.", e);
            retryCount++;
            connect();
            if(!skip) {
                points = getPoints(id);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if(config.debugDatabase) {
        	plugin.getLogger().info("getPlayers() result - " + points);
        }
        return points;
    }

    @Override
    public boolean setPoints(String id, int points) {
        boolean value = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(id == null || id.equals("")) {
            if(config.debugDatabase) {
            	plugin.getLogger().info("setPoints() - bad ID");
            }
            return value;
        }
        if(config.debugDatabase) {
        	plugin.getLogger().info("setPoints(" + id + "," + points + ")");
        }
        final boolean exists = playerEntryExists(id);
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            if(exists) {
                statement = mysql.prepare(UPDATE_PLAYER);
            } else {
                statement = mysql.prepare(INSERT_PLAYER);
            }
            statement.setInt(1, points);
            statement.setString(2, id);
            result = mysql.query(statement);
            value = true;
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create setter statement.", e);
            retryCount++;
            connect();
            if(!skip) {
                value = setPoints(id, points);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if(config.debugDatabase) {
        	plugin.getLogger().info("setPoints() result - " + value);
        }
        return value;
    }

    @Override
    public boolean consumePoints(String id, int points, String info) {
        boolean value = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(id == null || id.equals("")) {
            if(config.debugDatabase) {
                plugin.getLogger().info("consumePoint() - bad ID");
            }
            return value;
        }
        if(config.debugDatabase) {
            plugin.getLogger().info("consume(" + id + "," + points + "," + info +")");
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = df.format(new Date());
            statement = mysql.prepare(UPDATE_CONSUME);
            statement.setString(1, id);
            statement.setInt(2, points);
            statement.setString(3, info);
            statement.setString(4, date);
            if(PlayerPoints.getServerName() == null || PlayerPoints.getServerName().equals("")) {
                statement.setString(5, "unknow");
            }else {
                statement.setString(5, PlayerPoints.getServerName());
            }
            result = mysql.query(statement);
            value = true;
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create setter statement.", e);
            retryCount++;
            connect();
            if(!skip) {
                value = setPoints(id, points);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if(config.debugDatabase) {
            plugin.getLogger().info("consumePoint() result - " + value);
        }
        return value;
    }

    @Override
    public boolean playerEntryExists(String id) {
        boolean has = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(id == null || id.equals("")) {
            if(config.debugDatabase) {
            	plugin.getLogger().info("playerEntryExists() - bad ID");
            }
            return has;
        }
        if(config.debugDatabase) {
        	plugin.getLogger().info("playerEntryExists("+ id + ")");
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            statement = mysql.prepare(GET_POINTS);
            statement.setString(1, id);
            result = mysql.query(statement);
            if(result.next()) {
                has = true;
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create player check statement.", e);
            retryCount++;
            connect();
            if(!skip) {
                has = playerEntryExists(id);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if(config.debugDatabase) {
        	plugin.getLogger().info("playerEntryExists() result - " + has);
        }
        return has;
    }
    
    @Override
    public boolean removePlayer(String id) {
        boolean deleted = false;
        if(id == null || id.equals("")) {
            return deleted;
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(config.debugDatabase) {
        	plugin.getLogger().info("removePlayers(" + id + ")");
        }
        try {
            statement = mysql.prepare(REMOVE_PLAYER);
            statement.setString(1, id);
            result = mysql.query(statement);
            deleted = true;
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create player remove statement.", e);
            retryCount++;
            connect();
            if(!skip) {
                deleted = playerEntryExists(id);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if(config.debugDatabase) {
        	plugin.getLogger().info("renovePlayers() result - " + deleted);
        }
        return deleted;
    }

    @Override
    public Collection<String> getPlayers() {
        Collection<String> players = new HashSet<String>();

        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(config.debugDatabase) {
        	plugin.getLogger().info("Attempting getPlayers()");
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            statement = mysql.prepare(GET_PLAYERS);
            result = mysql.query(statement);

            while(result.next()) {
                String name = result.getString("playername");
                if(name != null) {
                    players.add(name);
                }
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create get players statement.", e);
            retryCount++;
            connect();
            if(!skip) {
                players.clear();
                players.addAll(getPlayers());
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if(config.debugDatabase) {
        	plugin.getLogger().info("getPlayers() result - " + players.size());
        }
        return players;
    }

    /**
     * Connect to MySQL database. Close existing connection if one exists.
     */
    private void connect() {
    	RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(mysql != null) {
        	if(config.debugDatabase) {
        		plugin.getLogger().info("Closing existing MySQL connection");
        	}
            mysql.close();
        }
        mysql = new MySQL(plugin.getLogger(), " ", config.host,
                Integer.valueOf(config.port), config.database, config.user,
                config.password);
        if(config.debugDatabase) {
    		plugin.getLogger().info("Attempting MySQL connection to " + config.user + "@" + config.host + ":" + config.port + "/" + config.database);
    	}
        if(retryCount < retryLimit) {
            mysql.open();
        } else {
            plugin.getLogger().severe(
                    "Tried connecting to MySQL " + retryLimit
                            + " times and could not connect.");
            plugin.getLogger()
                    .severe("It may be in your best interest to restart the plugin / server.");
            retryCount = 0;
            skip = true;
        }
    }

    @Override
    public boolean destroy() {
        boolean success = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(config.debugDatabase) {
        	plugin.getLogger().info("Dropping playerpoints table");
        }
        try {
            mysql.query(String.format("DROP TABLE %s;", tableName));
            success = true;
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not drop MySQL table.", e);
        }
        return success;
    }

    @Override
    public boolean build() {
        boolean success = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if(config.debugDatabase) {
        	plugin.getLogger().info(String.format("Creating %s table", tableName));
        }
        try {
            mysql.query(String.format("CREATE TABLE %s (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(36) NOT NULL, points INT NOT NULL, info varchar(50), time varchar(50), server varchar(50), PRIMARY KEY(id));" , consumeTable));
            mysql.query(String.format("CREATE TABLE %s (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(36) NOT NULL, points INT NOT NULL, PRIMARY KEY(id), UNIQUE(playername));", tableName));
            success = true;
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create MySQL table.", e);
        }
        return success;
    }

    @Override
    public void selectConsume() {
        try {
            ResultSet resultSet = mysql.query(SELECT_CONSUME);
            while (resultSet.next()){
                int id = resultSet.getInt("id");
                String name = resultSet.getString("playername");
                int point = resultSet.getInt("points");
                String info = resultSet.getString("info");
                String time = resultSet.getString("time");
                String server = resultSet.getString("server");
                PlayerPoints.getConsuM().consumeDatas.put(id , new ConsumeData(id , name , point , info , time , server));
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create MySQL table.", e);
        }
    }

}
