package us.eunoians.mcrpg.api.displays;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.skills.Skill;
import us.eunoians.mcrpg.types.DisplayType;
import us.eunoians.mcrpg.types.Skills;

public class ExpScoreboardDisplay extends GenericDisplay implements ScoreboardBase, ExpDisplayType{

  @Getter
  private Skills skill;
  @Getter
  private Scoreboard oldBoard;

  private static ScoreboardManager manager = Bukkit.getScoreboardManager();
  @Getter
  private Scoreboard board;

  /**
   *
   * @param player Player that the scoreboard belongs to
   * @param skill Skill to be displayed
   */
  public ExpScoreboardDisplay(McRPGPlayer player, Skills skill){
    super(player, DisplayType.SCOREBOARD);
    this.skill = skill;
    createScoreboard();
  }

  /**
   *
   * @param player Player that the scoreboard belongs to
   * @param skill Skill to be displayed
   * @param old The old board to be stored. Useful for interactions with things like Factions
   */
  public ExpScoreboardDisplay(McRPGPlayer player, Skills skill, Scoreboard old){
	super(player, DisplayType.SCOREBOARD);
	this.skill = skill;
	oldBoard = old;
	createScoreboard();
  }

  private void createScoreboard(){
	FileConfiguration config = McRPG.getInstance().getConfig();
	board = manager.getNewScoreboard();
	Objective objective = board.registerNewObjective("mcrpg", "dummy");
	objective.setDisplayName(Methods.color(config.getString("DisplayConfig.Scoreboard.DisplayName").replaceAll("%Player%", player.getPlayer().getDisplayName())
	.replaceAll("%Skill%", skill.getName())));
	Skill sk = player.getSkill(skill);
	config.getConfigurationSection("DisplayConfig.Scoreboard.Lines").getKeys(false).stream().forEach(key ->{
	  String value = Methods.color(config.getString("DisplayConfig.Scoreboard.Lines." + key));
	  objective.getScore(value).setScore(0);
	  });
	sendUpdate(sk.getCurrentExp(), sk.getExpToLevel(), sk.getCurrentLevel(), 0);
  }

  /**
   *
   * @param currentExp The current exp to display
   * @param expToLevel The exp needed to display
   * @param currentLevel The current level to display
   */
  @Override
  public void sendUpdate(int currentExp, int expToLevel, int currentLevel, int expGained){
	FileConfiguration config = McRPG.getInstance().getConfig();
	Objective objective = board.getObjective("mcrpg");
	objective.getScore(Methods.color(config.getString("DisplayConfig.Scoreboard.Lines.CurrentExp"))).setScore(currentExp);
	objective.getScore(Methods.color(config.getString("DisplayConfig.Scoreboard.Lines.ExpNeeded"))).setScore(expToLevel - currentExp);
	objective.getScore(Methods.color(config.getString("DisplayConfig.Scoreboard.Lines.CurrentLevel"))).setScore(currentLevel);
	objective.setDisplaySlot(DisplaySlot.SIDEBAR);
  }

  /**
   * Cancel the display
   */
  @Override
  public void cancel(){
    board = manager.getNewScoreboard();
    player.getPlayer().setScoreboard(board);
  }

  /**
   *
   * @return true if the player has an old baord
   */
  public boolean hasOldScoreBoard(){
	return oldBoard != null;
  }

  /**
   *
   * @return The old scoreboard a player has
   */
  public Scoreboard getOldScoreBoard(){
    return this.oldBoard;
  }
}
