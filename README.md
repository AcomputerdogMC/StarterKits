# StarterKits - Bukkit plugin to give a starter kit to new players

StarterKits is a simple plugin to provide free starter items to new players when they join a server.  Kit gifting can be controlled with the permission "starterkits.receive".

**Configuration (see /res/default.yml for more info):**   
Kits are defined by a list of items to give.  Each item is a pair of "name" and "count" tags that do what they sound like.  The normal structure of the configuration file is as follows:

    kit:
      - name: WOOD_PICKAXE //give 1 wood pickaxe
        count: 1
      - name: APPLE        //give 5 apples
        count: 5
      - name: ENDER_CHEST  //give 1 ender chest
        count: 1
      - name: BED          //give 1 bed
        count: 1
      - name: COMPASS      //give 1 compass
        count: 1
