package net.lumadevelopment.crates;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.nbtapi.NBTItem;

public class Listeners implements Listener {
	
	private ConfigMgr cmgr;
	private UserDataMgr usrdmgr;
	private CrateMgr cratemgr;
	
	public Listeners(ConfigMgr cmgr, UserDataMgr usrdmgr, CrateMgr cratemgr) {
		this.cmgr = cmgr;
		this.usrdmgr = usrdmgr;
		this.cratemgr = cratemgr;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Connection conn = usrdmgr.establishConnection();
		usrdmgr.initializePlayer(conn, e.getPlayer().getUniqueId());
		usrdmgr.closeConn(conn);
	}
	
	/*
	 * Inventory Listeners
	 * These listeners handle the creation, editing, and opening of crates
	 */
	List<UUID> editing = new ArrayList<UUID>();
	
	@EventHandler
	public void inventoryOpen(InventoryOpenEvent e) {
		
		//Check to see if it's an edit inventory
		if(e.getInventory().getHolder() instanceof CrateInvHolder) {
			
			editing.add(e.getPlayer().getUniqueId());
			
			boolean nonNull = false;
			
			for(ItemStack i : e.getInventory().getContents()) {
				if(i != null) {
					nonNull = true;
					break;
				}
			}
			
			//See if there's no contents, this would indicate that it's a /crate create
			if(!nonNull) {
				
				//Make an item that represents unavailable slots
				for(int i = cmgr.getMaxItemsPerCrate(); i < e.getInventory().getSize(); i++) {
					e.getInventory().setItem(i, cratemgr.unavailableItem());
				}
			}
		}
	}
	
	@EventHandler
	public void inventoryInteraction(InventoryClickEvent e) {
		
		//Make sure the interactor is a Player
		if(!(e.getWhoClicked() instanceof Player)) {
			return;
		}
		
		//Make sure inventory exists
		if(e.getClickedInventory() == null) {
			return;
		}
		
		//Check if inventory is from this plugin
		if(!(e.getClickedInventory().getHolder() instanceof CrateInvHolder)) {
			return;
		}
		
		e.setCancelled(true);
		
		//Make sure the player isn't trying to modify an unavailable slot
		if(e.getCursor() != null) {
			if(e.getCursor().equals(cratemgr.unavailableItem())) {
				return;
			}
		}
		
		if(e.getCurrentItem() != null) {
			if(e.getCurrentItem().equals(cratemgr.unavailableItem())) {
				return;
			}
		}
		
		if(!e.getClick().equals(ClickType.LEFT)) {
			return;
		}
		
		//Copies the item from their hand into the inventory
		int slot = e.getSlot();
		e.getClickedInventory().setItem(slot, e.getCursor());
		
		return;
	}
	
