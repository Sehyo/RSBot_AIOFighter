import org.powerbot.script.ClientAccessor;
import org.powerbot.script.ClientContext;

/**
 * Created by Alex Noble on 04/12/2014.
 */
public abstract class Task <C extends ClientContext> extends ClientAccessor<C>
{
    public abstract boolean activate();
    public abstract void execute();

    public Task(C ctx)
    {
        super(ctx);
    }
}
