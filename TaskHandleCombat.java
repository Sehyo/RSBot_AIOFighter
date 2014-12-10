import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.Item;
import org.powerbot.script.rt6.Npc;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by Alex Noble on 04/12/2014.
 */

public class TaskHandleCombat extends Task<ClientContext>
{
    short whenToHeal;
    int foodID;
    Item food;
    public TaskHandleCombat(ClientContext ctx, short whenToHeal, int foodID)
    {
        super(ctx);
        this.whenToHeal = whenToHeal;
        this.foodID = foodID;
    }

    public boolean activate()
    {
        if(ctx.players.local().interacting().valid() || ctx.npcs.select(aggressiveMonsterFilter).nearest().poll().valid())
            return true; // We seem to be interacting with an NPC or an NPC seems to be attackign us.
        return false;
    }

    public void execute()
    {
        Random random = new Random(); // Prepare random variable for our sleep.
        ctx.npcs.select();
        ctx.backpack.select();
        Npc currentTarget = ctx.npcs.select(currentTargetFilter).nearest().poll();
        if(!currentTarget.valid()) currentTarget = ctx.npcs.select(aggressiveMonsterFilter).nearest().poll();
        if(getHealth() < whenToHeal && ctx.backpack.id(foodID).count() != 0)
        {
            // Low health, but we got food :D EAT
            do
            {
                food = ctx.backpack.select().id(foodID).poll();
                food.interact("Eat");
                Condition.wait(foodEaten);
            }while(getHealth() < whenToHeal);

            Condition.wait(foodEaten, 400 - random.nextInt(200), 10 - random.nextInt(4));
        }
        // Make sure we are still attacking the target.
        if(!ctx.players.local().interacting().valid())
        {
            currentTarget.interact("Attack");
            Condition.sleep(250 - random.nextInt(100));
        }
    }

    public double getHealth()
    {
        return (ctx.combatBar.health() / (double)ctx.combatBar.maximumHealth()) * 100;
    }

    private final Filter<Npc> aggressiveMonsterFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            if(npc.interacting().equals(ctx.players.local())) return true;
            return false;
        }
    };

    private final Filter<Npc> currentTargetFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            if(ctx.players.local().interacting().equals(npc)) return true;
            return false;
        }
    };

    private final Callable<Boolean> foodEaten = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            return !food.valid();
        }
    };
}