import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Tile;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.GroundItem;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by Alex Noble on 04/12/2014.
 */
public class TaskLoot extends Task<ClientContext>
{
    ArrayList<Integer> lootIDs;
    GroundItem lootItem;
    Tile originTile;
    short radius;

    public TaskLoot(ClientContext ctx, ArrayList<Integer> lootIDs, Tile originTile, short radius)
    {
        super(ctx);
        this.lootIDs = lootIDs;
        this.originTile = originTile;
        this.radius = radius;
    }

    public boolean activate()
    {
        return lootAvailable() && !ctx.players.local().inMotion() && !ctx.players.local().interacting().valid();
    }

    public void execute()
    {
        while(lootAvailable())
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

    private boolean lootAvailable()
    {
        ctx.groundItems.select();
        if(ctx.groundItems.select(lootFilter).isEmpty()) return false;
        return true;
    }

    public final Filter<GroundItem> lootFilter = new Filter<GroundItem>()
    {
        public boolean accept(GroundItem item)
        {
            if (lootIDs.contains(item.id()) && item.tile().distanceTo(originTile) <= radius && item.tile().matrix(ctx).reachable()) return true;
            return false;
        }
    };

    private final Callable<Boolean> itemLooted = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            if(lootItem == null) return true; // null.. end wait.
            if(lootItem.valid()) return false;
            return true; // Item has been looted or disappeared or something >.>
        }
    };
}