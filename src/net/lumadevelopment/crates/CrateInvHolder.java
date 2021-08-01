package net.lumadevelopment.crates;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CrateInvHolder implements InventoryHolder {

	/*
	 * CrateInvHolder implements InventoryHolder and it allows the plugin to identify inventories belonging to it.
	 * It also makes crate name referencing easier without having to worry about conflictions.
	 * This is also a solution that works cross version without reflections, as InventoryHolder
	 * was one of the fields that survived the Inventory API revamp.
	 */
	
	private String crateName;
	
	public CrateInvHolder(String crateName) {
		this.crateName = crateName;
	}
	
	public String getCrateName() {
		return crateName;
	}
	
	@Override
	public Inventory getInventory() {
		return null;
	}

}
