/**
 * Created by Alex Noble on 26/11/2014.
 */

import org.powerbot.script.*;
import org.powerbot.script.rt6.*; // Fix later so don't import everything
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.PaintListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.Random;

@Script.Manifest(name = "AIO Fighter", description= "Fights anything, anywhere", properties = "client=6; topic=0;")
public class Main extends PollingScript<ClientContext>  implements PaintListener
{
    // GUI Stuff
    AIOFighterGUI gui;
    // Various settings set by GUI.
    final int millisInAnHour = 3600000;
    int radius;
    int foodAmount, whenToHeal;
    short foodID;
    boolean isUsingCannon;
    Tile originTile;
    ArrayList<Integer> monsterIDs;
    ArrayList<Integer> lootIDs;
    boolean activated;
    Npc currentTarget;
    boolean cannonSetup;
    final int[] normalCannonPartIDs = {6,8,10,12};
    final int[] goldCannonPartIDs = {20494, 20495, 20496, 20497};
    final int[] royaleCannonPartIDs = {20498, 20499, 20500, 20501};
    Tile cannonTile;
    // Varpbit info for cannon
    final int cannonVarpbit = 2733;
    final int outOfCannonAmmoValue = 2048;
    final int cannonIsFiringValue = 1050624;
    // Paint Variables
    int startAttackXP = 1, startDefenseXP = 1, startStrengthXP = 1, startConstitutionXP = 1, startRangeXP = 1, startMagicXP = 1;
    // Loot Item variable made global due to not sure if you can send params to a Callable<T>
    GroundItem lootItem = null;
    // Ends here

    public void poll()
    {
        switch(state())
        {
            case WAITING_FOR_ACTIVATION:
                // Do nothing?
                break;
            case WAIT_FOR_NPC_SPAWN:
                // Add some antiban shit later.
                break;
            case IN_COMBAT:
                handleCombat();
                break;
            case SETUP_CANNON:
                setupCannon();
                break;
            case LOAD_CANNON:
                loadCannon();
                break;
            case NO_MORE_BALLS:
                pickupCannon();
                break;
            case ATTACK:
                attack();
                break;
            case LOOT:
                loot();
                break;
            default:
                // Do nothing...
                break;
        }
    }
    /**
     * Initializes script. Launches GUI etc.
     */
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

    private State state()
    {
        if(!activated)                                  return State.WAITING_FOR_ACTIVATION;
        if(ctx.players.local().interacting().valid())   return State.IN_COMBAT;
        if(isUsingCannon && !cannonSetup)               return State.SETUP_CANNON;
        if(isUsingCannon && isOutOfAmmo())              return State.LOAD_CANNON;
        if(isUsingCannon && cannonSetup && !ballsAvailable())  return State.NO_MORE_BALLS;
        if(lootAvailable()) return State.LOOT;
        ctx.npcs.select();
        if(ctx.npcs.select(monsterFilter).isEmpty()) return State.WAIT_FOR_NPC_SPAWN;
        return State.ATTACK; // No loot, no cannon to load, no npc attacking us.. ATTACK :D
    }

    private enum State
    {
        WAITING_FOR_ACTIVATION,
        SETUP_CANNON,
        LOAD_CANNON,
        NO_MORE_BALLS,
        IN_COMBAT,
        ATTACK,
        LOOT,
        WAIT_FOR_NPC_SPAWN,
        SHIT
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

    private boolean lootAvailable()
    {
        ctx.groundItems.select();
        if(ctx.groundItems.select(lootFilter).isEmpty()) return false;
        return true;
    }

    private boolean ballsAvailable() // Check if we have cannon balls left to feed the cannon with.
    {
        ctx.backpack.select();
        return ctx.backpack.id(2).count() > 0; // count() should equal 1 if balls available
    }

