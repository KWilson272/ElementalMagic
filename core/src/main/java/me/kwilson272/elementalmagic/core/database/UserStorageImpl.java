package me.kwilson272.elementalmagic.core.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.database.UserStorage;
import me.kwilson272.elementalmagic.api.user.UserProfile;

public class UserStorageImpl implements UserStorage {

    private static class ConnectionConfig {
        @Configure(path = "Database.Sqlite.Enabled", config = Config.PLUGIN_PROPERTIES)
        private boolean useSqlite = true;
        @Configure(path = "Database.Sqlite.Name", config = Config.PLUGIN_PROPERTIES)
        private String sqliteName = "elementalmagic.db"; 
        @Configure(path = "Database.MySql.Host", config = Config.PLUGIN_PROPERTIES)
        private String host = "0.0.0.0";
        @Configure(path = "Database.MySql.Port", config = Config.PLUGIN_PROPERTIES)
        private String port = "3306";
        @Configure(path = "Database.MySql.Name", config = Config.PLUGIN_PROPERTIES)
        private String mysqlDbName = "";
        @Configure(path = "Database.MySql.Username", config = Config.PLUGIN_PROPERTIES)
        private String username = "";
        @Configure(path = "Database.Mysql.Password", config = Config.PLUGIN_PROPERTIES)
        private String password = "";
    }

    private Connection connection;
    
    private void log(Level level, String msg) {
        ElementalMagicApi.logger().log(level, "[DatabaseManager]: " + msg);
    }

	@Override
	public void enable() {
        ConnectionConfig config = new ConnectionConfig();
        ElementalMagicApi.configManager().configure(config);
    
        log(Level.INFO, "Establishing database connection...");
        try {
            connection = config.useSqlite ? 
                initSqliteConnection(config) : initSqlConnection(config); 
        } catch (Exception e) {
            log(Level.SEVERE, "Unable to establish database connection."); 
            e.printStackTrace();
            return;
        }

        setupTables();
	}

    private Connection initSqlConnection(ConnectionConfig config) 
                    throws SQLException, ClassNotFoundException {
        String host = config.host + ':' + config.port;
        String url = "jdbc:mysql://" + host + "/" + config.mysqlDbName;

        Class.forName("com.myqsl.cj.jdbc.Driver");
        return DriverManager.getConnection(url, config.username, config.password);  
    }

    private Connection initSqliteConnection(ConnectionConfig config) 
                        throws ClassNotFoundException, SQLException {
        Plugin plugin = ElementalMagicApi.plugin();
        String datafolder = plugin.getDataFolder().getAbsolutePath();
        String dbFile = datafolder + File.separator + config.sqliteName;
        String url = "jdbc:sqlite:" + dbFile;
        
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(url);
    }

    private void setupTables() {
        log(Level.INFO, "Creating empty tables...");

        try (Statement s = connection.createStatement()) {
            s.addBatch("CREATE TABLE IF NOT EXISTS users (" + 
                    "player_uuid VARCHAR(36) PRIMARY KEY NOT NULL, " +
                    "player_name VARCHAR(16))");

            s.addBatch("CREATE TABLE IF NOT EXISTS binds (" +
                    "player_uuid VARCHAR(36) NOT NULL, " + 
                    "ability_name VARCHAR(255), " + 
                    "slot_number INTEGER)");

            s.addBatch("CREATE TABLE IF NOT EXISTS presets (" + 
                    "player_uuid VARCHAR(36) NOT NULL, " + 
                    "preset_name VARCHAR(255), " +
                    "ability_name VARCHAR(255), " + 
                    "slot_number INTEGER)");

            s.addBatch("CREATE TABLE IF NOT EXISTS elements (" + 
                    "player_uuid VARCHAR(36) NOT NULL, " + 
                    "element_name VARCHAR(255), " + 
                    "toggled INTEGER CHECK (toggled IN (0, 1)))");

            s.executeBatch();
        } catch (SQLException e) {
            log(Level.SEVERE, "Unable to create tables.");
            e.printStackTrace();
        }
    }

