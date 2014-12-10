import org.powerbot.script.Condition;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.Item;

import java.util.concurrent.Callable;

/**
 * Created by Alex Noble on 04/12/2014.
 */
public class TaskBuryBones extends Task<ClientContext>
{
    private boolean buryBones;
    private Item bone;
    // Shit long list of bone ids >.>
    private final int[] boneIDs = {526,
            527,
            528,
            529,
            530,
            531,
            532,
            533,
            534,
            535,
            536,
            537,
            2391,
            2392,
            2530,
            2531,
            2859,
            2860,
            3123,
            3124,
            3125,
            3126,
            3179,
            3180,
            3181,
            3182,
            3183,
            3184,
            3185,
            3186,
            3187,
            4812,
            4813,
            4830,
            4831,
            4832,
            4833,
            4834,
            4835,
            6729,
            6730,
            6812,
            6816,
            6904,
            6905,
            6906,
            6907,
            11338,
            14638,
            14693,
            14793,
            14794,
            15410,
            17670,
            17671,
            17672,
            17673,
            17674,
            17675,
            17676,
            17677,
            18830,
            18831,
            18832,
            18833};

    public TaskBuryBones(ClientContext ctx, boolean buryBones)
    {
        super(ctx);
        this.buryBones = buryBones;
    }

    public boolean activate()
    {
        return buryBones && !ctx.backpack.select().id(boneIDs).isEmpty();
    }

    public void execute()
    {
        do
        {
            bone = ctx.backpack.select().id(boneIDs).poll();
            bone.interact("Bury");
            Condition.wait(boneBuried);
        }while(!ctx.backpack.select().id(boneIDs).isEmpty());
    }

    private final Callable<Boolean> boneBuried = new Callable<Boolean>()
    {
        public Boolean call() throws Exception
        {
            return !bone.valid();
        }
    };
}