package us.eunoians.mcmmox.events.vanilla;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import us.eunoians.mcmmox.Mcmmox;
import us.eunoians.mcmmox.abilities.BaseAbility;
import us.eunoians.mcmmox.api.util.Methods;
import us.eunoians.mcmmox.players.McMMOPlayer;
import us.eunoians.mcmmox.players.PlayerManager;
import us.eunoians.mcmmox.players.PlayerReadyBit;
import us.eunoians.mcmmox.types.Skills;

public class CheckReadyEvent implements Listener {

  @EventHandler(priority = EventPriority.MONITOR)
  public void checkReady(PlayerInteractEvent e){
	Player p = e.getPlayer();
	McMMOPlayer mp = PlayerManager.getPlayer(p.getUniqueId());
	ItemStack heldItem = e.getItem();
	if(e.isCancelled() && e.getAction() != Action.RIGHT_CLICK_AIR){
	  return;
	}
	Block target = e.getClickedBlock();
	Material type;
	if(target == null){
	  type = Material.AIR;
	}
	else{
	  type = target.getType();
	}
	if(type == Material.CHEST || type == Material.ENDER_CHEST ||  type == Material.TRAPPED_CHEST
  		|| type == Material.BEACON){
	  return;
	}
	//Verify that the player is in a state able to ready abilities
	//If the slot is not their hand or they arent right clicking or the player is charging or if their gamemode isnt survival or if they are holding air
	if(e.getHand() != EquipmentSlot.HAND || (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) || ShiftToggle.isPlayerCharging(p) || p.getGameMode() != GameMode.SURVIVAL || heldItem.getType() == Material.AIR){
	  return;
	}
	else{
	  //If they are already readying we dont need to do anything
	  if(mp.isReadying()){
		return;
	  }
	  else{
	    //Get the skill from the material of the item
		Skills skillType = Methods.getSkillsItem(heldItem);
		if(skillType == Skills.SWORDS){
		  if(mp.getCooldown(skillType) != -1){
		    p.sendMessage(Methods.color(Mcmmox.getInstance().getPluginPrefix() +
				Mcmmox.getInstance().getLangFile().getString("Messages.Players.CooldownActive").replace("%Skill%", skillType.getName())
					.replace("%Time%", Integer.toString((int) mp.getCooldown(skillType)))));
		    return;
		  }
		  if(mp.doesPlayerHaveActiveAbilityFromSkill(skillType)){
			BaseAbility ab = mp.getBaseAbility(mp.getActiveAbilityForSkill(skillType));
			if(!ab.isToggled() || !ab.getGenericAbility().isEnabled()){
			  return;
			}
		    p.sendMessage(Methods.color(Mcmmox.getInstance().getPluginPrefix() +
				Mcmmox.getInstance().getLangFile().getString("Messages.Players.PlayerReady").replace("%Skill_Item%", "Sword")));
			PlayerReadyBit bit = new PlayerReadyBit(mp.getActiveAbilityForSkill(skillType), mp);
			mp.setReadyingAbilityBit(bit);
			mp.setReadying(true);
		  }
		}
	  }
	}
  }
}