	@Override
	public void disable(boolean shutDown) {
        closeConnection();
	}

    private void closeConnection() {
        log(Level.INFO, "Closing database connection...");
        try {
            connection.close();
        } catch (Exception e) {
            log(Level.SEVERE, "Unable to close database connection.");
            e.printStackTrace();
        } 
    }

	@Override
	public void initPlayerData(Player player) {
        if (!existsInTable(player.getUniqueId(), "users")) {
            initInUsers(player);
        } else {
            syncPlayerName(player);
        }
	}

    private void initInUsers(Player player) {
        String q = "INSERT INTO users VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName().toUpperCase());
            ps.execute();
        } catch (SQLException e) {
            log(Level.WARNING, "Couldn't insert data into player table for: " + player.getName());
            e.printStackTrace();
        }
    }

    private void syncPlayerName(Player player) {
        String q = "UPDATE users SET player_name = ? WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, player.getName().toUpperCase());
            ps.setString(2, player.getUniqueId().toString());
            ps.execute();
        } catch (SQLException e) {
            log(Level.WARNING, "Error syncing player name for: " + player.getName());
            e.printStackTrace();
        }
    }

    private boolean existsInTable(UUID uuid, String table) {
        boolean found = false;
        String q = "SELECT * FROM " + table + " WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            found = rs.next();
            rs.close();

        } catch (SQLException e) {
            log(Level.WARNING, "Error querying presence of: " + uuid.toString());
            e.printStackTrace();
        }

        return found;
    }

    @Override
    public Optional<UUID> lookupUUID(String playerName) {
        String q = "SELECT player_uuid FROM users WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, playerName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String uuid = rs.getString("player_uuid");
                return Optional.of(UUID.fromString(uuid));
            }
            rs.close();

        } catch (SQLException e) {
            log(Level.WARNING, "Error looking up uuid for: " + playerName);
            e.printStackTrace();
        }

        return Optional.empty();
    }

	@Override
	public Optional<UserProfile> loadProfile(UUID uuid) {
        if (!existsInTable(uuid, "users")) {
            return Optional.empty(); 
        }

        AbilityController[] binds = loadBinds(uuid);
        Map<Element, Boolean> elements = loadElements(uuid);
        Map<String, AbilityController[]> presets = loadPresets(uuid);
        return Optional.of(new UserProfile(binds, elements, presets));
	}

	@Override
	public void storeProfile(UUID uuid, UserProfile profile) {
        storeBinds(uuid, profile.binds());
        storeElements(uuid, profile.elements());

        Map<String, AbilityController[]> presets = profile.presets();
        for (String presetName : presets.keySet()) {
            AbilityController[] binds = presets.get(presetName);
            storePreset(uuid, binds, presetName);
        }
	}

	@Override
	public AbilityController[] loadBinds(UUID uuid) {
        AbilityController[] binds = new AbilityController[1];
        
        String q = "SELECT ability_name, slot_number FROM binds WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            binds = parseBindSet(rs);
            rs.close();
        } catch (SQLException e) {
            log(Level.WARNING, "Unable to retrieve binds for: " + uuid.toString());
            e.printStackTrace();
        }
        
        return binds;
	}

    private AbilityController[] parseBindSet(ResultSet rs) throws SQLException {
        AbilityStorage storage = ElementalMagicApi.abilityStorage();
        AbilityController[] binds = new AbilityController[9];

        while (rs.next()) {
            String ability = rs.getString("ability_name");
            int slot = rs.getInt("slot_number");
            storage.getController(ability).ifPresent(c -> binds[slot] = c);
        }
        return binds;
    }

	@Override
	public void storeBinds(UUID uuid, AbilityController[] binds) {
        deleteBinds(uuid);
        
        String q = "INSERT INTO binds VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            for (int i = 0; i < 9; ++i) {
                AbilityController controller = binds[i];
                if (controller == null) {
                    continue;
                }
                
                ps.setString(1, uuid.toString());
                ps.setString(2, controller.name());
                ps.setInt(3, i);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log(Level.WARNING, "Failed to store binds for: " + uuid.toString());
            e.printStackTrace();
        }
	}

    private void deleteBinds(UUID uuid) {
        String q = "DELETE FROM binds WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ps.execute();
        } catch (SQLException e) {
            log(Level.WARNING, "Unable to delete binds for: " + uuid.toString());
            e.printStackTrace();
        }
    } 

	@Override
	public Map<String, AbilityController[]> loadPresets(UUID uuid) {
        Map<String, AbilityController[]> presets = new HashMap<>();
        
        String q = "SELECT preset_name, ability_name, slot_number FROM presets WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            presets = parsePresets(rs); 

            rs.close();
        } catch (SQLException e) {
            log(Level.WARNING, "Failed to retrieve presets for: " + uuid.toString());
            e.printStackTrace();
        }

        return presets;
	}
    
    private Map<String, AbilityController[]> parsePresets(ResultSet rs) throws SQLException {
        Map<String, AbilityController[]> presets = new HashMap<>();
        AbilityStorage storage = ElementalMagicApi.abilityStorage();

        while (rs.next()) {
            String pName = rs.getString("preset_name");
            String aName = rs.getString("ability_name");
            int slot = rs.getInt("slot_number");

            storage.getController(aName).ifPresent(c -> {
                AbilityController[] binds = presets.computeIfAbsent(pName, 
                        k -> new AbilityController[9]);
                binds[slot] = c;
            });
        }
        
        return presets;
    }
    
	@Override
	public void storePreset(UUID uuid, AbilityController[] binds, String presetName) {
        String q = "INSERT INTO presets VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            for (int i = 0; i < 9; ++i) {
                AbilityController controller = binds[i];
                if (controller == null) {
                    continue;
                }

                ps.setString(1, uuid.toString());
                ps.setString(2, presetName.toUpperCase());
                ps.setString(3, controller.name());
                ps.setInt(4, i);
                ps.addBatch();
            }
        
            ps.executeBatch();
        } catch (SQLException e) {
            log(Level.WARNING, "Unable to save preset: " + presetName + 
                    " for: " + uuid.toString());
            e.printStackTrace();
        }   
	}

	@Override
	public void deletePreset(UUID uuid, String presetName) {
        String q = "DELETE FROM presets WHERE player_uuid = ? AND preset_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, presetName.toUpperCase());
            ps.execute();
        } catch (SQLException e) {
            log(Level.WARNING, "Unable to delete preset: " + presetName +
                    " for: " + uuid.toString());
            e.printStackTrace();
        }
	}

	@Override
	public Map<Element, Boolean> loadElements(UUID uuid) {
        Map<Element, Boolean> elements = new HashMap<>();
        
        String q = "SELECT element_name, toggled FROM elements WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            AbilityStorage storage = ElementalMagicApi.abilityStorage();
            while (rs.next()) {
                String eName = rs.getString("element_name");
                boolean toggled = rs.getBoolean("toggled");
                storage.getElement(eName).ifPresent(e -> {
                    elements.put(e, toggled);
                });
            } 
            
            rs.close();
        } catch (SQLException e) {
            log(Level.WARNING, "Couldn't load elements for: " + uuid.toString());
            e.printStackTrace();
        }
    
        return elements;
	}

	@Override
	public void storeElements(UUID uuid, Map<Element, Boolean> elements) {
        deleteElements(uuid);

        String q = "INSERT INTO elements VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            for (Element element : elements.keySet()) {
                 ps.setString(1, uuid.toString());
                 ps.setString(2, element.name());
                 ps.setBoolean(3, elements.get(element));
                 ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log(Level.WARNING, "Failed to save elements for: " + uuid.toString());
            e.printStackTrace();
        }
	}

    public void deleteElements(UUID uuid) {
        String q = "DELETE FROM elements WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, uuid.toString());
            ps.execute();
        } catch (SQLException e) {
            log(Level.WARNING, "Couldn't delete elements for: " + uuid.toString());
            e.printStackTrace();
        }
    }
}
