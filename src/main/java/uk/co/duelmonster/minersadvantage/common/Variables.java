package uk.co.duelmonster.minersadvantage.common;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import uk.co.duelmonster.minersadvantage.client.ClientFunctions;
import uk.co.duelmonster.minersadvantage.network.NetworkHandler;
import uk.co.duelmonster.minersadvantage.network.packets.PacketSynchronization;

public class Variables {
	
	private static HashMap<UUID, Variables> playerVariables = new HashMap<UUID, Variables>();
	
	public static Variables get() {
		return get(Constants.instanceUID);
	}
	
	public static Variables get(UUID uid) {
		if (playerVariables.isEmpty() || playerVariables.get(uid) == null)
			set(uid, new Variables());
		
		return playerVariables.get(uid);
	}
	
	public static Variables set(String payload) {
		return set(JsonHelper.fromJson(payload, Variables.class));
	}

	public static Variables set(Variables variables) {
		return set(Constants.instanceUID, variables);
	}

	public static Variables set(UUID uid, String payload) {
		return set(uid, JsonHelper.fromJson(payload, Variables.class));
	}
	
	public static Variables set(UUID uid, Variables variables) {
		return playerVariables.put(uid, variables);
	}
	
	public Variables() {
		this.prevHeldItem = Constants.EMPTY_ITEMSTACK;
	}
	
	public static void syncToPlayer(ServerPlayerEntity player) {
		
		if (player != null) {
			Variables variables = Variables.get();
			if (variables.hasChanged())
				NetworkHandler.sendTo(new PacketSynchronization(player.getUniqueID(), JsonHelper.toJson(variables)), player);
		}
		
	}

	public static void syncToServer() {
		
		ClientPlayerEntity player = ClientFunctions.getPlayer();
		if (player != null) {
			Variables variables = Variables.get();
			if (variables.hasChanged())
				NetworkHandler.sendToServer(new PacketSynchronization(player.getUniqueID(), JsonHelper.toJson(variables)));
		}
		
	}
	
	public boolean hasChanged() {
		String current = JsonHelper.toJson(this);
		if (current.equals(current) || history == null || history.isEmpty()) {
			history = current;
			return true;
		}
		
		return false;
	}
	
	private transient String history = null;

	public boolean	HasPlayerSpawned	= false;
	
	public boolean	skipNext		= false;
	public boolean	skipNextShaft	= false;
	public boolean	HungerNotified	= false;
	
	public Direction faceHit = Direction.SOUTH;

	//====================================================================================================
	//= Substitution variables
	//====================================================================================================
	public transient ItemStack prevHeldItem;
	public boolean shouldSwitchBack = false;
	public boolean currentlySwitched = false;
	public int prevSlot = -99;
	public int optimalSlot = -99;

	public void resetSubstitution() {
		shouldSwitchBack = false;
		currentlySwitched = false;
		prevSlot = -99;
		optimalSlot = -99;
		prevHeldItem = Constants.EMPTY_ITEMSTACK;
	}
	//====================================================================================================
	
	public boolean	IsExcavationToggled		= false;
	public boolean	IsSingleLayerToggled	= false;
	public boolean	IsShaftanationToggled	= false;
	public boolean	IsPlayerAttacking		= false;
	
	public boolean	IsCropinating	= false;
	public boolean	IsExcavating	= false;
	public boolean	IsLumbinating	= false;
	public boolean	IsPathanating	= false;
	public boolean	IsShaftanating	= false;
	public boolean	IsVeinating		= false;
	
	public boolean areAgentsProcessing() {
		return (IsCropinating ||
				IsExcavating ||
				IsLumbinating ||
				IsPathanating ||
				IsShaftanating ||
				IsVeinating);
	}
	
	public boolean IsInToggleMode() {
		return this.IsExcavationToggled || this.IsSingleLayerToggled;
	}
}