	//Prevents any duplication glitches
	@EventHandler
	public void dropItem(PlayerDropItemEvent e) {
		if(editing.contains(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void closeInventory(InventoryCloseEvent e) {
		
		//Check if inventory is from this plugin
		if(!(e.getInventory().getHolder() instanceof CrateInvHolder)) {
			return;
		}
		
		//Checking to see if there's any real items (not Uavailable items or blank spaces) in the inventory, A.K.A. is it worth saving
		List<ItemStack> nonUnav = new ArrayList<ItemStack>();
		
		for(ItemStack i : e.getInventory().getContents()) {
			if(i != null) {
				if(!i.equals(cratemgr.unavailableItem())) {
					nonUnav.add(i);
				}
			}
		}
		
		CrateInvHolder civ = (CrateInvHolder) e.getInventory().getHolder();
		String name = civ.getCrateName();
		
		//If blank, delete the crate. If modified/created, save accordingly
		if(nonUnav.size() == 0) {
			cratemgr.deleteCrate(name);
			e.getPlayer().sendMessage(cmgr.configMsg("DeleteCrate").replaceAll("~cratename~", name));
		} else {
			if(cratemgr.crateExists(name)) {
				e.getPlayer().sendMessage(cmgr.configMsg("EditCrate").replaceAll("~cratename~", name));
			}else {
				e.getPlayer().sendMessage(cmgr.configMsg("CreateCrate").replaceAll("~cratename~", name));
			}
			
			cratemgr.saveCrate(e.getInventory());
		}
		
		editing.remove(e.getPlayer().getUniqueId());
		
		return;
	}
	
	@EventHandler
	public void interact(PlayerInteractEvent e) {
		if(e.getItem() == null) {
			return;
		}
		
		if(!cratemgr.isCrateItem(e.getItem())) {
			return;
		}
		
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack iih = e.getItem();
			e.setCancelled(true);
			
			//This is a measure to evade a bug where placing a crate on the ground deletes the item from your hand even though the event is cancelled.
			//Technically this isn't an issue because the server catches back up again, but for some reason it doesn't do that if you only had 1 crate in your hand.
			e.getPlayer().setItemInHand(iih);
			return;
		}
		
		if(e.getAction() == Action.RIGHT_CLICK_AIR) {
			e.setCancelled(true);
			
			Player p = e.getPlayer();
			
			/*
			 * Here I essentially branch the program logic into those with bypass permissions and those who don't
			 * The reason for this is that if you have the bypass permission no SQL calls need to be made
			 * On servers where the SQL server is not on the same machine that is hosting the servers,
			 * SQL calls can slow down opening crates by a couple seconds.
			 * While this isn't major, it's noticeable and I don't want to make calls where it's not necessary.
			 */
			if(!p.hasPermission("crates.bypass")) {
				Connection conn = usrdmgr.establishConnection();
				
				//This statement triggers if the player's delay is not over and they don't have the bypass permission
				if(!usrdmgr.isDelayOver(conn, p.getUniqueId())) {
					long secondsLeft = usrdmgr.leftInDelay(conn, p.getUniqueId());
					p.sendMessage(cmgr.configMsg("DelayNotOver").replaceAll("~time~", String.valueOf(secondsLeft)));
					
					usrdmgr.closeConn(conn);
					
					return;
				}
				
				//This statement triggers if the player can't open any more crates today and they don't have the bypass permission
				if(usrdmgr.getLeftInDay(conn, p.getUniqueId()) <= 0) {
					p.sendMessage(cmgr.configMsg("DailyLimitReached"));
					
					usrdmgr.closeConn(conn);
					
					return;
				}
				
				openCrate(e.getItem(), p);
				
				//Add delay
				usrdmgr.setDelayTime(conn, p.getUniqueId());
				
				//Reduce amount of crates left in day by 1
				usrdmgr.setLeftInDay(conn, usrdmgr.getLeftInDay(conn, p.getUniqueId()) - 1, p.getUniqueId());
				
				usrdmgr.closeConn(conn);
				
				return;
			}else {
				
				openCrate(e.getItem(), p);
				
			}
			
		}
	}
	
	//Container function that makes sure code isn't mirrored from bypass & non-bypass operations
	private void openCrate(ItemStack is, Player p) {
		NBTItem nbti = new NBTItem(is);
		String crateName = nbti.getString("CrateRef");
		
		//Remove item from hand or reduce amount
		if(is.getAmount() > 1) {
			is.setAmount(is.getAmount() - 1);
		} else {
			p.setItemInHand(null);
		}
		
		Inventory inv = cratemgr.getCrate(crateName);
		
		//Give all items from crate to player, as long as they're not air or an unavailable marker item
		for(int i = 0; i < inv.getSize(); i++) {
			if(inv.getItem(i) != null) {
				if(!inv.getItem(i).equals(cratemgr.unavailableItem())) {
					p.getWorld().dropItem(p.getLocation(), inv.getItem(i));
				}
			}
		}
		
		//Tell player they've opened crate
		p.sendMessage(cmgr.configMsg("CrateOpened").replaceAll("~cratename~", crateName));
	}

}
