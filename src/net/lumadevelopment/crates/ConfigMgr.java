package net.lumadevelopment.crates;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigMgr {
	
	private SeniorCrates core;
	private FileConfiguration config;
	
	//Allows the ConfigMgr to access the configuration
	public ConfigMgr(SeniorCrates core_i) {
		core = core_i;
		config = core.getConfig();
	}
	
	//This function intiializes default configuration values
	public void initializeConfiguration() {
		
		File config = new File("plugins/SeniorCrates/config.yml");
		
		if(!config.exists()) {
			core.saveDefaultConfig();
		}
		
		return;
	}
	
	//Passes along the actual FileConfiguration object when necessary
	public FileConfiguration rawConfig() {
		return config;
	}
	
	//Reloads the configuration file
	public void reloadConfig() {
		core.reloadConfig();
	}
	
	//Provides the prefix for plugin messages.
	public String getPrefix() {
		return ChatColor.translateAlternateColorCodes('&', config.getString("Prefix"));
	}
	
	//Condenses SQL connection information into an objects and returns it
	public SQLInfo getSQLInfo() {
		core.reloadConfig();
		
		return new SQLInfo(
				config.getString("SQL.IP"),
				config.getString("SQL.Port"),
				config.getString("SQL.DatabaseName"),
				config.getString("SQL.Username"),
				config.getString("SQL.Password"));
	}
	
	//Returns the max amount of crates a player can open per day
	public Integer getCratesPerDay() {
		return config.getInt("CratesPerDay");
	}
	
	//Returns the cooldown (in seconds) for crate opening
	public Integer getCooldown() {
		return config.getInt("Cooldown");
	}
	
	//Returns the max amount of items a crate can have
	public Integer getMaxItemsPerCrate() {
		return config.getInt("MaxItemsPerCrate");
	}
	
	//Gives a message from the config with all processing already done
	public String configMsg(String key) {
		String msg = ChatColor.translateAlternateColorCodes('&', config.getString("Messages." + key));
		
		//Prefix Processing
		String msg_w_prefix = msg.replaceAll("~prefix~", getPrefix());
		
		/*
		 * if(papi){
		 *     return processed msg
		 * }
		 */
		
		return msg_w_prefix;
	}
	
	//Secret Debug Toggle
	public boolean toggleDebug() {
		if(core.debugMode == false) {
			core.debugMode = true;
			return true;
		} else {
			core.debugMode = false;
			return false;
		}
	}
	
	//Reports if debug mode is enabled
	public boolean debugEnabled() {
		return core.debugMode;
	}

}
