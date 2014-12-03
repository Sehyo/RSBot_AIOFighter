/**
 * Created by Alex Noble on 02/12/2014.
 */
public class LootGUIItem
{
    private int id;
    private String name;

    public LootGUIItem(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }
}
