package net.lumadevelopment.crates;

import java.sql.Connection;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CrateCommand implements CommandExecutor {

	/*
	 * Permissions
	 *   crates.admin.create - Permission to create new types of crates
	 *   crates.admin.delete - Permission to delete a type of crate
	 *   crates.admin.edit - Permission to edit a type of create
	 *   crates.admin.give - Permission to give players crates
	 *   crates.admin.list - Permission to list all existing crates
	 *   crates.admin.newday - Permission to reset all crates per day limits
	 *   
	 *   Secret Permission - crates.admin.debug - Permission to toggle the plugin's debugMode
	 *   crates.bypass - Permission to bypass the crate cooldown and daily limit.
	 */
	
	private ConfigMgr cmgr;
	private CrateMgr cratemgr;
	private UserDataMgr usrdmgr;
	
	public CrateCommand(ConfigMgr cmgr, CrateMgr cratemgr, UserDataMgr usrdmgr) {
		this.cmgr = cmgr;
		this.cratemgr = cratemgr;
		this.usrdmgr = usrdmgr;
	}
	
	//Check if a string is an integer without throwing an exception
	private boolean isInteger(String s) {
		if(s.isEmpty()) return false;
		for(int i = 0; i < s.length(); i++) {
			if(i == 0 && s.charAt(i) == '-') {
				if(s.length() == 1) return false;
				else continue;
			}
			if(Character.digit(s.charAt(i), 10) < 0) return false;
		}
		return true;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if(args.length == 0) {
			
			//If there is no arguments provided, send the help message
			sender.sendMessage(helpmsg(sender));
			
		} else if(args.length == 1) {
			
			if(args[0].equalsIgnoreCase("help")) {
				
				sender.sendMessage(helpmsg(sender));
				
			} else if(args[0].equalsIgnoreCase("newday")) {
				
				//Reloads plugin configuration file
				if(sender.hasPermission("crates.admin.newday")) {
					
					Connection conn = usrdmgr.establishConnection();
					usrdmgr.resetLeftInDay(conn);
					usrdmgr.closeConn(conn);
					
					sender.sendMessage(cmgr.configMsg("NewDay"));
				} else {
					sender.sendMessage(cmgr.configMsg("InsufficientPermissions"));
					return true;
				}
				
			} else if(args[0].equalsIgnoreCase("list")) {
				
				//List all existing crates
				if(sender.hasPermission("crates.admin.list")) {
					String msg = cmgr.configMsg("ListCrates");
					
					for(String s : cratemgr.getCrateIndex()) {
						if(cratemgr.crateExists(s)) {
							msg += "\n- " + s;
						}
					}
					
					sender.sendMessage(msg);
				}else {
					sender.sendMessage(cmgr.configMsg("InsufficientPermissions"));
					return true;
				}
			} else if(args[0].equalsIgnoreCase("debug")) {
				
				if(sender.hasPermission("crates.admin.debug")) {
					sender.sendMessage(cmgr.getPrefix() + "Debug: " + String.valueOf(cmgr.toggleDebug()));
				} else {
					//Invalid usage handler
					sender.sendMessage(cmgr.configMsg("InvalidCommandUsage"));
				}
				
			} else {
				
				//Invalid usage handler
				sender.sendMessage(cmgr.configMsg("InvalidCommandUsage"));
				
			}
		} else {
			if(args[0].equalsIgnoreCase("create")) {
				
				//Creating a new crate
				
				//Check for permissions
				if(!sender.hasPermission("crates.admin.create")) {
					sender.sendMessage(cmgr.configMsg("InsufficientPermissions"));
					return true;
				}
				
				//Seeing as creating a crate requires GUI, check if sender is a player
				if(!(sender instanceof Player)) {
					sender.sendMessage(cmgr.configMsg("MustBePlayer"));
					return true;
				}
				
				//Assemble the name from arguments
				String name = args[1];
				for(int i = 2; i < args.length; i++) {
					name += " " + args[i];
				}
				
				//Check if Crate already exists
				if(cratemgr.crateExists(name)) {
					sender.sendMessage(cmgr.configMsg("CrateAlreadyExists").replaceAll("~cratename~", name));
					return true;
				}
				
				//Have CrateMgr initialize the crate
				Player p = (Player) sender;
				cratemgr.addToIndex(name);
				
				//Properly construct inventory size
				String invName = cmgr.getPrefix() + name;
				Integer baseSize = cmgr.getMaxItemsPerCrate();
				Integer finalSize;
				
				if(baseSize > 54) {
					sender.sendMessage(cmgr.configMsg("MIPCTooBig"));
					return true;
				}
				
				//Check to see if base size is evenly divisible by 9
				if((baseSize % 9) != 0) {
					finalSize = (int) (9 * Math.ceil((double) baseSize / 9));
				}else {
					finalSize = baseSize;
				}
				
				//Create Crate Inventory
				Inventory editCrate = Bukkit.createInventory(null, finalSize, invName);
				
				p.openInventory(editCrate);
				
			} else if(args[0].equalsIgnoreCase("edit")) {
				
				//Editing a crate
				
				//Check for permissions
				if(!sender.hasPermission("crates.admin.edit")) {
					sender.sendMessage(cmgr.configMsg("InsufficientPermissions"));
					return true;
				}
				
				//Seeing as editing a crate requires GUI, check if sender is a player
				if(!(sender instanceof Player)) {
					sender.sendMessage(cmgr.configMsg("MustBePlayer"));
					return true;
				}
				
				//Assemble the name from arguments
				String name = args[1];
				for(int i = 2; i < args.length; i++) {
					name += " " + args[i];
				}
				
				//Check if Crate exists
				if(!cratemgr.crateExists(name)) {
					sender.sendMessage(cmgr.configMsg("CrateDoesntExist").replaceAll("~cratename~", name));
					return true;
				}
				
				Player p = (Player) sender;
				Inventory i = cratemgr.getCrate(name);
				
				p.openInventory(i);
				
			} else if(args[0].equalsIgnoreCase("delete")) {
				
				//Deleting a crate
				
				//Check for permissions
				if(!sender.hasPermission("crates.admin.delete")) {
					sender.sendMessage(cmgr.configMsg("InsufficientPermissions"));
					return true;
				}
				
				//Assemble the name from arguments
				String name = args[1];
				for(int i = 2; i < args.length; i++) {
					name += " " + args[i];
				}
				
				//Check if Crate exists
				if(!cratemgr.crateExists(name)) {
					sender.sendMessage(cmgr.configMsg("CrateDoesntExist").replaceAll("~cratename~", name));
					return true;
				}
				
				cratemgr.deleteCrate(name);
				sender.sendMessage(cmgr.configMsg("DeleteCrate").replaceAll("~cratename~", name));
				
			} else if(args[0].equalsIgnoreCase("give")) {
				
				//Giving a player a crate
				
				//Check for permissions
				if(!sender.hasPermission("crates.admin.give")) {
					sender.sendMessage(cmgr.configMsg("InsufficientPermissions"));
					return true;
				}
				
				//See if target player exists, and if so, is online
				String playerName = args[1];
				if(Bukkit.getPlayer(playerName) == null) {
					sender.sendMessage(cmgr.configMsg("PlayerNotOnline").replaceAll("~playername~", playerName));
					return true;
				}
				
				Player p = Bukkit.getPlayer(playerName);
				
				//Assemble the crate name from arguments
				String crateName = args[2];
				for(int i = 3; i < (args.length - 1); i++) {
					crateName += " " + args[i];
				}
				
				//Check if crate exists
				if(!cratemgr.crateExists(crateName)) {
					sender.sendMessage(cmgr.configMsg("CrateDoesntExist").replaceAll("~cratename~", crateName));
					return true;
				}
				
				//Grab amount of crate items to be given
				if(!isInteger(args[args.length - 1])) {
					sender.sendMessage(cmgr.configMsg("AmountNotValid").replaceAll("~amount~", args[args.length - 1]));
					return true;
				}
				
				Integer amount = Integer.valueOf(args[args.length - 1]);
				
				ItemStack crate = cratemgr.generateCrateItem(crateName);
				
				//Give crate items
				for(int i = 0; i < amount; i++) {
					p.getWorld().dropItem(p.getLocation(), crate);
				}
				
				sender.sendMessage(cmgr.configMsg("GiveCrate")
						.replaceAll("~playername~", playerName)
						.replaceAll("~amount~", String.valueOf(amount))
						.replaceAll("~cratename~", crateName));
				
			} else {
				//Invalid usage handler
				sender.sendMessage(cmgr.configMsg("InvalidCommandUsage"));
				
				return true;
			}
		}
		
		return true;
	}
	
	private String helpmsg(CommandSender sender) {
		//Starts building the helpmsg
		String helpmsg = cmgr.configMsg("Help.Start");
		
		//Lists all admin commands
		String[] adminCommands = {"newday", "list", "create", "edit", "delete", "give"};
		
		//This loop automatically checks to see if the sender has permission to run the command, if so, gives the help message for it
		for(String cmd : adminCommands) {
			if(sender.hasPermission("crates.admin." + cmd)) {
				helpmsg += "\n" + cmgr.configMsg("Help." + cmd.substring(0, 1).toUpperCase() + cmd.substring(1));
			}
		}
		
		return helpmsg;
	}
	
}
