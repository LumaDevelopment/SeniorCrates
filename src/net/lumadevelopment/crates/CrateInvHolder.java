package net.lumadevelopment.crates;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CrateInvHolder implements InventoryHolder {

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
