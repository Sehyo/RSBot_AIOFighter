import java.awt.*;
import java.awt.event.*;
import java.net.URLConnection;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.GroupLayout;
import org.powerbot.script.rt6.MobileIdNameQuery;
import org.powerbot.script.rt6.Npc;

/**
 * @author Alex Noble
 */
public class AIOFighterGUI extends JFrame
{
    Main main;
    String[] foodTypes = {"Shrimp","Trout", "Pike", "Salmon", "Tuna", "Lobster", "Swordfish", "Bass", "Monkfish", "Shark"};
    // Container for items for the loot part of GUI.
    ArrayList<LootGUIItem> retrievedLootItems = new ArrayList<LootGUIItem>();
    public AIOFighterGUI(Main main)
    {
        initComponents();
        this.main = main;
        nearbyMonstersModel = new DefaultListModel();
        nearbyMonsters.setModel(nearbyMonstersModel);
        selectedMonstersModel = new DefaultListModel();
        selectedMonsters.setModel(selectedMonstersModel);
        foundLootItemsModel = new DefaultListModel();
        foundLootItems.setModel(foundLootItemsModel);
        selectedLootItemsModel = new DefaultListModel();
        selectedLootItems.setModel(selectedLootItemsModel);
        whenToHeal.setMaximum(80);
        whenToHeal.setMinimum(30);

        nearbyMonsters.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() >= 2)
                {
                    int index = list.locationToIndex(evt.getPoint());
                    // Handle the double+ click
                    selectedMonstersModel.addElement(nearbyMonstersModel.getElementAt(index));
                    nearbyMonstersModel.removeElementAt(index);
                }
            }
        });

        selectedMonsters.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() >= 2)
                {
                    int index = list.locationToIndex(evt.getPoint());
                    // Handle the double+ click
                    selectedMonstersModel.removeElementAt(index);
                }
            }
        });

        foundLootItems.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() >= 2)
                {
                    int index = list.locationToIndex(evt.getPoint());
                    // Handle the double+ click
                    selectedLootItemsModel.addElement(foundLootItemsModel.getElementAt(index));
                    foundLootItemsModel.removeElementAt(index);
                }
            }
        });

        selectedLootItems.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent evt)
            {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() >= 2)
                {
                    int index = list.locationToIndex(evt.getPoint());
                    // Handle the double+ click
                    selectedLootItemsModel.removeElementAt(index);
                }
            }
        });

        for(String food : foodTypes) foodType.addItem(food);
    }

    private void setCurrentTileActionPerformed(ActionEvent e)
    {
        // Set the "origin" tile.
        main.setOriginTile();
    }

    private void applySettingsActionPerformed(ActionEvent e)
    {
        main.foodAmount = ((Integer)foodCount.getValue()).shortValue();
        main.foodID = translateFood();
        main.radius = ((Integer)fightDistance.getValue()).shortValue();
        main.whenToHeal = ((Integer)whenToHeal.getValue()).shortValue();
        if(usingCannon.isSelected()) main.isUsingCannon = true;
        // Get and Add IDs of all monsters we wanna smash
        main.monsterIDs = getIDsInJListModel(selectedMonstersModel);
        main.lootIDs = getIDsInJListModel(selectedLootItemsModel);
        main.populateList();
    }

    private ArrayList<Integer> getIDsInJListModel(ListModel model)
    {
        ArrayList<Integer> IDs = new ArrayList<Integer>();
        for(int i = 0; i < model.getSize(); i++)
        {
            String element = model.getElementAt(i).toString();
            byte commaCount = 0;
            String ID = "";
            for(int y = 0; y < element.length(); y++)
            {
                if (commaCount >= 2 && Character.isDigit(element.charAt(y))) ID += element.charAt(y);
                else if(element.charAt(y) == ',') ++commaCount;
            }
            IDs.add(Integer.parseInt(ID));
        }
        return IDs;
    }

    private short translateFood()
    {
        short foodID;
        switch(foodType.getSelectedIndex())
        { // Add info to enum later.....
            case 0:
                foodID = 315; // Shrimp
                break;
            case 1:
                foodID = 333; // Trout
                break;
            case 2:
                foodID = 351; // Pike
                break;
            case 3:
                foodID = 329; // Salmon
                break;
            case 4:
                foodID = 361; // Tuna
                break;
            case 5:
                foodID = 379; // Lobster
                break;
            case 6:
                foodID = 373; // Swordfish
                break;
            case 7:
                foodID = 365; // Bass
                break;
            case 8:
                foodID = 7946; // Monkfish
                break;
            case 9:
                foodID = 385; // Shark
                break;
            default:
                foodID = -1;
                break;
        }
        return foodID;
    }

    private void closeGUIActionPerformed(ActionEvent e)
    {
        this.setVisible(false);
    }

    /**
     * Get all nearby NPCs and add them to the nearby monsters JList.
     * @param e Action Event.
     */
    private void refreshMonstersActionPerformed(ActionEvent e)
    {
        nearbyMonsters.removeAll(); // Prepare JList.
        nearbyMonstersModel.clear();
        final MobileIdNameQuery<Npc> npcs = main.getNearbyNpcs();
        npcs.sort(NpcNameComparator);
        for(Npc npc : npcs)
        {
            final Object[] dNpc = {npc.name(), npc.combatLevel(), npc.id()};
            final Vector<Object> npcVector = new Vector<Object>(Arrays.asList(dNpc));
            // Only list if Npc doesn't already appear in Selected Npc list
            if(!selectedMonstersModel.contains(npcVector))  nearbyMonstersModel.addElement(npcVector);
        }
    }

    // Comparator
    public static Comparator<Npc> NpcNameComparator = new Comparator<Npc>()
    {
        public int compare(Npc npc0, Npc npc1)
        {
            return npc0.name().toUpperCase().compareTo(npc1.name().toUpperCase());
        }
    };

    private void searchLootItemActionPerformed(ActionEvent e)
    {
        // Clear current stuff
        foundLootItemsModel.clear();
        retrievedLootItems.clear();
        // Get string (potential substring) for the item name.
        String name = searchLootItem.getText();
        // Prepare search query, use runelocus database :) hehe
        String searchURL = "http://www.runelocus.com/tools/rs-item-id-list/?search=" + name + "&order_by=itemlist_name";
        // Retrieve html result
        String html = null;
        URLConnection connection = null;
        try
        {
            connection = new URL(searchURL).openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            html = scanner.next();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        if(html == null) return; // html retrieval wasn't successful.
        ArrayList<String> itemTableData = stringsBetweenTags("<td>", "</td>", html);
        retrievedLootItems = formatTagData(itemTableData);
        for(LootGUIItem lootItem : retrievedLootItems)
        {
            Object[] curLootItem  = {lootItem.getName(),"ID: ", lootItem.getId()};
            Vector<Object> itemVector = new Vector<Object>(Arrays.asList(curLootItem));
            if(!selectedLootItemsModel.contains(itemVector)) foundLootItemsModel.addElement(itemVector);
        }
    }

    public ArrayList<String> stringsBetweenTags(String openTag, String closingTag, String html)
    {
        ArrayList<String> tagData = new ArrayList<String>();
        ArrayList<Integer> openTagPositions = new ArrayList<Integer>();
        ArrayList<Integer> closingTagPositions = new ArrayList<Integer>();
        for(int index = html.indexOf(openTag); index >= 0; index = html.indexOf(openTag, index + 1))
            openTagPositions.add(index);
        for(int index = html.indexOf(closingTag); index >= 0; index = html.indexOf(closingTag, index + 1))
            closingTagPositions.add(index);
        // Retrieve all the tag data
        for(int i = 0; i < openTagPositions.size(); i++)
        {
            String currentTagData = "";
            for(int startIndex = openTagPositions.get(i) + openTag.length(); startIndex < closingTagPositions.get(i); startIndex++)
                currentTagData += html.charAt(startIndex);
            tagData.add(currentTagData);
        }
        return tagData;
    }

    public ArrayList<LootGUIItem> formatTagData(ArrayList<String> tagData)
    {
        ArrayList<LootGUIItem> items = new ArrayList<LootGUIItem>();
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ArrayList<String> names = new ArrayList<String>();
        // Example of what we need to get rid of: <a href="http://www.runelocus.com/item-details?item_id=7146">Broken cannon</a></td>
        // The first <td> in the html is always an id, the second is always the corresponding item name
        // However, the item name <td>'s include an <a> tag that we need to get rid of.
        boolean isID = true;
        for(int i = 0; i < tagData.size(); i++)
        {
            if(isID)
            {
                isID = !isID;
                ids.add(Integer.parseInt(tagData.get(i)));
            }
            else
            {
                isID = !isID;
                // We have a name entry! Get rid of the annoying <a> part
                int startIndex = tagData.get(i).charAt(tagData.get(i).indexOf(">"));
                String name = "";
                while(tagData.get(i).charAt(startIndex) != '<')
                {
                    name += tagData.get(i).charAt(startIndex);
                    ++startIndex;
                }
                names.add(name);
            }
        }
        // Put data into items
        for(int i = 0; i < ids.size(); i++)
            items.add(new LootGUIItem(ids.get(i), names.get(i)));
        return items;
    }


    private void initComponents() {
        tabbedPane1 = new JTabbedPane();
        panel1 = new JPanel();
        label5 = new JLabel();
        foodCount = new JSpinner(new SpinnerNumberModel(10,0,28,1));
        foodType = new JComboBox();
        whenToHeal = new JSlider();
        label6 = new JLabel();
        label7 = new JLabel();
        label8 = new JLabel();
        textField1 = new JTextField();
        usingCannon = new JCheckBox();
        label9 = new JLabel();
        fightDistance = new JSpinner(new SpinnerNumberModel(10,1,25,1));
        label10 = new JLabel();
        currentTile = new JLabel();
        setCurrentTile = new JButton();
        panel2 = new JPanel();
        label11 = new JLabel();
        refreshMonsters = new JButton();
        scrollPane2 = new JScrollPane();
        nearbyMonsters = new JList();
        label12 = new JLabel();
        scrollPane3 = new JScrollPane();
        selectedMonsters = new JList();
        panel3 = new JPanel();
        label13 = new JLabel();
        scrollPane4 = new JScrollPane();
        foundLootItems = new JList();
        searchLootItem = new JTextField();
        scrollPane5 = new JScrollPane();
        selectedLootItems = new JList();
        label14 = new JLabel();
        applySettings = new JButton();
        closeGUI = new JButton();

        //======== this ========
        Container contentPane = getContentPane();

        //======== tabbedPane1 ========
        {

            //======== panel1 ========
            {

                //---- label5 ----
                label5.setText("Food Type:");

                //---- label6 ----
                label6.setText("Heal At:");

                //---- label7 ----
                label7.setText("30%");

                //---- label8 ----
                label8.setText("80%");

                //---- textField1 ----
                textField1.setText("Thank you for using my AIO Fighter (Beta)");

                //---- usingCannon ----
                usingCannon.setText("Use Dwarf MultiCannon?");

                //---- label9 ----
                label9.setText("Fight Distance:");

                //---- label10 ----
                label10.setText("tiles around tile");

                //---- currentTile ----
                currentTile.setText("(tile not set)");

                //---- setCurrentTile ----
                setCurrentTile.setText("Set Current Tile");
                setCurrentTile.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setCurrentTileActionPerformed(e);
                    }
                });

                GroupLayout panel1Layout = new GroupLayout(panel1);
                panel1.setLayout(panel1Layout);
                panel1Layout.setHorizontalGroup(
                        panel1Layout.createParallelGroup()
                                .addComponent(textField1)
                                .addGroup(panel1Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(panel1Layout.createParallelGroup()
                                                .addComponent(usingCannon)
                                                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addGroup(panel1Layout.createSequentialGroup()
                                                                .addComponent(label7)
                                                                .addGap(258, 258, 258)
                                                                .addComponent(label8))
                                                        .addGroup(panel1Layout.createParallelGroup()
                                                                .addGroup(panel1Layout.createSequentialGroup()
                                                                        .addComponent(label5)
                                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(foodCount, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(foodType, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                                                                .addComponent(label6, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(whenToHeal, GroupLayout.PREFERRED_SIZE, 304, GroupLayout.PREFERRED_SIZE)))
                                                .addGroup(panel1Layout.createSequentialGroup()
                                                        .addComponent(label9)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(fightDistance, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(panel1Layout.createParallelGroup()
                                                                .addComponent(setCurrentTile)
                                                                .addGroup(panel1Layout.createSequentialGroup()
                                                                        .addComponent(label10)
                                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(currentTile)))))
                                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                panel1Layout.setVerticalGroup(
                        panel1Layout.createParallelGroup()
                                .addGroup(panel1Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(label5)
                                                .addComponent(foodCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(foodType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(label6)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(whenToHeal, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(label7)
                                                .addComponent(label8))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(usingCannon)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(panel1Layout.createParallelGroup()
                                                .addComponent(label9)
                                                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(fightDistance, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(label10)
                                                        .addComponent(currentTile)))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                                        .addComponent(setCurrentTile)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(textField1, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("General", panel1);

            //======== panel2 ========
            {

                //---- label11 ----
                label11.setText("Nearby Monsters");

                //---- refreshMonsters ----
                refreshMonsters.setText("Refresh");
                refreshMonsters.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refreshMonstersActionPerformed(e);
                    }
                });

                //======== scrollPane2 ========
                {
                    scrollPane2.setViewportView(nearbyMonsters);
                }

                //---- label12 ----
                label12.setText("Monster List");

                //======== scrollPane3 ========
                {
                    scrollPane3.setViewportView(selectedMonsters);
                }

                GroupLayout panel2Layout = new GroupLayout(panel2);
                panel2.setLayout(panel2Layout);
                panel2Layout.setHorizontalGroup(
                        panel2Layout.createParallelGroup()
                                .addGroup(panel2Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(panel2Layout.createParallelGroup()
                                                .addGroup(panel2Layout.createSequentialGroup()
                                                        .addComponent(label11)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(refreshMonsters)
                                                        .addGap(72, 72, 72))
                                                .addGroup(panel2Layout.createSequentialGroup()
                                                        .addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 157, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(panel2Layout.createParallelGroup()
                                                                .addComponent(scrollPane3, GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                                                                .addGroup(panel2Layout.createSequentialGroup()
                                                                        .addComponent(label12)
                                                                        .addGap(0, 83, Short.MAX_VALUE)))
                                                        .addContainerGap())))
                );
                panel2Layout.setVerticalGroup(
                        panel2Layout.createParallelGroup()
                                .addGroup(panel2Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(label11)
                                                .addComponent(refreshMonsters))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panel2Layout.createParallelGroup()
                                                .addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                                .addGroup(GroupLayout.Alignment.TRAILING, panel2Layout.createSequentialGroup()
                                                        .addComponent(label12)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(scrollPane3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                        .addContainerGap(44, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("Monster", panel2);

            //======== panel3 ========
            {

                //---- label13 ----
                label13.setText("Search Item");

                //======== scrollPane4 ========
                {
                    scrollPane4.setViewportView(foundLootItems);
                }

                //---- searchLootItem ----
                searchLootItem.setText("Item Name");
                searchLootItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        searchLootItemActionPerformed(e);
                    }
                });

                //======== scrollPane5 ========
                {
                    scrollPane5.setViewportView(selectedLootItems);
                }

                //---- label14 ----
                label14.setText("Items to Loot");

                GroupLayout panel3Layout = new GroupLayout(panel3);
                panel3.setLayout(panel3Layout);
                panel3Layout.setHorizontalGroup(
                        panel3Layout.createParallelGroup()
                                .addGroup(panel3Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(scrollPane4)
                                                .addGroup(panel3Layout.createSequentialGroup()
                                                        .addComponent(label13)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(searchLootItem, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                .addComponent(scrollPane5, GroupLayout.PREFERRED_SIZE, 142, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(label14))
                                        .addContainerGap(20, Short.MAX_VALUE))
                );
                panel3Layout.setVerticalGroup(
                        panel3Layout.createParallelGroup()
                                .addGroup(panel3Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(label13)
                                                .addComponent(searchLootItem, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(label14))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addComponent(scrollPane5, GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                                                .addComponent(scrollPane4, GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE))
                                        .addContainerGap(62, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("Looting", panel3);
        }

        //---- applySettings ----
        applySettings.setText("Apply New Settings");
        applySettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applySettingsActionPerformed(e);
            }
        });

        //---- closeGUI ----
        closeGUI.setText("Close GUI");
        closeGUI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeGUIActionPerformed(e);
            }
        });

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addComponent(tabbedPane1)
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGap(0, 109, Short.MAX_VALUE)
                                .addComponent(applySettings)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(closeGUI)
                                .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addComponent(tabbedPane1, GroupLayout.PREFERRED_SIZE, 332, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(closeGUI)
                                        .addComponent(applySettings))
                                .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
    }

    private JTabbedPane tabbedPane1;
    private JPanel panel1;
    private JLabel label5;
    private JSpinner foodCount;
    private JComboBox foodType;
    private JSlider whenToHeal;
    private JLabel label6;
    private JLabel label7;
    private JLabel label8;
    private JTextField textField1;
    private JCheckBox usingCannon;
    private JLabel label9;
    private JSpinner fightDistance;
    private JLabel label10;
    private JLabel currentTile;
    private JButton setCurrentTile;
    private JPanel panel2;
    private JLabel label11;
    private JButton refreshMonsters;
    private JScrollPane scrollPane2;
    private JList nearbyMonsters;
    private DefaultListModel nearbyMonstersModel;
    private JLabel label12;
    private JScrollPane scrollPane3;
    private JList selectedMonsters;
    private DefaultListModel selectedMonstersModel;
    private JPanel panel3;
    private JLabel label13;
    private JScrollPane scrollPane4;
    private JList foundLootItems;
    private DefaultListModel foundLootItemsModel;
    private JTextField searchLootItem;
    private JScrollPane scrollPane5;
    private JList selectedLootItems;
    private DefaultListModel selectedLootItemsModel;
    private JLabel label14;
    private JButton applySettings;
    private JButton closeGUI;
}