    private void loot()
    {
        while(lootAvailable()) // Remove ! when done
        {
            // Pick stuff up..
            ctx.groundItems.select();
            lootItem = ctx.groundItems.select(lootFilter).nearest().poll();
            getTileInViewport(lootItem.tile());
            lootItem.interact("Take", lootItem.name());
            Random random = new Random();
            Condition.wait(itemLooted, 400 - random.nextInt(200), 10 - random.nextInt(4)); // Sleep for maximum of 400, minimum of 200. 6 to 10 times.
        }
    }

    private void setupCannon()
    {
        if(!cannonSetup)
        {
            // Cannon is not setup. Set it up if we have one!
                if((ctx.backpack.select().id(normalCannonPartIDs[0]).count() > 0 && ctx.backpack.select().id(normalCannonPartIDs[1]).count() > 0 && ctx.backpack.select().id(normalCannonPartIDs[2]).count() > 0 && ctx.backpack.select().id(normalCannonPartIDs[3]).count() > 0) || (ctx.backpack.select().id(goldCannonPartIDs[0]).count() > 0 && ctx.backpack.select().id(goldCannonPartIDs[1]).count() > 0 && ctx.backpack.select().id(goldCannonPartIDs[2]).count() > 0 && ctx.backpack.select().id(goldCannonPartIDs[3]).count() > 0) || (ctx.backpack.select().id(royaleCannonPartIDs[0]).count() > 0 && ctx.backpack.select().id(royaleCannonPartIDs[1]).count() > 0 && ctx.backpack.select().id(royaleCannonPartIDs[2]).count() > 0 && ctx.backpack.select().id(royaleCannonPartIDs[3]).count() > 0))
                {
                    // We got one :)
                    // Set a cannon up at the current tile (which should be the tile the player started the script at).
                    cannonTile = ctx.players.local().tile();
                    final Item cannonBase = getCannonBase();
                    if(cannonBase != null) cannonBase.interact("Set-up");
                    cannonSetup = true;
                    ctx.backpack.select();
                    Random random = new Random();
                    Condition.wait(cannonPlaced, 400 - random.nextInt(200), 30 - random.nextInt(4)); // Sleep for maximum of 400, minimum of 200. 30 to 26 times.
                    Condition.sleep(1200 - random.nextInt(600));
                    loadCannon();
                }
        }
    }

    private void loadCannon()
    {
        ctx.backpack.select();
        if(ctx.varpbits.varpbit(cannonVarpbit) == 0)
        {
            isUsingCannon = false;
            return; // No cannon anymore.
        }
        if(ctx.backpack.id(2).count() == 0)
        {
            pickupCannon(); // No ammo left but we have a cannon out.
            isUsingCannon = false;
        }
        getTileInViewport(cannonTile);
        // Get cannon GameObject.
        ctx.objects.select();
        final GameObject cannon = ctx.objects.select(cannonFilter).poll();
        cannon.interact("Fire");
        // Wait till cannon is loaded
        Random random = new Random();
        Condition.wait(cannonLoaded, 400 - random.nextInt(200), 30 - random.nextInt(4)); // Sleep for maximum of 400, minimum of 200. 30 to 26 times.
    }

    private void pickupCannon()
    {
        ctx.objects.select();
        final GameObject cannon = ctx.objects.select(cannonFilter).poll();
        getTileInViewport(cannon.tile());
        Random random = new Random();
        Condition.wait(cannonPickedUp, 400 - random.nextInt(200), 30 - random.nextInt(4)); // Sleep for maximum of 400, minimum of 200. 30 to 26 times.
    }

    private void getTileInViewport(Tile tile)
    {
        while(!tile.matrix(ctx).inViewport())
        {
            if(!ctx.players.local().inMotion())
            {
                // Move towards npc.
                ctx.movement.step(tile);
                ctx.camera.turnTo(tile);
            }
        }
    }

    private boolean isOutOfAmmo()
    {
        return ctx.varpbits.varpbit(cannonVarpbit) == outOfCannonAmmoValue;
    }

