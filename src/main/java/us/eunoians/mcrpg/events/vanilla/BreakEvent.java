package us.eunoians.mcrpg.events.vanilla;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.abilities.excavation.BuriedTreasure;
import us.eunoians.mcrpg.abilities.excavation.Extraction;
import us.eunoians.mcrpg.abilities.excavation.LargerSpade;
import us.eunoians.mcrpg.abilities.excavation.ManaDeposit;
import us.eunoians.mcrpg.abilities.herbalism.DiamondFlowers;
import us.eunoians.mcrpg.abilities.herbalism.Replanting;
import us.eunoians.mcrpg.abilities.herbalism.TooManyPlants;
import us.eunoians.mcrpg.abilities.mining.DoubleDrop;
import us.eunoians.mcrpg.abilities.mining.ItsATriple;
import us.eunoians.mcrpg.abilities.mining.RemoteTransfer;
import us.eunoians.mcrpg.abilities.mining.RicherOres;
import us.eunoians.mcrpg.abilities.woodcutting.DryadsGift;
import us.eunoians.mcrpg.abilities.woodcutting.ExtraLumber;
import us.eunoians.mcrpg.abilities.woodcutting.HeavySwing;
import us.eunoians.mcrpg.abilities.woodcutting.TemporalHarvest;
import us.eunoians.mcrpg.api.events.mcrpg.HeavySwingTestEvent;
import us.eunoians.mcrpg.api.events.mcrpg.LargerSpadeTestEvent;
import us.eunoians.mcrpg.api.events.mcrpg.PansShrineTestEvent;
import us.eunoians.mcrpg.api.events.mcrpg.excavation.BuriedTreasureEvent;
import us.eunoians.mcrpg.api.events.mcrpg.excavation.ExtractionEvent;
import us.eunoians.mcrpg.api.events.mcrpg.excavation.LargerSpadeEvent;
import us.eunoians.mcrpg.api.events.mcrpg.excavation.ManaDepositEvent;
import us.eunoians.mcrpg.api.events.mcrpg.herbalism.DiamondFlowersEvent;
import us.eunoians.mcrpg.api.events.mcrpg.herbalism.ReplantingEvent;
import us.eunoians.mcrpg.api.events.mcrpg.herbalism.TooManyPlantsEvent;
import us.eunoians.mcrpg.api.events.mcrpg.mining.DoubleDropEvent;
import us.eunoians.mcrpg.api.events.mcrpg.mining.ItsATripleEvent;
import us.eunoians.mcrpg.api.events.mcrpg.mining.RicherOresEvent;
import us.eunoians.mcrpg.api.events.mcrpg.woodcutting.DryadsGiftEvent;
import us.eunoians.mcrpg.api.events.mcrpg.woodcutting.ExtraLumberEvent;
import us.eunoians.mcrpg.api.events.mcrpg.woodcutting.HeavySwingEvent;
import us.eunoians.mcrpg.api.events.mcrpg.woodcutting.TemporalHarvestEvent;
import us.eunoians.mcrpg.api.util.*;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.players.PlayerManager;
import us.eunoians.mcrpg.types.DefaultAbilities;
import us.eunoians.mcrpg.types.GainReason;
import us.eunoians.mcrpg.types.Skills;
import us.eunoians.mcrpg.types.UnlockedAbilities;
import us.eunoians.mcrpg.util.Parser;
import us.eunoians.mcrpg.util.mcmmo.HerbalismMethods;
import us.eunoians.mcrpg.util.mcmmo.ItemUtils;
import us.eunoians.mcrpg.util.worldguard.ActionLimiterParser;
import us.eunoians.mcrpg.util.worldguard.WGRegion;
import us.eunoians.mcrpg.util.worldguard.WGSupportManager;

import java.util.*;

public class BreakEvent implements Listener{

  private static HashSet<Material> largerSpadeBlocks;

  public static boolean canLargerSpade(Material material){
    if(largerSpadeBlocks == null){
      generateMaterialSet();
    }
    return largerSpadeBlocks.contains(material);
  }

  public static Set<Material> getExcavationBlocks(){
    if(largerSpadeBlocks == null){
      generateMaterialSet();
    }
    return (Set<Material>) largerSpadeBlocks.clone();
  }

