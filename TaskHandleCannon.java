import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Tile;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.GameObject;
import org.powerbot.script.rt6.Item;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by Alex Noble on 04/12/2014.
 */
public class TaskHandleCannon extends Task<ClientContext>
{
    // Varpbits
    private final int cannonVarpbit = 2733;
    private final int outOfCannonAmmoValue = 2048;
    private final int cannonIsFiringValue = 1050624;
    // IDs
    private final int[] normalCannonPartIDs = {6,8,10,12};
    private final int[] goldCannonPartIDs = {20494, 20495, 20496, 20497};
    private final int[] royaleCannonPartIDs = {20498, 20499, 20500, 20501};
    // Misc Info about Cannon
    private Tile cannonTile;
    private Boolean cannonSetup = false;
    private Boolean stopUsingCannon = false;

    public TaskHandleCannon(ClientContext ctx)
    {
        super(ctx);
    }

    public boolean activate()
    {
        if(stopUsingCannon) return false;
        return true;
    }

    public void execute()
    {
        setupCannon();
        loadCannon();
        pickupCannon();
    }

    private void setupCannon()
    {
        if(cannonSetup) return; // Cannon already out >.<
        if(!backpackContainsCannon()) return; // We dont even have a cannon >.>
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
    }

    private void loadCannon()
    {
        ctx.backpack.select();
        if(!(ctx.varpbits.varpbit(cannonVarpbit) == outOfCannonAmmoValue)) return; //  Cannon not in "out of ammo" state.
        if(ctx.varpbits.varpbit(cannonVarpbit) == 0 || ctx.backpack.id(2).count() == 0) // True if no cannon out or no ammo left in backpack.
        {
            stopUsingCannon = true; // No cannon anymore.
            return;
        }
        // Looks like we have a cannon and ammo.
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
        if(ctx.backpack.id(2).count() != 0) return; // We have ammo, not time to pick up yet.
        if(ctx.varpbits.varpbit(cannonVarpbit) == 0) return; // We don't have a cannon up, no need to pick up.
        // Pick up.
        ctx.objects.select();
        final GameObject cannon = ctx.objects.select(cannonFilter).poll();
        getTileInViewport(cannon.tile());
        Random random = new Random();
        Condition.wait(cannonPickedUp, 400 - random.nextInt(200), 30 - random.nextInt(4)); // Sleep for maximum of 400, minimum of 200. 30 to 26 times.
    }

    private Item getCannonBase()
    {
        if((ctx.backpack.select().id(normalCannonPartIDs[0]).count() > 0))    return ctx.backpack.select().id(normalCannonPartIDs[0]).poll();
        else if((ctx.backpack.select().id(goldCannonPartIDs[0]).count() > 0))    return ctx.backpack.select().id(goldCannonPartIDs[0]).poll();
        else if((ctx.backpack.select().id(royaleCannonPartIDs[0]).count() > 0))    return ctx.backpack.select().id(royaleCannonPartIDs[0]).poll();
        return null;
    }

    private boolean backpackContainsCannon() // Clunkiest boolean ever
    {
        return          (ctx.backpack.select().id(normalCannonPartIDs[0]).count() > 0 &&
                        ctx.backpack.select().id(normalCannonPartIDs[1]).count() > 0 &&
                        ctx.backpack.select().id(normalCannonPartIDs[2]).count() > 0 &&
                        ctx.backpack.select().id(normalCannonPartIDs[3]).count() > 0) ||
                        (ctx.backpack.select().id(goldCannonPartIDs[0]).count() > 0 &&
                        ctx.backpack.select().id(goldCannonPartIDs[1]).count() > 0 &&
                        ctx.backpack.select().id(goldCannonPartIDs[2]).count() > 0 &&
                        ctx.backpack.select().id(goldCannonPartIDs[3]).count() > 0) ||
                        (ctx.backpack.select().id(royaleCannonPartIDs[0]).count() > 0 &&
                        ctx.backpack.select().id(royaleCannonPartIDs[1]).count() > 0 &&
                        ctx.backpack.select().id(royaleCannonPartIDs[2]).count() > 0 &&
                        ctx.backpack.select().id(royaleCannonPartIDs[3]).count() > 0);
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

    private final Filter<GameObject> cannonFilter = new Filter<GameObject>()
    {
        public boolean accept(GameObject gObject)
        {
            return (gObject.id() == normalCannonPartIDs[0] || gObject.id() == goldCannonPartIDs[0] || gObject.id() == royaleCannonPartIDs[0]) && gObject.tile().equals(cannonTile);
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