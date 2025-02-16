package us.eunoians.mcrpg.events.vanilla;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.abilities.BaseAbility;
import us.eunoians.mcrpg.abilities.herbalism.NaturesWrath;
import us.eunoians.mcrpg.api.events.mcrpg.herbalism.NaturesWrathEvent;
import us.eunoians.mcrpg.api.util.FileManager;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.players.PlayerManager;
import us.eunoians.mcrpg.players.PlayerReadyBit;
import us.eunoians.mcrpg.types.Skills;
import us.eunoians.mcrpg.types.UnlockedAbilities;

import java.util.ArrayList;
import java.util.UUID;

public class CheckReadyEvent implements Listener {

  private static final ArrayList<UUID> playersToIgnore = new ArrayList<>();

  @EventHandler(priority = EventPriority.MONITOR)
  public void checkReady(PlayerInteractEvent e) {
    if(PlayerManager.isPlayerFrozen(e.getPlayer().getUniqueId())){
      return;
    }
    Player p = e.getPlayer();
    McRPGPlayer mp = PlayerManager.getPlayer(p.getUniqueId());
    ItemStack heldItem = e.getItem();
    //verify a proper ready action/special case for archery
    if(e.isCancelled() && e.getAction() != Action.RIGHT_CLICK_AIR) {
      if(e.getHand() != null && e.getAction() != null && heldItem != null && e.getHand() == EquipmentSlot.HAND && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && heldItem.getType() == Material.BOW) {
        if(mp.isReadying()){
          return;
        }
        Skills skillType = Skills.ARCHERY;
        if(mp.getCooldown(skillType) != -1) {
          p.sendMessage(Methods.color(p, McRPG.getInstance().getPluginPrefix() +
                  McRPG.getInstance().getLangFile().getString("Messages.Players.CooldownActive").replace("%Skill%", skillType.getDisplayName())
                          .replace("%Time%", Integer.toString((int) mp.getCooldown(skillType)))));
          return;
        }
        readyHandler(p, mp, skillType, e.getClickedBlock());
        return;
      }
      return;
    }
    Block target = e.getClickedBlock();
    Material type;
    if(target == null) {
      type = Material.AIR;
    }
    else {
      type = target.getType();
    }
    if(heldItem == null) {
      heldItem = new ItemStack(Material.AIR);
    }
    if(type == Material.CHEST || type == Material.ENDER_CHEST || type == Material.TRAPPED_CHEST
            || type == Material.BEACON) {
      return;
    }
    if((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && UnlockedAbilities.NATURES_WRATH.isEnabled() &&
            mp.getAbilityLoadout().contains(UnlockedAbilities.NATURES_WRATH) && mp.getBaseAbility(UnlockedAbilities.NATURES_WRATH).isToggled()) {
      //verify they have a weapon or tool. prevents annoying food bug
      if(Methods.getSkillsItem(p.getInventory().getItemInMainHand().getType(), type) != null && !playersToIgnore.contains(p.getUniqueId())) {
        NaturesWrath naturesWrath = (NaturesWrath) mp.getBaseAbility(UnlockedAbilities.NATURES_WRATH);
        FileConfiguration herbalism = McRPG.getInstance().getFileManager().getFile(FileManager.Files.HERBALISM_CONFIG);
        String key = "NaturesWrathConfig.Tier" + Methods.convertToNumeral(naturesWrath.getCurrentTier()) + ".";
        ItemStack offHand = p.getInventory().getItemInOffHand();
        if((offHand != null && Methods.isDiamondFlower(offHand.getType())) ||
                (p.getItemInHand() != null && Methods.isDiamondFlower(p.getItemInHand().getType()))) {
          boolean isOffHand = Methods.isDiamondFlower(offHand.getType());
          Material itemType = isOffHand ? offHand.getType() : p.getItemInHand().getType();
          if(itemType == Material.POPPY && herbalism.getBoolean(key + itemType.toString())) {
            int hungerLost = herbalism.getInt(key + "HungerLost");
            int hungerLimit = herbalism.getInt(key + "HungerLimit");
            if(p.getFoodLevel() - hungerLost >= hungerLimit) {
              int modifier = herbalism.getInt(key + "Modifier");
              int duration = herbalism.getInt(key + "Duration");
              PotionEffectType effectType = PotionEffectType.INCREASE_DAMAGE;
              NaturesWrathEvent naturesWrathEvent = new NaturesWrathEvent(mp, naturesWrath, hungerLost, modifier, effectType, duration);
              Bukkit.getPluginManager().callEvent(naturesWrathEvent);
              if(!naturesWrathEvent.isCancelled()) {
                p.getLocation().getWorld().spawnParticle(Particle.ITEM_CRACK, p.getEyeLocation(), 1, 0, 0, 0, new ItemStack(Material.POPPY));
                p.getLocation().getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 5, 1);
                if(isOffHand){
                  p.getInventory().getItemInOffHand().setAmount(p.getInventory().getItemInOffHand().getAmount() - 1);
                }
                else{
                  p.getItemInHand().setAmount(p.getItemInHand().getAmount());
                }
                p.updateInventory();
                p.setFoodLevel(p.getFoodLevel() - naturesWrathEvent.getHungerLost());
                p.addPotionEffect(new PotionEffect(naturesWrathEvent.getEffectType(), naturesWrathEvent.getDuration() * 20, naturesWrathEvent.getModifier() - 1));
              }
            }
          }
          else if(itemType == Material.DANDELION && herbalism.getBoolean(key + itemType.toString())) {
            int hungerLost = herbalism.getInt(key + "HungerLost");
            int hungerLimit = herbalism.getInt(key + "HungerLimit");
            if(p.getFoodLevel() - hungerLost >= hungerLimit) {
              int modifier = herbalism.getInt(key + "Modifier");
              int duration = herbalism.getInt(key + "Duration");
              PotionEffectType effectType = PotionEffectType.FAST_DIGGING;
              NaturesWrathEvent naturesWrathEvent = new NaturesWrathEvent(mp, naturesWrath, hungerLost, modifier, effectType, duration);
              Bukkit.getPluginManager().callEvent(naturesWrathEvent);
              if(!naturesWrathEvent.isCancelled()) {
                p.getLocation().getWorld().spawnParticle(Particle.ITEM_CRACK, p.getEyeLocation(), 1, 0, 0, 0, new ItemStack(Material.DANDELION));
                p.getLocation().getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 5, 1);
                if(isOffHand){
                  p.getInventory().getItemInOffHand().setAmount(p.getInventory().getItemInOffHand().getAmount() - 1);
                }
                else{
                  p.getItemInHand().setAmount(p.getItemInHand().getAmount());
                }                p.updateInventory();
                p.setFoodLevel(p.getFoodLevel() - naturesWrathEvent.getHungerLost());
                p.addPotionEffect(new PotionEffect(naturesWrathEvent.getEffectType(), naturesWrathEvent.getDuration() * 20, naturesWrathEvent.getModifier() - 1));
              }
            }
          }
          else if(itemType == Material.BLUE_ORCHID && herbalism.getBoolean(key + itemType.toString())) {
            int hungerLost = herbalism.getInt(key + "HungerLost");
            int hungerLimit = herbalism.getInt(key + "HungerLimit");
            if(p.getFoodLevel() - hungerLost >= hungerLimit) {
              int modifier = herbalism.getInt(key + "Modifier");
              int duration = herbalism.getInt(key + "Duration");
              PotionEffectType effectType = PotionEffectType.SPEED;
              NaturesWrathEvent naturesWrathEvent = new NaturesWrathEvent(mp, naturesWrath, hungerLost, modifier, effectType, duration);
              Bukkit.getPluginManager().callEvent(naturesWrathEvent);
              if(!naturesWrathEvent.isCancelled()) {
                p.getLocation().getWorld().spawnParticle(Particle.ITEM_CRACK, p.getEyeLocation(), 1, 0, 0, 0, new ItemStack(Material.BLUE_ORCHID));
                p.getLocation().getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 5, 1);
                if(isOffHand){
                  p.getInventory().getItemInOffHand().setAmount(p.getInventory().getItemInOffHand().getAmount() - 1);
                }
                else{
                  p.getItemInHand().setAmount(p.getItemInHand().getAmount());
                }                p.updateInventory();
                p.setFoodLevel(p.getFoodLevel() - naturesWrathEvent.getHungerLost());
                p.addPotionEffect(new PotionEffect(naturesWrathEvent.getEffectType(), naturesWrathEvent.getDuration() * 20, naturesWrathEvent.getModifier() - 1));
              }
            }
          }
          else if(itemType == Material.LILAC && herbalism.getBoolean(key + itemType.toString())) {
            int hungerLost = herbalism.getInt(key + "HungerLost");
            int hungerLimit = herbalism.getInt(key + "HungerLimit");
            if(p.getFoodLevel() - hungerLost >= hungerLimit) {
              int modifier = herbalism.getInt(key + "Modifier");
              int duration = herbalism.getInt(key + "Duration");
              PotionEffectType effectType = PotionEffectType.DAMAGE_RESISTANCE;
              NaturesWrathEvent naturesWrathEvent = new NaturesWrathEvent(mp, naturesWrath, hungerLost, modifier, effectType, duration);
              Bukkit.getPluginManager().callEvent(naturesWrathEvent);
              if(!naturesWrathEvent.isCancelled()) {
                p.getLocation().getWorld().spawnParticle(Particle.ITEM_CRACK, p.getEyeLocation(), 1, 0, 0, 0, new ItemStack(Material.LILAC));
                p.getLocation().getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 5, 1);
                if(isOffHand){
                  p.getInventory().getItemInOffHand().setAmount(p.getInventory().getItemInOffHand().getAmount() - 1);
                }
                else{
                  p.getItemInHand().setAmount(p.getItemInHand().getAmount());
                }                p.updateInventory();
                p.setFoodLevel(p.getFoodLevel() - naturesWrathEvent.getHungerLost());
                p.addPotionEffect(new PotionEffect(naturesWrathEvent.getEffectType(), naturesWrathEvent.getDuration() * 20, naturesWrathEvent.getModifier() - 1));
              }
            }
          }
          playersToIgnore.add(p.getUniqueId());
          new BukkitRunnable(){
            @Override
            public void run(){
              playersToIgnore.remove(p.getUniqueId());
            }
          }.runTaskLater(McRPG.getInstance(), 3 * 20);
        }
      }
    }
    //Verify that the player is in a state able to ready abilities
    //If the slot is not their hand or they arent right clicking or the player is charging or if their gamemode isnt survival or if they are holding air

    if(e.getHand() == null || e.getHand() != EquipmentSlot.HAND || (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) || ShiftToggle.isPlayerCharging(p) || p.getGameMode() != GameMode.SURVIVAL) {
      return;
    }
    else {
      //If they are already readying we dont need to do anything
      if(mp.isReadying()) {
        return;
      }
      else {
        //Get the skill from the material of the item
        Block b = e.getClickedBlock();
        Material m = Material.AIR;
        if(b != null){
          m = b.getType();
        }
        Material heldItemType = heldItem.getType();
        if(heldItem.getType() == Material.AIR && Methods.specialHandDigggingCase(m) && mp.getAbilityLoadout().contains(UnlockedAbilities.HAND_DIGGING)){
          heldItemType = Material.DIAMOND_SHOVEL;
        }
        Skills skillType = Methods.getSkillsItem(heldItemType, m);
        if(skillType == null || skillType == Skills.ARCHERY) {
          return;
        }
        if(p.getInventory().getItemInOffHand().getType() == Material.POPPY || p.getInventory().getItemInOffHand().getType() == Material.DANDELION
                || p.getInventory().getItemInOffHand().getType() == Material.LILAC || p.getInventory().getItemInOffHand().getType() == Material.BLUE_ORCHID) {
          if(UnlockedAbilities.NATURES_WRATH.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.NATURES_WRATH) && mp.getBaseAbility(UnlockedAbilities.NATURES_WRATH).isToggled()) {
            if(!playersToIgnore.contains(p.getUniqueId())) {
              return;
            }
          }
        }
        if(McRPG.getInstance().getConfig().getBoolean("Configuration.RequireEmptyOffHand") && p.getInventory().getItemInOffHand().getType() != Material.AIR) {
          return;
        }
        if(mp.getCooldown(skillType) > -1) {
          p.sendMessage(Methods.color(p, McRPG.getInstance().getPluginPrefix() +
                  McRPG.getInstance().getLangFile().getString("Messages.Players.CooldownActive").replace("%Skill%", skillType.getDisplayName())
                          .replace("%Time%", Integer.toString((int) mp.getCooldown(skillType)))));
          return;
        }
        else if(mp.getCooldown(skillType) <= -1){
          mp.removeAbilityOnCooldown(skillType);
        }
        readyHandler(p, mp, skillType, e.getClickedBlock());
      }
    }
  }

  private void readyHandler(Player p, McRPGPlayer mp, Skills skillType, Block block) {
    if(mp.doesPlayerHaveActiveAbilityFromSkill(skillType)) {
      BaseAbility ab = mp.getBaseAbility(mp.getActiveAbilityForSkill(skillType));
      if(mp.getActiveAbilities().contains(ab.getGenericAbility())) {
        return;
      }
      if(!ab.isToggled() || !ab.getGenericAbility().isEnabled() || ab.getGenericAbility() == UnlockedAbilities.NATURES_WRATH
      || ab.getGenericAbility() == UnlockedAbilities.DEMETERS_SHRINE || ab.getGenericAbility() == UnlockedAbilities.HESPERIDES_APPLES) {
        return;
      }
      String skill = "";
      if(skillType == Skills.SWORDS) {
        skill = "Sword";
      }
      else if(skillType == Skills.MINING) {
        skill = "Pickaxe";
      }
      else if(skillType == Skills.UNARMED || (skillType == Skills.EXCAVATION && ab.getGenericAbility() == UnlockedAbilities.HAND_DIGGING)) {
        skill = "Fist";
      }
      else if(skillType == Skills.EXCAVATION){
        skill = "Shovel";
      }
      else if(skillType == Skills.HERBALISM) {
        if(block == null){
          return;
        }
        if(block.getType() == Material.GRASS_BLOCK) {
          return;
        }
        skill = "Hoe";
      }
      else if(skillType == Skills.ARCHERY) {
        skill = "Bow";
      }
      else if(skillType == Skills.WOODCUTTING){
        //To deal with bark stripping
        if(McRPG.getInstance().getFileManager().getFile(FileManager.Files.WOODCUTTING_CONFIG).getBoolean("CrouchForReady") &&
        !p.isSneaking() && block.getType().toString().contains("LOG") && !block.getType().toString().contains("STRIPPED")){
          return;
        }
        skill = "Axe";
      }
      else if(skillType == Skills.AXES){
        skill = "Battle Axe";
      }
      p.sendMessage(Methods.color(p, McRPG.getInstance().getPluginPrefix() +
              McRPG.getInstance().getLangFile().getString("Messages.Players.PlayerReady").replace("%Skill_Item%", skill)));
      PlayerReadyBit bit = new PlayerReadyBit(mp.getActiveAbilityForSkill(skillType), mp);
      mp.setReadyingAbilityBit(bit);
      mp.setReadying(true);
    }
  }
}
