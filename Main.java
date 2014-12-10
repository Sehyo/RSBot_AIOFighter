/**
 * Created by Alex Noble on 26/11/2014.
 */

import org.powerbot.script.*;
import org.powerbot.script.rt6.*;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.PaintListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Script.Manifest(name = "AIO Fighter", description= "Fights anything, anywhere", properties = "client=6; topic=0;")
public class Main extends PollingScript<ClientContext>  implements PaintListener
{
    // GUI Stuff
    AIOFighterGUI gui;
    // Various settings set by GUI.
    short radius;
    short foodAmount, whenToHeal;
    short foodID;
    boolean isUsingCannon;
    boolean buryBones;
    Tile originTile;
    ArrayList<Integer> monsterIDs;
    ArrayList<Integer> lootIDs;
    // Paint Variables
    int startAttackXP = 1, startDefenseXP = 1, startStrengthXP = 1, startConstitutionXP = 1, startRangeXP = 1, startMagicXP = 1;
    // Task List
    private List<Task> tasks = new ArrayList<Task>();

    public void poll()
    {
        for(Task task : tasks)
            if(task.activate()) task.execute();
    }

    public void start()
    {
        gui = new AIOFighterGUI(this);
        gui.setVisible(true);
        monsterIDs = new ArrayList<Integer>();
        lootIDs = new ArrayList<Integer>();
        // Some default values for the settings.
        radius = 10;
        foodAmount = 10;
        whenToHeal = 50;
        isUsingCannon = false; // Redundant but for clarity
        startAttackXP = ctx.skills.experience(Constants.SKILLS_ATTACK);
        startDefenseXP = ctx.skills.experience(Constants.SKILLS_DEFENSE);
        startStrengthXP = ctx.skills.experience(Constants.SKILLS_STRENGTH);
        startRangeXP = ctx.skills.experience(Constants.SKILLS_RANGE);
        startMagicXP = ctx.skills.experience(Constants.SKILLS_MAGIC);
        startConstitutionXP = ctx.skills.experience(Constants.SKILLS_CONSTITUTION);
    }

    public void populateList()
    {
        tasks.clear();
        tasks.addAll(Arrays.asList(new TaskHandleCombat(ctx, whenToHeal, foodID), new TaskHandleCannon(ctx, isUsingCannon), new TaskBuryBones(ctx, buryBones),new TaskLoot(ctx, lootIDs, originTile, radius), new TaskAttack(ctx, monsterIDs, originTile, radius)));
    }

    public MobileIdNameQuery<Npc> getNearbyNpcs()
    {
        ctx.npcs.select();
        final MobileIdNameQuery<Npc> npcs = ctx.npcs.select(refreshFilter);
        return npcs;
    }

    public void setOriginTile()
    {
        originTile = playerTile();
    }

    public Tile playerTile() { return ctx.players.local().tile(); }

    public void repaint(Graphics g)
    {
        // Simple paint method
        int startY = 100;
        long timeRan = ctx.controller.script().getRuntime() / 3600000;
        if(timeRan == 0) timeRan = 1;
        if(attackXPGained() > 0)
        {
            g.drawString("Attack XP Gained: " + attackXPGained() + " xp/hour: " + xpHour(attackXPGained()), 0, startY);
            startY += 20;
        }
        if(defenseXPGained() > 0)
        {
            g.drawString("Defense XP Gained: " + defenseXPGained() + " xp/hour: " + xpHour(defenseXPGained()), 0, startY);
            startY += 20;
        }
        if(strengthXPGained() > 0)
        {
            g.drawString("Strength XP Gained: " + strengthXPGained() + " xp/hour: " + xpHour(strengthXPGained()), 0, startY);
            startY += 20;
        }
        if(rangeXPGained() > 0) {
            g.drawString("Range XP Gained: " + rangeXPGained() + " xp/hour: " + xpHour(rangeXPGained()), 0, startY);
            startY += 20;
        }
        if(magicXPGained() > 0)
        {
            g.drawString("Magic XP Gained: " + magicXPGained() + " xp/hour: " + xpHour(magicXPGained()), 0, startY);
            startY += 20;
        }
        if(constitutionXPGained() > 0)
        {
            g.drawString("Constitution XP Gained: " + constitutionXPGained() + " xp/hour: " + xpHour(constitutionXPGained()), 0, startY);
            startY += 20;
        }
        g.drawString("Time Ran: " + formatTime(ctx.controller.script().getRuntime()),0, startY);
    }

    /**
     * Formats milliseconds to hours : minutes : seconds
     * @param runTime the run time in milliseconds.
     * @return the run time formatted to decimal format.
     */
    private String formatTime(long runTime)
    {
        DecimalFormat decFormat = new DecimalFormat("00");
        long millis = runTime;
        long hours = runTime / 3600000;
        millis -= hours * 3600000;
        long minutes = millis / 60000;
        millis -= minutes * 60000;
        long seconds = millis / 1000;
        return decFormat.format(hours) + ":" + decFormat.format(minutes) + ":" + decFormat.format(seconds);
    }

    public String xpHour(int xp)
    {
        return formatXP((int)(xp * 3600000D / ctx.controller.script().getRuntime()));
    }

    public String formatXP(int i)
    {
        DecimalFormat decFormat = new DecimalFormat("0.0");
        if(i >= 1000000) return decFormat.format((i / 1000000)) + "m";
        if(i >= 1000) return decFormat.format((i / 1000)) + "k";
        return ""+i;
    }
    // Various helper methods for the paint
    private int attackXPGained()
    {
        return ctx.skills.experience(Constants.SKILLS_ATTACK) - startAttackXP;
    }

    private int defenseXPGained()
    {
        return ctx.skills.experience(Constants.SKILLS_DEFENSE) - startDefenseXP;
    }

    private int strengthXPGained()
    {
        return ctx.skills.experience(Constants.SKILLS_STRENGTH) - startStrengthXP;
    }

    private int rangeXPGained()
    {
        return ctx.skills.experience(Constants.SKILLS_RANGE) - startRangeXP;
    }

    private int magicXPGained()
    {
        return ctx.skills.experience(Constants.SKILLS_MAGIC) - startMagicXP;
    }

    private int constitutionXPGained()
    {
        return ctx.skills.experience(Constants.SKILLS_CONSTITUTION) - startConstitutionXP;
    }

    private final Filter<Npc> refreshFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            if (npc.tile().distanceTo(originTile) <= radius) return true;
            return false;
        }
    };
}