package net.lumadevelopment.crates;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class SeniorCrates extends JavaPlugin {
	
	/*
	 * Class Layout
	 * SeniorCrates.java - Main class, initializes everything else and provides instances
	 * 
	 * Managers
	 * ConfigMgr.java - Handles everything in relation to config.yml
	 * CrateMgr.java - Handles everything in relation to crates.yml
	 * UserDataMgr.java - Handles everything in relation to the SQL database.
	 * 
	 * User Interfaces
	 * CrateCommand.java - Starts the creation and editing of crates, Fully handles the deletion and listing of crates, debug mode, daily limit reset, help command, etc.
	 * Listeners.java - Handles all physical player interactions with the plugin
	 * 
	 * Data Holders
	 * SQLInfo.java - Condenses SQL connection info into a single class to make it easier to manage (1 object vs. 5 strings)
	 */
	
	private ConfigMgr cmgr;
	private UserDataMgr usrdmgr;
	private CrateMgr cratemgr;
	public boolean debugMode = false;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		cmgr = new ConfigMgr(this);
		usrdmgr = new UserDataMgr(cmgr);
		cratemgr = new CrateMgr(cmgr);
		
		//If a configuration file doesn't exist, create it
		cmgr.initializeConfiguration();
		
		//If the 'cooldowns' SQL table doesn't exist, create it
		Connection conn = usrdmgr.establishConnection();
		usrdmgr.initializeTable(conn);
		usrdmgr.closeConn(conn);
		
		//Create/load crates.yml
		cratemgr.intializeCratesData();
		
		//Register Command
		getCommand("crate").setExecutor(new CrateCommand(cmgr, cratemgr, usrdmgr));
		
		//Register Listeners
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new Listeners(cmgr, usrdmgr, cratemgr), this);
		
		//Starts timer that checks every minute to see if it is a new day. If it is, it resets the amount of crates a user can open per day.
		BukkitScheduler scheduler = getServer().getScheduler();
		
		scheduler.scheduleAsyncRepeatingTask(this, new Runnable() {
			
			Date startDate = new Date();
			LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			
			int startmonth = startLocalDate.getMonthValue();
			int startday = startLocalDate.getDayOfMonth();
			
			@Override
			public void run() {
				Date date = new Date();
				LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				
				int month = localDate.getMonthValue();
				int day = localDate.getDayOfMonth();
				
				//Checks to see if a day has passed
				if(month > startmonth ||
						(month == startmonth && day > startday)) {
					
					//Resets the amount of crates a player can open for the day
					Connection conn = usrdmgr.establishConnection();
					usrdmgr.resetLeftInDay(conn);
					usrdmgr.closeConn(conn);
					
					//Resets calendar info so the scheduler can continue to function
					startmonth = month;
					startday = day;
					
				}
			}
		}, 0L, 1200L);
	}

}