  private static void generateMaterialSet(){
    largerSpadeBlocks = new HashSet<>();
    largerSpadeBlocks.add(Material.DIRT);
    largerSpadeBlocks.add(Material.GRASS_BLOCK);
    largerSpadeBlocks.add(Material.COARSE_DIRT);
    largerSpadeBlocks.add(Material.GRAVEL);
    largerSpadeBlocks.add(Material.MYCELIUM);
    largerSpadeBlocks.add(Material.PODZOL);
    largerSpadeBlocks.add(Material.SAND);
    largerSpadeBlocks.add(Material.RED_SAND);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  @SuppressWarnings("Duplicates")
  public void breakEvent(BlockBreakEvent event){
    if(PlayerManager.isPlayerFrozen(event.getPlayer().getUniqueId())){
      return;
    }
    if(event instanceof PansShrineTestEvent){
      return;
    }
    if(!event.isCancelled() && (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE)){
      Player p = event.getPlayer();
      Block block = event.getBlock();
      McRPGPlayer mp = PlayerManager.getPlayer((p).getUniqueId());
      if(McRPG.getInstance().isWorldGuardEnabled()){
        WGSupportManager wgSupportManager = McRPG.getInstance().getWgSupportManager();
        if(wgSupportManager.isWorldTracker(event.getBlock().getWorld())){
          RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
          Location loc = event.getBlock().getLocation();
          RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
          HashMap<String, WGRegion> regions = wgSupportManager.getRegionManager().get(loc.getWorld());
          assert manager != null;
          ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
          for(ProtectedRegion region : set){
            if(regions.containsKey(region.getId()) && regions.get(region.getId()).getBreakExpressions().containsKey(event.getBlock().getType())){
              List<String> expressions = regions.get(region.getId()).getBreakExpressions().get(event.getBlock().getType());
              for(String s : expressions){
                ActionLimiterParser actionLimiterParser = new ActionLimiterParser(s, mp);
                if(actionLimiterParser.evaluateExpression()){
                  event.setCancelled(true);
                  return;
                }
              }
            }
          }
        }
      }
      FileConfiguration mining = McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG);
      FileConfiguration herbalism = McRPG.getInstance().getFileManager().getFile(FileManager.Files.HERBALISM_CONFIG);
      FileConfiguration woodCutting = McRPG.getInstance().getFileManager().getFile(FileManager.Files.WOODCUTTING_CONFIG);
      FileConfiguration excavationConfig = McRPG.getInstance().getFileManager().getFile(FileManager.Files.EXCAVATION_CONFIG);
      if(excavationConfig.getBoolean("ExcavationEnabled")){
        int dropMultiplier = 1;
        if(!McRPG.getPlaceStore().isTrue(block)){
          if(excavationConfig.contains("ExpAwardedPerBlock." + block.getType().toString())){
            int expWorth = excavationConfig.getInt("ExpAwardedPerBlock." + block.getType().toString());
            mp.giveExp(Skills.EXCAVATION, expWorth, GainReason.BREAK);
          }
          if(DefaultAbilities.EXTRACTION.isEnabled() && mp.getBaseAbility(DefaultAbilities.EXTRACTION).isToggled()){
            if(woodCutting.getStringList("ExtraLumberBlocks").contains(block.getType().toString())){
              Extraction extraction = (Extraction) mp.getBaseAbility(DefaultAbilities.EXTRACTION);
              Parser parser = DefaultAbilities.EXTRA_LUMBER.getActivationEquation();
              parser.setVariable("excavation_level", mp.getSkill(Skills.EXCAVATION).getCurrentLevel());
              parser.setVariable("power_level", mp.getPowerLevel());
              int chance = (int) (parser.getValue() * 1000);
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val){
                ExtractionEvent extractionEvent = new ExtractionEvent(mp, extraction, block);
                Bukkit.getPluginManager().callEvent(extractionEvent);
                if(!extractionEvent.isCancelled()){
                  dropMultiplier = 2;
                }
              }
            }
          }
          DropItemEvent.getBlockDropsToMultiplier().put(block.getLocation(), dropMultiplier);
          if(!(event instanceof LargerSpadeTestEvent) && UnlockedAbilities.LARGER_SPADE.isEnabled() && mp.doesPlayerHaveAbilityInLoadout(UnlockedAbilities.LARGER_SPADE)
                  && mp.getBaseAbility(UnlockedAbilities.LARGER_SPADE).isToggled() && canLargerSpade(block.getType()) && p.getItemInHand().getType().toString().contains("SHOVEL")){
            LargerSpade largerSpade = (LargerSpade) mp.getBaseAbility(UnlockedAbilities.LARGER_SPADE);
            int chance = (int) (excavationConfig.getDouble("LargerSpadeConfig.Tier" + Methods.convertToNumeral(largerSpade.getCurrentTier()) + ".ActivationChance") * 1000);
            Random rand = new Random();
            int val = rand.nextInt(100000);
            if(chance >= val){
              ArrayList<Block> blocks = new ArrayList<>();
              int radius = excavationConfig.getInt("LargerSpadeConfig.Tier" + Methods.convertToNumeral(largerSpade.getCurrentTier()) + ".Radius");
              for(int x1 = -radius; x1 <= radius; x1++){
                for(int y1 = -radius; y1 <= radius; y1++){
                  for(int z1 = -radius; z1 <= radius; z1++){
                    Block b = block.getLocation().add(x1, y1, z1).getBlock();
                    if(canLargerSpade(b.getType())){
                      if(!McRPG.getPlaceStore().isTrue(b)){
                        blocks.add(b);
                      }
                    }
                  }
                }
              }
              LargerSpadeEvent largerSpadeEvent = new LargerSpadeEvent(mp, largerSpade, blocks);
              Bukkit.getPluginManager().callEvent(largerSpadeEvent);
              if(!largerSpadeEvent.isCancelled()){
                for(Block b : blocks){
                  LargerSpadeTestEvent largerSpadeTestEvent = new LargerSpadeTestEvent(p, b);
                  Bukkit.getPluginManager().callEvent(largerSpadeTestEvent);
                  if(!largerSpadeTestEvent.isCancelled()){
                    b.breakNaturally(p.getItemInHand());
                  }
                }
              }
            }
          }
          if(UnlockedAbilities.MANA_DEPOSIT.isEnabled() && mp.doesPlayerHaveAbilityInLoadout(UnlockedAbilities.MANA_DEPOSIT) && mp.getBaseAbility(UnlockedAbilities.MANA_DEPOSIT).isToggled()){
            ManaDeposit manaDeposit = (ManaDeposit) mp.getBaseAbility(UnlockedAbilities.MANA_DEPOSIT);
            if(getExcavationBlocks().contains(block.getType())){
              int chance = (int) (excavationConfig.getDouble("ManaDepositConfig.Tier" + Methods.convertToNumeral(manaDeposit.getCurrentTier()) + ".ActivationChance") * 1000);
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val){
                int highBound = (int) excavationConfig.getDouble("ManaDepositConfig.Tier" + Methods.convertToNumeral(manaDeposit.getCurrentTier()) + ".HighBound");
                int lowBound = (int) excavationConfig.getDouble("ManaDepositConfig.Tier" + Methods.convertToNumeral(manaDeposit.getCurrentTier()) + ".LowBound");
                List<String> skills = excavationConfig.getStringList("ManaDepositConfig.Tier" + Methods.convertToNumeral(manaDeposit.getCurrentTier()) + ".Skills");
                int exp = lowBound + rand.nextInt(highBound - lowBound);
                String s = skills.get(rand.nextInt(skills.size()));
                Skills skill;
                if(s.equalsIgnoreCase("ALL")){
                  skill = Skills.values()[rand.nextInt(Skills.values().length)];
                }
                else{
                  skill = Skills.fromString(s);
                }
                ManaDepositEvent manaDepositEvent = new ManaDepositEvent(mp, manaDeposit, exp, skill);
                Bukkit.getPluginManager().callEvent(manaDepositEvent);
                if(!manaDepositEvent.isCancelled()){
                  if(manaDepositEvent.getExp() > 0){
                    mp.giveExp(manaDepositEvent.getSkill(), manaDepositEvent.getExp(), GainReason.ABILITY);
                  }
                }
              }
            }
          }
          if(UnlockedAbilities.BURIED_TREASURE.isEnabled() && mp.doesPlayerHaveAbilityInLoadout(UnlockedAbilities.BURIED_TREASURE) && mp.getBaseAbility(UnlockedAbilities.BURIED_TREASURE).isToggled()){
            BuriedTreasure buriedTreasure = (BuriedTreasure) mp.getBaseAbility(UnlockedAbilities.BURIED_TREASURE);
            if(block != null && !McRPG.getPlaceStore().isTrue(block)){
              if(BuriedTreasureData.getBuriedTreasureData().containsKey(block.getType())){
                ArrayList<String> categoriesToChooseFrom = new ArrayList<>();
                Random rand = new Random();
                String key = "BuriedTreasureConfig.Tier" + Methods.convertToNumeral(buriedTreasure.getCurrentTier()) + ".Categories";
                for(String s : excavationConfig.getConfigurationSection(key).getKeys(false)){
                  int chance = (int) (excavationConfig.getDouble(key + "." + s) * 1000);
                  int val = rand.nextInt(100000);
                  if(chance >= val){
                    categoriesToChooseFrom.add(s);
                  }
                }
                if(!categoriesToChooseFrom.isEmpty()){
                  int index = rand.nextInt(categoriesToChooseFrom.size());
                  String catToUse = categoriesToChooseFrom.get(index);
                  ArrayList<BuriedTreasureData.BuriedTreasureItem> itemsPossible = new ArrayList<>();
                  if(BuriedTreasureData.getBuriedTreasureData().get(block.getType()).containsKey(catToUse)){
                    while(itemsPossible.isEmpty()){
                      for(BuriedTreasureData.BuriedTreasureItem buriedTreasureItem : BuriedTreasureData.getBuriedTreasureData().get(block.getType()).get(catToUse)){
                        int chance = (int) (buriedTreasureItem.getDropChance() * 1000);
                        int val = rand.nextInt(100000);
                        if(chance >= val){
                          itemsPossible.add(buriedTreasureItem);
                        }
                      }
                    }
                    if(itemsPossible.size() >= 1){
                      BuriedTreasureData.BuriedTreasureItem buriedTreasureItem = itemsPossible.get(rand.nextInt(itemsPossible.size()));
                      BuriedTreasureEvent buriedTreasureEvent = new BuriedTreasureEvent(mp, buriedTreasure, buriedTreasureItem);
                      Bukkit.getPluginManager().callEvent(buriedTreasureEvent);
                      if(!buriedTreasureEvent.isCancelled()){
                        mp.getSkill(Skills.EXCAVATION).giveExp(mp, buriedTreasureEvent.getExp(), GainReason.BONUS);
                        int range = buriedTreasureEvent.getMaxAmount() - buriedTreasureEvent.getMinAmount();
                        int bonusAmount = rand.nextInt((range == 0) ? 1 : range);
                        ItemStack itemToDrop = new ItemStack(buriedTreasureEvent.getMaterial(), buriedTreasureEvent.getMinAmount() + bonusAmount);
                        p.getLocation().getWorld().dropItemNaturally(block.getLocation(), itemToDrop);
                        p.getLocation().getWorld().spawnParticle(Particle.HEART, p.getLocation(), 10);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      //Deal with woodcutting
      if(woodCutting.getBoolean("WoodcuttingEnabled")){
        int dropMultiplier = 1;
        if(!McRPG.getPlaceStore().isTrue(block)){
          if(woodCutting.contains("ExpAwardedPerBlock." + block.getType().toString())){
            int expWorth = woodCutting.getInt("ExpAwardedPerBlock." + block.getType().toString());
            mp.giveExp(Skills.WOODCUTTING, expWorth, GainReason.BREAK);
          }
          if(DefaultAbilities.EXTRA_LUMBER.isEnabled() && mp.getBaseAbility(DefaultAbilities.EXTRA_LUMBER).isToggled()){
            if(woodCutting.getStringList("ExtraLumberBlocks").contains(block.getType().toString())){
              ExtraLumber extraLumber = (ExtraLumber) mp.getBaseAbility(DefaultAbilities.EXTRA_LUMBER);
              Parser parser = DefaultAbilities.EXTRA_LUMBER.getActivationEquation();
              parser.setVariable("woodcutting_level", mp.getSkill(Skills.WOODCUTTING).getCurrentLevel());
              parser.setVariable("power_level", mp.getPowerLevel());
              int chance = (int) (parser.getValue() * 1000);
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val){
                ExtraLumberEvent extraLumberEvent = new ExtraLumberEvent(mp, extraLumber, block.getType());
                Bukkit.getPluginManager().callEvent(extraLumberEvent);
                if(!extraLumberEvent.isCancelled()){
                  dropMultiplier = 2;
                }
              }
            }
          }
          if(UnlockedAbilities.DRYADS_GIFT.isEnabled() && mp.doesPlayerHaveAbilityInLoadout(UnlockedAbilities.DRYADS_GIFT)
                  && mp.getBaseAbility(UnlockedAbilities.DRYADS_GIFT).isToggled() && !McRPG.getPlaceStore().isTrue(block) && woodCutting.contains("ExpAwardedPerBlock." + block.getType().toString())){
            DryadsGift dryadsGift = (DryadsGift) mp.getBaseAbility(UnlockedAbilities.DRYADS_GIFT);
            int chance = (int) (woodCutting.getDouble("DryadsGiftConfig.Tier" + Methods.convertToNumeral(dryadsGift.getCurrentTier()) + ".ActivationChance") * 1000);
            Random rand = new Random();
            int val = rand.nextInt(100000);
            if(chance >= val){
              int expToDrop = woodCutting.getInt("DryadsGiftConfig.Tier" + Methods.convertToNumeral(dryadsGift.getCurrentTier()) + ".ExpDropped");
              DryadsGiftEvent dryadsGiftEvent = new DryadsGiftEvent(mp, dryadsGift, expToDrop);
              Bukkit.getPluginManager().callEvent(dryadsGiftEvent);
              if(!dryadsGiftEvent.isCancelled()){
                (block.getLocation().getWorld().spawn(block.getLocation(), ExperienceOrb.class)).setExperience(dryadsGiftEvent.getExpDropped());
              }
            }
          }
          DropItemEvent.getBlockDropsToMultiplier().put(block.getLocation(), dropMultiplier);
          if(!(event instanceof HeavySwingTestEvent) && UnlockedAbilities.HEAVY_SWING.isEnabled() && mp.doesPlayerHaveAbilityInLoadout(UnlockedAbilities.HEAVY_SWING)
                  && mp.getBaseAbility(UnlockedAbilities.HEAVY_SWING).isToggled() && block.getType().toString().contains("LOG") && p.getItemInHand().getType().toString().contains("AXE")){
            HeavySwing heavySwing = (HeavySwing) mp.getBaseAbility(UnlockedAbilities.HEAVY_SWING);
            int chance = (int) (woodCutting.getDouble("HeavySwingConfig.Tier" + Methods.convertToNumeral(heavySwing.getCurrentTier()) + ".ActivationChance") * 1000);
            Random rand = new Random();
            int val = rand.nextInt(100000);
            if(chance >= val){
              ArrayList<Block> blocks = new ArrayList<>();
              int radius = woodCutting.getInt("HeavySwingConfig.Tier" + Methods.convertToNumeral(heavySwing.getCurrentTier()) + ".Radius");
              for(int x1 = -radius; x1 <= radius; x1++){
                for(int y1 = -radius; y1 <= radius; y1++){
                  for(int z1 = -radius; z1 <= radius; z1++){
                    Block b = block.getLocation().add(x1, y1, z1).getBlock();
                    if((b.getType().toString().contains("LOG") && b.getType() == block.getType()) ||
                            (b.getType().toString().contains("LEAVES") && block.getType().toString().contains(b.getType().toString().replace("_LEAVES", "")))){
                      if(!McRPG.getPlaceStore().isTrue(b)){
                        blocks.add(b);
                      }
                    }
                  }
                }
              }
              HeavySwingEvent heavySwingEvent = new HeavySwingEvent(mp, heavySwing, blocks);
              Bukkit.getPluginManager().callEvent(heavySwingEvent);
              if(!heavySwingEvent.isCancelled()){
                for(Block b : blocks){
                  HeavySwingTestEvent heavySwingTestEvent = new HeavySwingTestEvent(p, b);
                  Bukkit.getPluginManager().callEvent(heavySwingTestEvent);
                  if(!heavySwingTestEvent.isCancelled()){
                    b.breakNaturally(p.getItemInHand());
                  }
                }
              }
            }
          }
          if(mp.isReadying() && mp.getReadyingAbilityBit().getAbilityReady() == UnlockedAbilities.TEMPORAL_HARVEST){
            TemporalHarvest temporalHarvest = (TemporalHarvest) mp.getBaseAbility(UnlockedAbilities.TEMPORAL_HARVEST);
            Material saplingType = Material.AIR;
            Material woodType = Material.AIR;
            Material leafType = Material.AIR;
            if(block.getType() == Material.ACACIA_SAPLING){
              saplingType = Material.ACACIA_SAPLING;
              woodType = Material.ACACIA_LOG;
              leafType = Material.ACACIA_LEAVES;
            }
            else if(block.getType() == Material.SPRUCE_SAPLING){
              saplingType = Material.SPRUCE_SAPLING;
              woodType = Material.SPRUCE_LOG;
              leafType = Material.SPRUCE_LEAVES;
            }
            else if(block.getType() == Material.OAK_SAPLING){
              saplingType = Material.OAK_SAPLING;
              woodType = Material.OAK_LOG;
              leafType = Material.OAK_LEAVES;
            }
            else if(block.getType() == Material.DARK_OAK_SAPLING){
              saplingType = Material.DARK_OAK_SAPLING;
              woodType = Material.DARK_OAK_LOG;
              leafType = Material.DARK_OAK_LEAVES;
            }
            else if(block.getType() == Material.BIRCH_SAPLING){
              saplingType = Material.BIRCH_SAPLING;
              woodType = Material.BIRCH_LOG;
              leafType = Material.BIRCH_LEAVES;
            }
            else if(block.getType() == Material.JUNGLE_SAPLING){
              saplingType = Material.JUNGLE_SAPLING;
              woodType = Material.JUNGLE_LOG;
              leafType = Material.JUNGLE_LEAVES;
            }
            if(saplingType != Material.AIR){
              String key = "TemporalHarvestConfig.Tier" + Methods.convertToNumeral(temporalHarvest.getCurrentTier()) + ".";
              int minWood = woodCutting.getInt(key + "WoodMinDrop");
              int maxWood = woodCutting.getInt(key + "WoodMaxDrop");
              int minSapling = woodCutting.getInt(key + "SaplingsMinDrop");
              int maxSapling = woodCutting.getInt(key + "SaplingsMaxDrop");
              int minApple = woodCutting.getInt(key + "AppleMinDrop");
              int maxApple = woodCutting.getInt(key + "AppleMaxDrop");
              int cooldown = woodCutting.getInt(key + "Cooldown");
              Random rand = new Random();
              if(maxWood != 0 && maxSapling != 0 && maxApple != 0){
                ItemStack wood = new ItemStack(woodType, minWood + rand.nextInt(maxWood - minWood));
                ItemStack sapling = new ItemStack(saplingType, minSapling + rand.nextInt(maxSapling - minSapling));
                ItemStack apple = new ItemStack(saplingType, minApple + rand.nextInt(maxApple - minApple));
                TemporalHarvestEvent temporalHarvestEvent = new TemporalHarvestEvent(mp, temporalHarvest, wood.getAmount(), apple.getAmount(), sapling.getAmount(), cooldown);
                Bukkit.getPluginManager().callEvent(temporalHarvestEvent);
                if(!temporalHarvestEvent.isCancelled()){
                  int woodAmount = temporalHarvestEvent.getWoodAmount();
                  while(woodAmount > 0){
                    if(woodAmount >= 64){
                      wood.setAmount(64);
                      woodAmount -= 64;
                      block.getLocation().getWorld().dropItemNaturally(block.getLocation(), wood);
                    }
                    else{
                      wood.setAmount(woodAmount);
                      block.getLocation().getWorld().dropItemNaturally(block.getLocation(), wood);
                      woodAmount = 0;
                    }
                  }
                  int saplingAmount = temporalHarvestEvent.getSaplingAmount();
                  while(saplingAmount > 0){
                    if(saplingAmount >= 64){
                      sapling.setAmount(64);
                      saplingAmount -= 64;
                      block.getLocation().getWorld().dropItemNaturally(block.getLocation(), sapling);
                    }
                    else{
                      sapling.setAmount(saplingAmount);
                      block.getLocation().getWorld().dropItemNaturally(block.getLocation(), sapling);
                      saplingAmount = 0;
                    }
                  }
                  int appleAmount = temporalHarvestEvent.getAppleAmount();
                  while(appleAmount > 0){
                    if(appleAmount >= 64){
                      apple.setAmount(64);
                      appleAmount -= 64;
                      block.getLocation().getWorld().dropItemNaturally(block.getLocation(), apple);
                    }
                    else{
                      apple.setAmount(appleAmount);
                      block.getLocation().getWorld().dropItemNaturally(block.getLocation(), apple);
                      appleAmount = 0;
                    }
                  }
                  Calendar cal = Calendar.getInstance();
                  cal.add(Calendar.SECOND, temporalHarvestEvent.getCooldown());
                  Bukkit.getScheduler().cancelTask(mp.getReadyingAbilityBit().getEndTaskID());
                  mp.setReadyingAbilityBit(null);
                  mp.setReadying(false);
                  mp.addAbilityOnCooldown(UnlockedAbilities.TEMPORAL_HARVEST, cal.getTimeInMillis());
                }
              }
            }
          }
        }
      }
      //Deal with herbalism
      if(herbalism.getBoolean("HerbalismEnabled")){
        int dropMultiplier = 1;
        if(!McRPG.getPlaceStore().isTrue(block)){
          if(herbalism.contains("ExpAwardedPerBlock." + block.getType().toString())){
            int expWorth = herbalism.getInt("ExpAwardedPerBlock." + block.getType().toString());
            boolean oneBlockPlant = !(block.getType() == Material.CACTUS || block.getType() == Material.CHORUS_PLANT || block.getType() == Material.SUGAR_CANE);
            if(!oneBlockPlant){
              int amount = HerbalismMethods.calculateMultiBlockPlantDrops(block.getState());
              expWorth *= amount;
            }
            mp.giveExp(Skills.HERBALISM, expWorth, GainReason.BREAK);
          }
          if(DefaultAbilities.TOO_MANY_PLANTS.isEnabled() && mp.getBaseAbility(DefaultAbilities.TOO_MANY_PLANTS).isToggled()){
            if(herbalism.getStringList("TooManyPlantsBlocks").contains(block.getType().toString())){
              TooManyPlants tooManyPlants = (TooManyPlants) mp.getBaseAbility(DefaultAbilities.TOO_MANY_PLANTS);
              Parser parser = DefaultAbilities.TOO_MANY_PLANTS.getActivationEquation();
              parser.setVariable("herbalism_level", mp.getSkill(Skills.HERBALISM).getCurrentLevel());
              parser.setVariable("power_level", mp.getPowerLevel());
              int chance = (int) (parser.getValue() * 1000);
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val){
                TooManyPlantsEvent tooManyPlantsEvent = new TooManyPlantsEvent(mp, tooManyPlants, block.getType());
                Bukkit.getPluginManager().callEvent(tooManyPlantsEvent);
                if(!tooManyPlantsEvent.isCancelled()){
                  dropMultiplier = 2;
                }
              }
            }
          }
          if(McRPG.getPlaceStore().isTrue(block)){
            dropMultiplier = 1;
          }
          Material type = block.getType();
          if(UnlockedAbilities.REPLANTING.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.REPLANTING) && mp.getBaseAbility(UnlockedAbilities.REPLANTING).isToggled()){
            Replanting replanting = (Replanting) mp.getBaseAbility(UnlockedAbilities.REPLANTING);
            if(!McRPG.getPlaceStore().isTrue(block) && ItemUtils.isCrop(block.getType())){
              {
                int chance = (int) (herbalism.getDouble("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".ActivationChance") * 1000);
                Random rand = new Random();
                int val = rand.nextInt(100000);
                if(chance >= val){
                  int growChance = (int) (herbalism.getDouble("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".StageGrowthChance") * 1000);
                  int maxAge = herbalism.getInt("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".MaxGrowthLevel");
                  int minAge = herbalism.getInt("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".MinGrowthLevel");
                  boolean grow = false;
                  if(growChance >= rand.nextInt(100000)){
                    grow = true;
                  }
                  ReplantingEvent replantingEvent = new ReplantingEvent(mp, replanting, grow, maxAge, minAge);
                  Bukkit.getPluginManager().callEvent(replantingEvent);
                  if(!replantingEvent.isCancelled()){
                    block.setType(type);
                    Ageable ageable = (Ageable) block.getBlockData();
                    ageable.setAge(0);
                    if(replantingEvent.isDoStageGrowth()){
                      int tier = rand.nextInt(replantingEvent.getMaxAge() - replantingEvent.getMinAge() + 1);
                      ageable.setAge(tier);
                    }
                  }
                }
              }
            }
          }
          if(UnlockedAbilities.DIAMOND_FLOWERS.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.DIAMOND_FLOWERS) && mp.getBaseAbility(UnlockedAbilities.DIAMOND_FLOWERS).isToggled()){
            DiamondFlowers diamondFlowers = (DiamondFlowers) mp.getBaseAbility(UnlockedAbilities.DIAMOND_FLOWERS);
            if(!McRPG.getPlaceStore().isTrue(block)){
              if(DiamondFlowersData.getDiamondFlowersData().containsKey(type)){
                ArrayList<String> categoriesToChooseFrom = new ArrayList<>();
                Random rand = new Random();
                String key = "DiamondFlowersConfig.Tier" + Methods.convertToNumeral(diamondFlowers.getCurrentTier()) + ".Categories";
                for(String s : herbalism.getConfigurationSection(key).getKeys(false)){
                  int chance = (int) (herbalism.getDouble(key + "." + s) * 1000);
                  int val = rand.nextInt(100000);
                  if(chance >= val){
                    categoriesToChooseFrom.add(s);
                  }
                }
                if(!categoriesToChooseFrom.isEmpty()){
                  int index = rand.nextInt(categoriesToChooseFrom.size());
                  String catToUse = categoriesToChooseFrom.get(index);
                  ArrayList<DiamondFlowersData.DiamondFlowersItem> itemsPossible = new ArrayList<>();
                  if(DiamondFlowersData.getDiamondFlowersData().get(type).containsKey(catToUse)){
                    while(itemsPossible.isEmpty()){
                      for(DiamondFlowersData.DiamondFlowersItem diamondFlowersItem : DiamondFlowersData.getDiamondFlowersData().get(type).get(catToUse)){
                        int chance = (int) (diamondFlowersItem.getDropChance() * 1000);
                        int val = rand.nextInt(100000);
                        if(chance >= val){
                          itemsPossible.add(diamondFlowersItem);
                        }
                      }
                    }
                    DiamondFlowersData.DiamondFlowersItem diamondFlowersItem = itemsPossible.get(rand.nextInt(itemsPossible.size()));
                    DiamondFlowersEvent diamondFlowersEvent = new DiamondFlowersEvent(mp, diamondFlowers, diamondFlowersItem);
                    Bukkit.getPluginManager().callEvent(diamondFlowersEvent);
                    if(!diamondFlowersEvent.isCancelled()){
                      mp.getSkill(Skills.HERBALISM).giveExp(mp, diamondFlowersEvent.getExp(), GainReason.BONUS);
                      int range = diamondFlowersItem.getMaxAmount() - diamondFlowersItem.getMinAmount();
                      int bonusAmount = rand.nextInt((range == 0) ? 1 : range);
                      ItemStack itemToDrop = new ItemStack(diamondFlowersEvent.getMaterial(), diamondFlowersEvent.getMinAmount() + bonusAmount);
                      p.getLocation().getWorld().dropItemNaturally(block.getLocation(), itemToDrop);
                      p.getLocation().getWorld().spawnParticle(Particle.HEART, p.getLocation(), 10);
                    }
                  }
                }
              }
            }
          }
          DropItemEvent.getBlockDropsToMultiplier().put(block.getLocation(), dropMultiplier);
        }
      }
      //Deal with mining
      if(mining.getBoolean("MiningEnabled")){
        int dropMultiplier = 1;

        if(!McRPG.getPlaceStore().isTrue(block)){
          if(p.getItemInHand().getType().toString().contains("PICK") && mining.contains("ExpAwardedPerBlock." + block.getType().toString())){
            int expWorth = mining.getInt("ExpAwardedPerBlock." + block.getType().toString());
            mp.giveExp(Skills.MINING, expWorth, GainReason.BREAK);
          }
          boolean incDrops = mining.getStringList("DoubleDropBlocks").contains(block.getType().toString());
          if(DefaultAbilities.DOUBLE_DROP.isEnabled() && mp.getSkill(Skills.MINING).getAbility(DefaultAbilities.DOUBLE_DROP).isToggled()){
            DoubleDrop doubleDrop = (DoubleDrop) mp.getSkill(Skills.MINING).getAbility(DefaultAbilities.DOUBLE_DROP);
            double boost = 0;
            if(UnlockedAbilities.RICHER_ORES.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.RICHER_ORES)
                    && mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.RICHER_ORES).isToggled()){
              RicherOres richerOres = (RicherOres) mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.RICHER_ORES);
              RicherOresEvent richerOresEvent = new RicherOresEvent(mp, richerOres);
              Bukkit.getPluginManager().callEvent(richerOresEvent);
              if(!richerOresEvent.isCancelled()){
                boost = mining.getDouble("RicherOresConfig.Tier" + Methods.convertToNumeral(richerOres.getCurrentTier()) + ".ActivationBoost");
              }
            }

            Parser parser = DefaultAbilities.DOUBLE_DROP.getActivationEquation();
            parser.setVariable("mining_level", mp.getSkill(Skills.MINING).getCurrentLevel());
            parser.setVariable("power_level", mp.getPowerLevel());
            double chance = (parser.getValue() + doubleDrop.getBonusChance() + boost) * 1000;
            if(incDrops){
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val){
                DoubleDropEvent doubleDropEvent = new DoubleDropEvent(mp, doubleDrop);
                Bukkit.getPluginManager().callEvent(doubleDropEvent);
                if(!doubleDropEvent.isCancelled()){
                  dropMultiplier = 2;
                }
              }
            }
          }

          if(incDrops && UnlockedAbilities.ITS_A_TRIPLE.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.ITS_A_TRIPLE)
                  && mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.ITS_A_TRIPLE).isToggled()){
            ItsATriple itsATriple = (ItsATriple) mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.ITS_A_TRIPLE);
            double chance = (double) mining.getDouble("ItsATripleConfig.Tier" + Methods.convertToNumeral(itsATriple.getCurrentTier()) + ".ActivationChance") * 1000;
            Random rand = new Random();
            int val = rand.nextInt(100000);
            if(chance >= val){
              ItsATripleEvent itsATripleEvent = new ItsATripleEvent(mp, itsATriple);
              Bukkit.getPluginManager().callEvent(itsATripleEvent);
              if(!itsATripleEvent.isCancelled()){
                dropMultiplier = 3;
              }
            }
          }
        }

        if(McRPG.getPlaceStore().isTrue(block)){
          dropMultiplier = 1;
        }
        //Check if the block is tracked by remote transfer
        if(block.getType() == Material.CHEST && RemoteTransferTracker.isTracked(event.getBlock().getLocation())){
          UUID uuid = RemoteTransferTracker.getUUID(event.getBlock().getLocation());
          if(p.getUniqueId().equals(uuid)){
            if(!McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG).getBoolean("RemoteTransferConfig.UnlinkAndBreakOnMine")){
              event.setCancelled(true);
            }
            RemoteTransfer remoteTransfer = (RemoteTransfer) mp.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER);
            remoteTransfer.setLinkedChestLocation(null);
            mp.setLinkedToRemoteTransfer(false);
            RemoteTransferTracker.removeLocation(uuid);
            p.sendMessage(Methods.color(p, McRPG.getInstance().getPluginPrefix() + McRPG.getInstance().getLangFile().getString("Messages.Abilities.RemoteTransfer.Unlinked")));
          }
          else{
            //TODO will need to add support for admins to remove these such as towny mayors
            if(p.hasPermission("mcadmin.*") || p.hasPermission("mcadmin.unlink")){
              McRPGPlayer target;
              OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
              if(offlinePlayer.isOnline()){
                target = PlayerManager.getPlayer(uuid);
                target.getPlayer().sendMessage(Methods.color(McRPG.getInstance().getPluginPrefix() + McRPG.getInstance().getLangFile().getString("Messages.Abilities.RemoteTransfer.AdminUnlinked")));
              }
              else{
                target = new McRPGPlayer(uuid);
              }
              target.setLinkedToRemoteTransfer(false);
              ((RemoteTransfer) target.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER)).setLinkedChestLocation(null);
              RemoteTransferTracker.removeLocation(uuid);
              if(!McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG).getBoolean("RemoteTransferConfig.UnlinkAndBreakOnMine")){
                event.setCancelled(true);
              }
            }
            else{
              event.setCancelled(true);
              p.sendMessage(Methods.color(p, McRPG.getInstance().getPluginPrefix() + McRPG.getInstance().getLangFile().getString("Messages.Abilities.RemoteTransfer.IsLinked")
                      .replace("%Player%", Bukkit.getOfflinePlayer(uuid).getName())));
              return;
            }
          }
        }

        else if(UnlockedAbilities.REMOTE_TRANSFER.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.REMOTE_TRANSFER)
                && mp.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER).isToggled() && mp.isLinkedToRemoteTransfer()){
          RemoteTransfer transfer = (RemoteTransfer) mp.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER);
          int tier = transfer.getCurrentTier();
          int range = McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG).getInt("RemoteTransferConfig.Tier" + Methods.convertToNumeral(tier) + ".Range");
          if(block.getLocation().getWorld().equals(transfer.getLinkedChestLocation().getWorld())){
            if((block.getLocation().distance(transfer.getLinkedChestLocation()) <= range)){
              DropItemEvent.getBlocksToRemoteTransfer().put(block.getLocation(), p.getUniqueId());
            }
          }
        }
        DropItemEvent.getBlockDropsToMultiplier().put(block.getLocation(), dropMultiplier);
      }
    }
  }

  private enum EasterEgg{
    VERUM("HOE");

    @Getter
    private String socialStatus;

    EasterEgg(String socialStatus){
      this.socialStatus = socialStatus;
    }
  }

}