    private Item getCannonBase()
    {
        if((ctx.backpack.select().id(normalCannonPartIDs[0]).count() > 0))    return ctx.backpack.select().id(normalCannonPartIDs[0]).poll();
        else if((ctx.backpack.select().id(goldCannonPartIDs[0]).count() > 0))    return ctx.backpack.select().id(goldCannonPartIDs[0]).poll();
        else if((ctx.backpack.select().id(royaleCannonPartIDs[0]).count() > 0))    return ctx.backpack.select().id(royaleCannonPartIDs[0]).poll();
        return null;
    }

    private void attack()
    {
        ctx.npcs.select();
        if(ctx.npcs.select(monsterFilter).isEmpty()) return; // No monsters :(
        currentTarget = ctx.npcs.select(monsterFilter).nearest().limit(3).shuffle().poll(); // Anti pattern
        while(!currentTarget.tile().matrix(ctx).inViewport() && currentTarget.valid())
        {
            if(!currentTarget.valid()) return; // npc dead.
            if(!ctx.players.local().inMotion())
            {
                // Move towards npc.
                ctx.movement.step(currentTarget.tile());
                ctx.camera.turnTo(currentTarget.tile());
            }
        }
        if(!ctx.players.local().interacting().valid() && !ctx.players.local().inMotion() && ctx.players.local().animation() == -1)
            currentTarget.interact("Attack");
        Random random = new Random();
        Condition.sleep(250 - random.nextInt(100));
    }

    private void handleCombat() // Add ability AI / functionality in future. Beta afterall :)
    {
        Random random = new Random();
        while(currentTarget.valid())
        {
            if(ctx.players.local().healthPercent() < whenToHeal && !ctx.backpack.id(foodID).isEmpty())
            {
                ctx.backpack.select();
                // Low health, but we got food :D EAT
                Item food = ctx.backpack.id(foodID).select().poll();
                food.interact("Eat");
            }
            // Make sure we are still attacking the target.
            if(!ctx.players.local().interacting().valid() && !ctx.players.local().inMotion() && ctx.players.local().animation() == -1 && !currentTarget.inCombat())
            {
                currentTarget.interact("Attack");
                Condition.sleep(250 - random.nextInt(100));
            }
        }
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

    public final Filter<GroundItem> lootFilter = new Filter<GroundItem>()
    {
        public boolean accept(GroundItem item)
        {
            if (lootIDs.contains(item.id()) && item.tile().distanceTo(originTile) <= radius) return true;
            return false;
        }
    };

    private final Filter<Npc> monsterFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            return monsterIDs.contains(npc.id()) && npc.tile().distanceTo(originTile) <= radius && npc.tile().matrix(ctx).reachable() && !npc.inCombat();
          //  player grows giant arse return true;
        }
    };

    private final Filter<Npc> refreshFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            if (npc.tile().distanceTo(originTile) <= radius) return true;
            return false;
        }
    };

    private final Filter<GameObject> cannonFilter = new Filter<GameObject>()
    {
        public boolean accept(GameObject gObject)
        {
            return (gObject.id() == normalCannonPartIDs[0] || gObject.id() == goldCannonPartIDs[0] || gObject.id() == royaleCannonPartIDs[0]) && gObject.tile().equals(cannonTile);
        }
    };

    Callable<Boolean> itemLooted = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            if(lootItem == null) return true; // null.. end wait.
            if(lootItem.valid()) return false;
            return true; // Item has been looted or disappeared or something >.>
        }
    };

    Callable<Boolean> cannonPickedUp = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            if(ctx.varpbits.varpbit(cannonVarpbit) == 0) return true;
            return false;
        }
    };

    Callable<Boolean> cannonLoaded = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            if(ctx.varpbits.varpbit(cannonVarpbit) == cannonIsFiringValue) return true;
            return false;
        }
    };

    Callable<Boolean> cannonPlaced = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            ctx.backpack.select();
            if(ctx.backpack.id(normalCannonPartIDs[3]).count() > 0 ||
               ctx.backpack.id(goldCannonPartIDs[3]).count() > 0   ||
               ctx.backpack.id(royaleCannonPartIDs[3]).count() > 0)      return false;
            else return true;
        }
    };
}