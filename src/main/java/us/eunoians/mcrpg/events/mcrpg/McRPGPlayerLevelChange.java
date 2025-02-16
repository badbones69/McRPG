package us.eunoians.mcrpg.events.mcrpg;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.abilities.BaseAbility;
import us.eunoians.mcrpg.api.displays.DisplayManager;
import us.eunoians.mcrpg.api.displays.ExpDisplayType;
import us.eunoians.mcrpg.api.events.mcrpg.AbilityUnlockEvent;
import us.eunoians.mcrpg.api.events.mcrpg.McRPGPlayerLevelChangeEvent;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.skills.Skill;
import us.eunoians.mcrpg.types.GenericAbility;
import us.eunoians.mcrpg.types.TipType;
import us.eunoians.mcrpg.types.UnlockedAbilities;

import java.util.List;
import java.util.Random;

@SuppressWarnings("ALL")
public class McRPGPlayerLevelChange implements Listener {

  //private FileConfiguration soundFile = McRPG.getInstance().getFileManager().getFile(FileManager.Files.SOUNDS_GUI);

  @EventHandler(priority = EventPriority.NORMAL)
  public void levelChange(McRPGPlayerLevelChangeEvent e) {
    McRPG mcRPG = McRPG.getInstance();
    if(e.getNextLevel() > e.getSkillLeveled().getType().getMaxLevel()){
      e.setNextLevel( e.getSkillLeveled().getType().getMaxLevel());
    }
    e.getMcRPGPlayer().updatePowerLevel();
    //Send the player a message that they leveled up
    String message = Methods.color(e.getMcRPGPlayer().getPlayer(), mcRPG.getPluginPrefix() +
            mcRPG.getLangFile().getString("Messages.Players.LevelUp")
                    .replaceAll("%Levels%", Integer.toString(e.getAmountOfLevelsIncreased())).replaceAll("%Skill%", e.getSkillLeveled().getType().getDisplayName())
                    .replaceAll("%Current_Level%", Integer.toString(e.getNextLevel())));
    Skill skillLeveled = e.getSkillLeveled();
    skillLeveled.updateExpToLevel();
    McRPGPlayer mp = e.getMcRPGPlayer();
    //iterate across all levels gained
    for(int i = e.getPreviousLevel() + 1; i <= e.getNextLevel(); i++) {
      //if the level is at a interval to gain the player an ability point, award it to them
      if(i % mcRPG.getConfig().getInt("PlayerConfiguration.AbilityPointInterval") == 0) {
        mp.setAbilityPoints(mp.getAbilityPoints() + 1);
        //Need to fiddle with this sound
        mp.getPlayer().getLocation().getWorld().playSound(mp.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_YES, 2, 1);
        mp.getPlayer().sendMessage(Methods.color(mp.getPlayer(),mcRPG.getPluginPrefix() + mcRPG.getLangFile().getString("Messages.Players.AbilityPointGained")
                .replaceAll("%Ability_Points%", Integer.toString(e.getMcRPGPlayer().getAbilityPoints()))));
        mp.saveData();
      }
    }
    TipType tipType = TipType.getSkillTipType(e.getSkillLeveled().getType());
    if(!mp.isIgnoreTips() && !mp.getUsedTips().contains(tipType)) {
      List<String> possibleMessages = mcRPG.getLangFile().getStringList("Messages.Tips.LevelUp" + e.getSkillLeveled().getName());
      if(possibleMessages.size() > 0) {
        Random rand = new Random();
        int val = rand.nextInt(possibleMessages.size());
        mp.getPlayer().sendMessage(Methods.color(mp.getPlayer(), possibleMessages.get(val)));
        mp.getUsedTips().add(tipType);
      }
    }
    //TODO for future reference
 /**   //Do things for swords ability
    if(skillLeveled.getType().equals(Skills.SWORDS)) {
      //Get all enabled abilites
      List<String> enabledAbilities = Skills.SWORDS.getEnabledAbilities();
      //Iterate across these bois
      addToPending(e, mcRPG, skillLeveled, mp, enabledAbilities);
    }
    //Do things for mining ability
    if(skillLeveled.getType().equals(Skills.MINING)) {
      //Get all enabled abilites
      List<String> enabledAbilities = Skills.MINING.getEnabledAbilities();
      //Iterate across these bois
      addToPending(e, mcRPG, skillLeveled, mp, enabledAbilities);
    }
    //Do things for mining ability
    if(skillLeveled.getType().equals(Skills.UNARMED)) {
      //Get all enabled abilites
      List<String> enabledAbilities = Skills.UNARMED.getEnabledAbilities();
      //Iterate across these bois
      addToPending(e, mcRPG, skillLeveled, mp, enabledAbilities);
    }
    //Do things for herbalism ability
    if(skillLeveled.getType().equals(Skills.HERBALISM)) {
      //Get all enabled abilites
      List<String> enabledAbilities = Skills.HERBALISM.getEnabledAbilities();
      //Iterate across these bois
      addToPending(e, mcRPG, skillLeveled, mp, enabledAbilities);
    }
    //Do things for archery ability
    if(skillLeveled.getType().equals(Skills.ARCHERY)) {
      //Get all enabled abilites
      List<String> enabledAbilities = Skills.ARCHERY.getEnabledAbilities();
      //Iterate across these bois
      addToPending(e, mcRPG, skillLeveled, mp, enabledAbilities);
    }
    if(skillLeveled.getType().equals(Skills.WOODCUTTING)){
      List<String> enabledAbilities = Skills.WOODCUTTING.getEnabledAbilities();
      //Iterate across these bois
      addToPending(e, mcRPG, skillLeveled, mp, enabledAbilities);
    }**/

    addToPending(e, mcRPG, skillLeveled, mp, skillLeveled.getType().getEnabledAbilities());

    //Update their general info and scoreboards
    if(e.getMcRPGPlayer().isOnline()) {
      Player p = e.getMcRPGPlayer().getPlayer();
      p.sendMessage(message);
      World w = p.getWorld();
      w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1);
      if(!McRPG.getInstance().getDisplayManager().doesPlayerHaveDisplay(e.getMcRPGPlayer().getPlayer())) {
        return;
      }
      DisplayManager displayManager = McRPG.getInstance().getDisplayManager();
      if(displayManager.doesPlayerHaveDisplay(p)) {
        if(displayManager.getDisplay(p) instanceof ExpDisplayType) {
          ExpDisplayType expDisplayType = (ExpDisplayType) displayManager.getDisplay(p);
          Skill skill = mp.getSkill(expDisplayType.getSkill());
          expDisplayType.sendUpdate(skill.getCurrentExp(), skill.getExpToLevel(), skill.getCurrentLevel(), 0);
        }
      }
    }
  }

  private void addToPending(McRPGPlayerLevelChangeEvent e, McRPG mcRPG, Skill skillLeveled, McRPGPlayer mp, List<String> enabledAbilities) {
    for(String s : enabledAbilities) {
      //Get the generic ability.
      GenericAbility ability = skillLeveled.getGenericAbility(s);
      //If its unlocked
      if(ability instanceof UnlockedAbilities) {
        //We get variables and verify that its not already unlocked
        UnlockedAbilities ab = (UnlockedAbilities) ability;
        BaseAbility base = skillLeveled.getAbility(ability);
        if(base.isUnlocked()) {
          continue;
        }
        else {
          //Otherwise we check if they are allowed to unlock the ability
          if(e.getNextLevel() >= ab.getUnlockLevel()) {
            AbilityUnlockEvent abilityUnlockEvent = new AbilityUnlockEvent(mp, base);
            Bukkit.getPluginManager().callEvent(abilityUnlockEvent);
            if(abilityUnlockEvent.isCancelled()) {
              return;
            }
            boolean denyFitness = mp.getAbilityLoadout().stream().filter(abilitiy -> abilitiy.getSkill().equalsIgnoreCase("Fitness")).count() >= 2;
            if(mp.isOnline()) {
              Player p = mp.getPlayer();
              if(mp.isAutoDeny() || denyFitness) {
                p.sendMessage(Methods.color(mp.getPlayer(),mcRPG.getPluginPrefix() +
                        mcRPG.getLangFile().getString("Messages.Players.AbilityUnlockedButDenied").replaceAll("%Ability%", ab.getName())));
              }
              else {
                p.sendMessage(Methods.color(mp.getPlayer(),mcRPG.getPluginPrefix() +
                        mcRPG.getLangFile().getString("Messages.Players.AbilityUnlocked").replaceAll("%Ability%", ab.getName())));
                p.getLocation().getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3, 1);
                mp.addPendingAbilityUnlock(ab);
              }
            }
            base.setUnlocked(true);
            base.setCurrentTier(1);
            mp.saveData();
          }
          else {
            continue;
          }
        }
      }
      else {
        continue;
      }
    }
  }
}
