package net.lumadevelopment.crates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.nbtapi.NBTItem;

public class CrateMgr {
	
	/*
	 * This class manages saving and retrieving crate info to the 'crates.yml' file
	 */
	
	private ConfigMgr cmgr;
	private File cratesDataFile;
	private FileConfiguration cratesData;
	
	public CrateMgr(ConfigMgr cmgr) {
		this.cmgr = cmgr;
	}
	
	public FileConfiguration getCratesConfig() {
		return cratesData;
	}
	
	public void saveCrateData() {
		try {
			cratesData.save(cratesDataFile);
		} catch (IOException e) {
			Bukkit.getLogger().severe("Error saving crates.yml!");
			e.printStackTrace();
		}
	}
	
	public void intializeCratesData() {
		
		//Creates crates.yml if it is not existent
		try {
			cratesDataFile = new File("plugins/SeniorCrates/crates.yml");
			if(!cratesDataFile.exists()) {
				cratesDataFile.createNewFile();
			}
		} catch(IOException e) {
			Bukkit.getLogger().severe("Error creating/loading crates.yml!");
			e.printStackTrace();
		}
		
		//Attempts to create the FileConfiguration instance and link the FileConfiguration and File
		cratesData = new YamlConfiguration();
		try {
			cratesData.load(cratesDataFile);
		} catch (IOException | InvalidConfigurationException e) {
			Bukkit.getLogger().severe("Error creating/loading crates.yml!");
			e.printStackTrace();
		}
		
		//Clear out any ghost entries in CrateIndex from interrupted creation
		if(cratesData.getStringList("CrateIndex") != null) {
			
			List<String> crateIndex = cratesData.getStringList("CrateIndex");
			List<String> toRemove = new ArrayList<String>();
			
			if(crateIndex.size() > 0) {
				for(String name : crateIndex) {
					if(cratesData.get(name) == null) {
						toRemove.add(name);
					}
				}
			}
			
			crateIndex.removeAll(toRemove);
			
			cratesData.set("CrateIndex", crateIndex);
			saveCrateData();
		}
		
		return;
		
	}
	
	//Gives the unavilable item for crate editing
	public ItemStack unavailableItem() {
		ItemStack unav = new ItemStack(Material.BARRIER);
		ItemMeta unavm = unav.getItemMeta();
		unavm.setDisplayName(ChatColor.RED + "UNAVAILABLE");
		unav.setItemMeta(unavm);
		return unav;
	}
	
	//Check if a crate already exists
	public boolean crateExists(String crateName) {
		if(cratesData.getStringList("CrateIndex") != null && cratesData.get(crateName + ".InvSize") != null) {
			if(cratesData.getStringList("CrateIndex").contains(crateName) && !cratesData.get(crateName + ".InvSize").equals(0)) {
				return true;
			}
		}
		
		return false;
	}
	
	//Add new crate to CrateIndex
	public void addToIndex(String crateName) {
		
		//Update Crate Index in crates.yml
		if(cratesData.getStringList("CrateIndex") != null) {
			List<String> index = cratesData.getStringList("CrateIndex");
			index.add(crateName);
			cratesData.set("CrateIndex", index);
			saveCrateData();
		} else {
			List<String> index = new ArrayList<String>();
			index.add(crateName);
			cratesData.set("CrateIndex", index);
			saveCrateData();
		}
		
	}
	
	//Returns raw crate index
	public List<String> getCrateIndex(){
		
		if(cratesData.getStringList("CrateIndex") != null) {
			return cratesData.getStringList("CrateIndex");
		}else {
			return new ArrayList<String>();
		}
		
	}
	
	//Saves crate inventory
	public void saveCrate(Inventory i) {
		/*
		 * The Inventory class structure is not serializable, so we'll have to get creative
		 * We'll save the ItemStacks and the size of the inventory and construct it from there
		 * This also saves minutes amount of data by not saving null entries.
		 */
		String name = i.getName().substring(cmgr.getPrefix().length());
		int size = i.getSize();
		
		cratesData.set(name + ".InvSize", size);
		
		for(int c = 0; c < size; c++) {
			cratesData.set(name + "." + String.valueOf(c), i.getItem(c));
		}
		
		saveCrateData();
	}
	
	//Recall crate inventory
	public Inventory getCrate(String name) {
		int size = cratesData.getInt(name + ".InvSize");
		Inventory inv = Bukkit.createInventory(null, size, cmgr.getPrefix() + name);
		
		for(int i = 0; i < size; i++) {
			inv.setItem(i, cratesData.getItemStack(name + "." + String.valueOf(i)));
		}
		
		return inv;
	}
	
	//Delete crate in index and remove inventory
	public void deleteCrate(String name) {
		
		if(cratesData.getStringList("CrateIndex") != null) {
			List<String> index = cratesData.getStringList("CrateIndex");
			index.remove(name);
			cratesData.set("CrateIndex", index);
		}
		
		cratesData.set(name, "");
		
		saveCrateData();
		
	}
	
	/*
	 * Creates an actual crate item that can be right clicked to extract contents
	 * I went ahead and used a NBT library for the sake of convenience and speed, but there are alternate ways to do this
	 */
	public ItemStack generateCrateItem(String name) {
		try {
			ItemStack crate = new ItemStack(Material.CHEST);
			ItemMeta crateMeta = crate.getItemMeta();
			crateMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b" + name));
			crate.setItemMeta(crateMeta);
			
			NBTItem nbti = new NBTItem(crate);
			nbti.setString("CrateRef", name);
			crate = nbti.getItem();
			
			return crate;
		} catch (Exception e) {
			Bukkit.getLogger().severe("Error creating crate item! Please ensure you have NBTAPI installed!");
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * This function verifies if an item is a crate item
	 * A.K.A., whether the plugin should care if someone right clicks it
	 */
	public boolean isCrateItem(ItemStack item) {
		NBTItem nbti = new NBTItem(item);
		if(nbti.getString("CrateRef") != null) {
			if(nbti.getString("CrateRef") != "") {
				return true;
			}
		}
		
		return false;
	}
}
