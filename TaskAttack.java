import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Tile;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.Npc;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by Alex Noble on 04/12/2014.
 */
public class TaskAttack extends Task<ClientContext>
{
    ArrayList<Integer> monsterIDs;
    Tile originTile;
    short radius;
    Npc currentTarget;

    public TaskAttack(ClientContext ctx, ArrayList<Integer> monsterIDs, Tile originTile, short radius)
    {
        super(ctx);
        this.monsterIDs = monsterIDs;
        this.originTile = originTile;
        this.radius = radius;
    }

    public boolean activate()
    {
        if(hasTarget() || ctx.players.local().inMotion()) return false; // We already have a target. No need to acquire a new one.
        return true;
    }

    public void execute()
    {
        ctx.npcs.select();
        if(ctx.npcs.select(monsterFilter).isEmpty()) return; // No monsters :(
        currentTarget = ctx.npcs.select(monsterFilter).nearest().limit(3).shuffle().poll(); // Anti pattern
        while(!currentTarget.tile().matrix(ctx).inViewport() && currentTarget.valid()) // Get target in view port.
        {
            if(!ctx.players.local().inMotion())
            {
                // Move towards npc.
                ctx.movement.step(currentTarget.tile());
                ctx.camera.turnTo(currentTarget.tile());
            }
        }
        currentTarget.interact("Attack");
        Random random = new Random();
        Condition.sleep(250 - random.nextInt(100));
    }

    private final Filter<Npc> aggressiveMonsterFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            if(npc.interacting().equals(ctx.players.local())) return true;
            return false;
        }
    };

    private boolean hasTarget()
    {
        return ctx.players.local().interacting().valid() || ctx.npcs.select(aggressiveMonsterFilter).nearest().poll().valid();
    }

    private final Filter<Npc> monsterFilter = new Filter<Npc>()
    {
        public boolean accept(Npc npc)
        {
            return monsterIDs.contains(npc.id()) && npc.tile().distanceTo(originTile) <= radius && npc.tile().matrix(ctx).reachable() && !npc.inCombat();
            //  player grows giant arse return true;
        }
    };
}