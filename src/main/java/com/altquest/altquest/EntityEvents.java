package com.altquest.altquest;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.permissions.BroadcastPermissions;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by explodi on 11/7/15.
 */
public class EntityEvents implements Listener {
    AltQuest altQuest;
    StringBuilder rawwelcome = new StringBuilder();

    private static final List<Material> PROTECTED_BLOCKS = Arrays.asList(Material.CHEST, Material.ACACIA_DOOR, Material.BIRCH_DOOR,Material.DARK_OAK_DOOR,
            Material.JUNGLE_DOOR, Material.SPRUCE_DOOR, Material.WOOD_DOOR, Material.WOODEN_DOOR,
            Material.FURNACE, Material.BURNING_FURNACE, Material.ACACIA_FENCE_GATE, Material.BIRCH_FENCE_GATE,
            Material.DARK_OAK_FENCE_GATE, Material.FENCE_GATE, Material.JUNGLE_FENCE_GATE,
            Material.SPRUCE_FENCE_GATE, Material.DISPENSER, Material.DROPPER, Material.HOPPER);
    
    public EntityEvents(AltQuest plugin) {
        altQuest = plugin;


        
        for (String line : altQuest.getConfig().getStringList("welcomeMessage")) {
            for (ChatColor color : ChatColor.values()) {
                line = line.replaceAll("<" + color.name() + ">", color.toString());
            }
            // add links
            final Pattern pattern = Pattern.compile("<link>(.+?)</link>");
            final Matcher matcher = pattern.matcher(line);
            matcher.find();
            String link = matcher.group(1);
            // Right here we need to replace the link variable with a minecraft-compatible link
            line = line.replaceAll("<link>" + link + "<link>", link);

            rawwelcome.append(line);
        }
    }

    
    @EventHandler
    public void onExperienceChange(PlayerExpChangeEvent event) throws ParseException, org.json.simple.parser.ParseException, IOException {    
        event.setAmount(0);
    }
    
    @EventHandler
    public void onEnchantItemEvent(EnchantItemEvent event) throws ParseException, org.json.simple.parser.ParseException, IOException {
        // Simply setting the cost to zero does not work. there are probably
        // checks downstream for this. Instead cancel out the cost.
        // None of this actually changes the bitquest xp anyway, so just make
        // things look correct for the user. This only works for the enchantment table,
        // not the anvil.
        event.getEnchanter().setLevel(event.getEnchanter().getLevel() + event.whichButton() + 1);
        
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws IOException, org.json.simple.parser.ParseException, ParseException, JSONException {
        final Player player=event.getPlayer();
        if(AltQuest.ADMIN_UUID!=null && player.getUniqueId().toString().equals(AltQuest.ADMIN_UUID.toString())) {
            player.setOp(true);
        } else {
            player.setOp(false);
        }
        player.setGameMode(GameMode.SURVIVAL);
        final User user = new User(player);

        altQuest.updateScoreboard(player);
        user.setTotalExperience(user.experience());
        final String ip=player.getAddress().toString().split("/")[1].split(":")[0];
        System.out.println("User "+player.getName()+"logged in with IP "+ip);
        AltQuest.REDIS.set("ip"+player.getUniqueId().toString(),ip);
        AltQuest.REDIS.set("displayname:"+player.getUniqueId().toString(),player.getDisplayName());
        AltQuest.REDIS.set("uuid:"+player.getDisplayName().toString(),player.getUniqueId().toString());

        if (altQuest.isModerator(player)) {
            if (altQuest.ALTQUEST_ENV.equals("development")==true) {
                player.setOp(true);
            }
            player.sendMessage(ChatColor.YELLOW + "You are a moderator on this server.");
            player.sendMessage(ChatColor.YELLOW + "The world BTC wallet balance is: " + altQuest.wallet.balance() / 100 + " bits");
            player.sendMessage(ChatColor.BLUE + "" + ChatColor.UNDERLINE + "blockchain.info/address/" + altQuest.wallet.address);
player.sendMessage(ChatColor.YELLOW + "The world DOGE wallet balance is: " + altQuest.DOGE_wallet.balance() + " DOGE");
            player.sendMessage(ChatColor.BLUE + "" + ChatColor.UNDERLINE + "dogechain.info/address/" + altQuest.DOGE_wallet.address);
player.sendMessage(ChatColor.YELLOW + "The world LTC wallet balance is: " + altQuest.LTC_wallet.balance()  + " LTC");
            player.sendMessage(ChatColor.BLUE + "" + ChatColor.UNDERLINE + "http://ltc.blockr.io/address/info/" + altQuest.LTC_wallet.address);
        }
        
        String welcome = rawwelcome.toString();
        welcome = welcome.replace("<name>", player.getName());
        player.sendMessage(welcome);
        if(AltQuest.REDIS.exists("clan:"+player.getUniqueId().toString())) {
            String clan=AltQuest.REDIS.get("clan:"+player.getUniqueId().toString());
            System.out.println(clan);
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard board = manager.getNewScoreboard();
            player.setDisplayName("["+clan+"] "+player.getDisplayName());
        }

        // Prints the user balance

        try {

        	// check and set experience
        	user.setTotalExperience((Integer) user.experience());
        	altQuest.updateScoreboard(player);


        	altQuest.sendWalletInfo(user);

        	player.sendMessage("");
        	player.sendMessage(ChatColor.YELLOW + "Don't forget to visit the AltQuest Wiki");
        	player.sendMessage(ChatColor.YELLOW + "There's tons of useful stuff there!");
        	player.sendMessage("");
        	player.sendMessage(ChatColor.BLUE + "     " + ChatColor.UNDERLINE + "http://bit.ly/wikibq");
        	player.sendMessage("");
        } catch (ParseException e) {
        	e.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }

        if(altQuest.messageBuilder != null) {
        	final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

            scheduler.runTaskAsynchronously(altQuest, new Runnable() {
                @Override
                public void run() {
                    org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Login", null);
                    org.json.JSONObject props = new org.json.JSONObject();
                    try {
                        props.put("$name", player.getName());
                        props.put("$ip", ip);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    org.json.JSONObject update = altQuest.messageBuilder.set(player.getUniqueId().toString(), props);


                    ClientDelivery delivery = new ClientDelivery();
                    delivery.addMessage(sentEvent);
                    delivery.addMessage(update);

                    MixpanelAPI mixpanel = new MixpanelAPI();
                    try {
                        mixpanel.deliver(delivery);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

            });


        }


    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) throws ParseException, org.json.simple.parser.ParseException, IOException {
        if(AltQuest.REDIS.exists("STARTUP")==true&&altQuest.isModerator(event.getPlayer())==false) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Can't join right now. Come back later");
        }
        Player player=event.getPlayer();
        AltQuest.REDIS.set("displayname:"+player.getUniqueId().toString(),player.getDisplayName());
        AltQuest.REDIS.set("uuid:"+player.getDisplayName().toString(),player.getUniqueId().toString());
	   

	if(!AltQuest.REDIS.sismember("banlist",event.getPlayer().getUniqueId().toString())) {

            User user = new User(event.getPlayer());

        } else {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Can't join right now. Come back later");
        }
	//if BTC address does not exist...
        if(!AltQuest.REDIS.exists("address"+player.getUniqueId().toString())&&!AltQuest.REDIS.exists("private"+player.getUniqueId().toString())) {
            System.out.println("Generating new address...");
            URL url = new URL("https://api.blockcypher.com/v1/"+AltQuest.BLOCKCHAIN+"/addrs");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj = (JSONObject) parser.parse(response.toString());
            System.out.println(response.toString());
            AltQuest.REDIS.set("private"+player.getUniqueId().toString(), (String) jsonobj.get("private"));
            AltQuest.REDIS.set("public"+player.getUniqueId().toString(), (String) jsonobj.get("public"));
            AltQuest.REDIS.set("address"+player.getUniqueId().toString(), (String) jsonobj.get("address"));
        }
	//if LTC address does not exist...
if(!AltQuest.REDIS.exists("LTC_address"+player.getUniqueId().toString())&&!AltQuest.REDIS.exists("LTC_private"+player.getUniqueId().toString())) {
            System.out.println("Generating new LTC address...");
            URL url = new URL("https://api.blockcypher.com/v1/ltc/main/addrs");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj = (JSONObject) parser.parse(response.toString());
            System.out.println(response.toString());
            AltQuest.REDIS.set("LTC_private"+player.getUniqueId().toString(), (String) jsonobj.get("private"));
            AltQuest.REDIS.set("LTC_public"+player.getUniqueId().toString(), (String) jsonobj.get("public"));
            AltQuest.REDIS.set("LTC_address"+player.getUniqueId().toString(), (String) jsonobj.get("address"));
        }//end LTC
	//if DOGE address does not exist...
if(!AltQuest.REDIS.exists("DOGE_address"+player.getUniqueId().toString())&&!AltQuest.REDIS.exists("DOGE_private"+player.getUniqueId().toString())) {
            System.out.println("Generating new DOGE address...");
            URL url = new URL("https://api.blockcypher.com/v1/doge/main/addrs");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj = (JSONObject) parser.parse(response.toString());
            System.out.println(response.toString());
            AltQuest.REDIS.set("DOGE_private"+player.getUniqueId().toString(), (String) jsonobj.get("private"));
            AltQuest.REDIS.set("DOGE_public"+player.getUniqueId().toString(), (String) jsonobj.get("public"));
            AltQuest.REDIS.set("DOGE_address"+player.getUniqueId().toString(), (String) jsonobj.get("address"));
        }//end LTC
        if ((AltQuest.REDIS.get("currency"+player.getUniqueId().toString()))== null){
	AltQuest.REDIS.set("currency"+player.getUniqueId().toString(), "BTC");}
	if(AltQuest.REDIS.get("private"+event.getPlayer().getUniqueId().toString())==null||AltQuest.REDIS.get("address"+event.getPlayer().getUniqueId().toString())==null) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,"There was a problem loading your Bitcoin wallet. Try Again Later. If this problem persists, please write to altquest09@gmail.com");
        }


    }

    @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) throws ParseException, org.json.simple.parser.ParseException, IOException {
        if(event.getFrom().getChunk()!=event.getTo().getChunk()) {
            altQuest.updateScoreboard(event.getPlayer());
            if(!event.getFrom().getWorld().getName().endsWith("_nether") && !event.getFrom().getWorld().getName().endsWith("_end")) {
                // announce new area
                int x1=event.getFrom().getChunk().getX();
                int z1=event.getFrom().getChunk().getZ();

                int x2=event.getTo().getChunk().getX();
                int z2=event.getTo().getChunk().getZ();

                String name1=AltQuest.REDIS.get("chunk"+x1+","+z1+"name")!= null ? AltQuest.REDIS.get("chunk"+x1+","+z1+"name") : "the wilderness";
                String name2=AltQuest.REDIS.get("chunk"+x2+","+z2+"name")!= null ? AltQuest.REDIS.get("chunk"+x2+","+z2+"name") : "the wilderness";

                if(name1==null) name1="the wilderness";
                if(name2==null) name2="the wilderness";
		
		if(altQuest.isPvP(event.getPlayer().getLocation())==true) { 
			event.getPlayer().sendMessage(ChatColor.RED+"IN PVP ZONE");
			}
                if(!name1.equals(name2)) {
                    if(name2.equals("the wilderness")){
                        event.getPlayer().sendMessage(ChatColor.GRAY+"[ "+name2+" ]");
                    }else{
                        event.getPlayer().sendMessage(ChatColor.YELLOW+"[ "+name2+" ]");
                    }
                }
            }
        }


    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getItem() != null) {
            final Player player=event.getPlayer();
                if (event.getItem().getType() == Material.EYE_OF_ENDER) {
                    if (!player.hasMetadata("teleporting")) {
                        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            if (player.getBedSpawnLocation() != null) {
                                // TODO: tp player home
                                player.sendMessage(ChatColor.GREEN + "Teleporting to your bed...");
                                player.setMetadata("teleporting", new FixedMetadataValue(altQuest, true));
                                World world = Bukkit.getWorld("world");

                                final Location spawn = player.getBedSpawnLocation();

                                Chunk c = spawn.getChunk();
                                if (!c.isLoaded()) {
                                    c.load();
                                }
                                altQuest.getServer().getScheduler().scheduleSyncDelayedTask(altQuest, new Runnable() {

                                    public void run() {
                                        player.teleport(spawn);
                                        player.removeMetadata("teleporting", altQuest);
                                    }
                                }, 60L);
                            } else {
                                player.sendMessage(ChatColor.RED + "You must sleep in a bed before using the ender eye teleport");
                            }


                        }
                    }
                    event.setCancelled(true);
                }
                if (!player.hasMetadata("teleporting") && event.getItem().getType() == Material.COMPASS) {

                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    // TODO: open the tps inventory
                    player.sendMessage(ChatColor.GREEN+"Teleporting to Bitcoin Central...");
                    player.setMetadata("teleporting", new FixedMetadataValue(altQuest, true));
                    World world=Bukkit.getWorld("world");

                    final Location spawn=world.getHighestBlockAt(world.getSpawnLocation()).getLocation();

                    Chunk c = spawn.getChunk();
                    if (!c.isLoaded()) {
                        c.load();
                    }
                    altQuest.getServer().getScheduler().scheduleSyncDelayedTask(altQuest, new Runnable() {

                        public void run() {
                            player.teleport(spawn);
                            player.removeMetadata("teleporting", altQuest);
                        }
                    }, 60L);

                }
            }
        }

    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            int maxHealth = (int) ((LivingEntity) event.getEntity()).getMaxHealth() * 2;
            int health = (int) (((LivingEntity) event.getEntity()).getHealth() - event.getDamage()) * 2;
            String name = event.getEntity().getName();
            // TODO: Show damage message
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDeathMessage(null);
        String spawnkey=spawnKey(event.getEntity().getLocation());
        AltQuest.REDIS.expire(spawnkey,30);
    }
    @EventHandler

    void onEntityDeath(EntityDeathEvent e) throws IOException, ParseException, org.json.simple.parser.ParseException {
        final LivingEntity entity = e.getEntity();

        int level = new Double(entity.getMaxHealth() / 4).intValue();

        if (entity instanceof Monster) {
            final String spawnkey = spawnKey(entity.getLocation());

            AltQuest.REDIS.expire(spawnkey,30000);
            System.out.println("[death] "+spawnkey+", "+level);
            if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                final EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent) e.getEntity().getLastDamageCause();
                if (damage.getDamager() instanceof Player && level >= 1) {
                    final Player player = (Player) damage.getDamager();
                    final User user = new User(player);
                    final int money = 20000;
                    final int d128 = AltQuest.rand(1, level);
                    final int whatLoot = AltQuest.rand(1, 10000);
                    System.out.println("lastloot: "+AltQuest.REDIS.get("lastloot"));
			//start if btc
			if (whatLoot>=9250){                    
			altQuest.wallet.updateBalance();
                    if(altQuest.wallet.balance>money) {


                        final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                        final Wallet userWallet=user.wallet;
                        AltQuest.REDIS.expire("balance"+player.getUniqueId().toString(),5);

                        scheduler.runTaskAsynchronously(altQuest, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (altQuest.wallet.transaction(money, userWallet)) {
                                        System.out.println("[loot]"+whatLoot+" "+player.getDisplayName()+": Bitcoin "+money);
                                        player.sendMessage(ChatColor.GREEN + "You got " + ChatColor.BOLD + money / 100 + ChatColor.GREEN + " bits of loot!");
                                        // player.playSound(player.getLocation(), Sound.LEVEL_UP, 20, 1);
                                        if (altQuest.messageBuilder != null) {

                                            // Create an event
                                            org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Loot", null);


                                            ClientDelivery delivery = new ClientDelivery();
                                            delivery.addMessage(sentEvent);

                                            MixpanelAPI mixpanel = new MixpanelAPI();
                                            mixpanel.deliver(delivery);
                                        }
                                    }
                                   
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        });

                    }
		}//end of btc
		//start if DOGE
		else if ((whatLoot<=9249)&&(whatLoot>=8500)){                    
			altQuest.DOGE_wallet.updateBalance();
                    if(altQuest.DOGE_wallet.balance>=2000) {
			double TDLoot=	((altQuest.getSwapValue(money*0.00000001, "btc", "doge")));		
			int Doge_Loot = (int)(TDLoot/0.00000001);

                        final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                        final DOGE_Wallet userDOGE_Wallet=user.DOGE_wallet;
                        AltQuest.REDIS.expire("DOGE_balance"+player.getUniqueId().toString(),5);

                        scheduler.runTaskAsynchronously(altQuest, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (altQuest.DOGE_wallet.create_blockcypher_transaction(Doge_Loot, userDOGE_Wallet.address)) {
                                        System.out.println("[loot]"+whatLoot+" "+player.getDisplayName()+": DOGE "+(Doge_Loot*0.00000001));
                                        player.sendMessage(ChatColor.GREEN + "You got " + ChatColor.BOLD + (Doge_Loot*0.00000001) + ChatColor.GREEN + " DOGE of loot!");
                                        // player.playSound(player.getLocation(), Sound.LEVEL_UP, 20, 1);
                                        if (altQuest.messageBuilder != null) {

                                            // Create an event
                                            org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Loot", null);


                                            ClientDelivery delivery = new ClientDelivery();
                                            delivery.addMessage(sentEvent);

                                            MixpanelAPI mixpanel = new MixpanelAPI();
                                            mixpanel.deliver(delivery);
                                        }
                                    }
                                   
                                } catch (org.json.simple.parser.ParseException e) {
        	e.printStackTrace();
        } catch (IOException e1) {
                                    e1.printStackTrace();
                                } 
                            }
                        });

                    }
		}//end of DOGE
			//start if emerald
		else if (whatLoot<=1000){                    
			


                        final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                        scheduler.runTaskAsynchronously(altQuest, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (altQuest.addEmeralds(player,(money/100))) {
                                        System.out.println("[loot]"+whatLoot+" "+player.getDisplayName()+": Emeralds  "+(money/100));
                                        player.sendMessage(ChatColor.GREEN + "You got " + ChatColor.BOLD + money / 100 + ChatColor.GREEN + " Emeralds of loot!");
                                        // player.playSound(player.getLocation(), Sound.LEVEL_UP, 20, 1);
                                        if (altQuest.messageBuilder != null) {

                                            // Create an event
                                            org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Loot", null);


                                            ClientDelivery delivery = new ClientDelivery();
                                            delivery.addMessage(sentEvent);

                                            MixpanelAPI mixpanel = new MixpanelAPI();
                                            mixpanel.deliver(delivery);
                                        }
                                    }
                                    
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } 
                            }
                        });

                    
		}//end of emerald
                    // Add EXP
                    user.addExperience(level*2);
                    if(altQuest.messageBuilder!=null) {

                        final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

                        scheduler.runTaskAsynchronously(altQuest, new Runnable() {


                            @Override
                            public void run() {
                                // Create an event
                                org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Kill", null);


                                ClientDelivery delivery = new ClientDelivery();
                                delivery.addMessage(sentEvent);

                                MixpanelAPI mixpanel = new MixpanelAPI();
                                try {
                                    mixpanel.deliver(delivery);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }

                        });

                    }
                }

            } else {
                e.setDroppedExp(0);
            }
        } else {
            e.setDroppedExp(0);
        }

    }

    String spawnKey(Location location) {
        return location.getWorld().getName()+location.getChunk().getX()+","+location.getChunk().getZ()+"spawn";

    }
    // TODO: Right now, entity spawns are cancelled, then replaced with random mob spawns. Perhaps it would be better to
    //          find a way to instead set the EntityType of the event. Is there any way to do that?
    // TODO: Magma Cubes don't get levels or custom names for some reason...
    @EventHandler
    void onEntitySpawn(org.bukkit.event.entity.CreatureSpawnEvent e) {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();



        LivingEntity entity = e.getEntity();
        if (entity instanceof Monster) {

            int baselevel=16;

            if(e.getLocation().getWorld().getName().equals("world_nether")) {
                baselevel=32;
            } else if(e.getLocation().getWorld().getName().equals("world_end")) {
                baselevel=64;
            }

            // Disable mob spawners. Keep mob farmers away
            if (e.getSpawnReason() == SpawnReason.SPAWNER) {
                e.setCancelled(true);
            } else if(altQuest.landIsClaimed(e.getLocation())==false) {
                e.setCancelled(false);
                World world = e.getLocation().getWorld();
                EntityType entityType = entity.getType();
                // nerf_level makes sure high level mobs are away from the spawn
                int spawn_distance= (int)e.getLocation().getWorld().getSpawnLocation().distance(e.getLocation());
                int buff_level=(spawn_distance/128);
                if(buff_level>baselevel) buff_level=baselevel;
                if(buff_level<1) buff_level=1;

                // max level is baselevel * 2 minus nerf level
                int level=AltQuest.rand(1, buff_level*2);

                entity.setMaxHealth(level * 4);
                entity.setHealth(level * 4);
                entity.setMetadata("level", new FixedMetadataValue(altQuest, level));
                entity.setCustomName(String.format("%s lvl %d", WordUtils.capitalizeFully(entityType.name().replace("_", " ")), level));

                // add potion effects
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, Integer.MAX_VALUE, 2), true);
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 2), true);
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 2), true);
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 2), true);
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 2), true);
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 2), true);
                if (AltQuest.rand(0, 128) < level)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2), true);

                // give random equipment
                if (entity instanceof Zombie || entity instanceof PigZombie || entity instanceof Skeleton) {
                    useRandomEquipment(entity, level);
                }

                // some creepers are charged
                if (entity instanceof Creeper && AltQuest.rand(0, 100) < level) {
                    ((Creeper) entity).setPowered(true);
                }

                // pigzombies are always angry
                if (entity instanceof PigZombie) {
                    PigZombie pigZombie = (PigZombie) entity;
                    pigZombie.setAngry(true);
                }

                // some skeletons are black
                if (entity instanceof Skeleton) {
                    Skeleton skeleton = (Skeleton) entity;
                    ItemStack bow = new ItemStack(Material.BOW);
                    if (AltQuest.rand(0, 64) < level) {
                        randomEnchantItem(bow);
                    }
                }
                System.out.println("[spawn mob] "+entityType.name()+" lvl "+level+" spawn distance: "+spawn_distance+" buff level: "+buff_level);
            } else {
                e.setCancelled(true);
            }
        } else {
            e.setCancelled(false);
        }
    }
    @EventHandler
    void onEntityDamage(EntityDamageEvent event) throws ParseException, org.json.simple.parser.ParseException, IOException {

    	// damage by entity
    	if (event instanceof EntityDamageByEntityEvent) {
    		// Player vs. Animal in claimed location
    		if (event.getEntity() instanceof Animals && ((EntityDamageByEntityEvent) event).getDamager() instanceof Player){
    			if(!altQuest.canBuild(event.getEntity().getLocation(), (Player)((EntityDamageByEntityEvent) event).getDamager())){
    				event.setCancelled(true);
    			}
    		}
    		// Player vs. Villager
    		if (event.getEntity() instanceof Villager) {
    			if (!altQuest.isPvP(event.getEntity().getLocation()))
			event.setCancelled(true);
    		}
    		// PvP is always off unless landPermissionCode()=="v"
    		if (event.getEntity() instanceof Player && ((EntityDamageByEntityEvent) event).getDamager() instanceof Player) {		
			if (!altQuest.isPvP(event.getEntity().getLocation()))
			event.setCancelled(true);	
			
    		}
		//  use the above to create a PVP chunk if set to false?


        }
    }
   


    public void useRandomEquipment(LivingEntity entity, int level) {

        // Gives random SWORD
        if (AltQuest.rand(0, 32) < level && !(entity instanceof Skeleton)) {
            ItemStack sword = new ItemStack(Material.WOODEN_DOOR);
            if (AltQuest.rand(0, 128) < level) sword = new ItemStack(Material.WOODEN_DOOR);
            if (AltQuest.rand(0, 128) < level) sword = new ItemStack(Material.IRON_AXE);
            if (AltQuest.rand(0, 128) < level) sword = new ItemStack(Material.WOOD_SWORD);
            if (AltQuest.rand(0, 128) < level) sword = new ItemStack(Material.IRON_SWORD);
            if (AltQuest.rand(0, 128) < level) sword = new ItemStack(Material.DIAMOND_SWORD);

            if (AltQuest.rand(0, 128) < level) randomEnchantItem(sword);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(sword);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(sword);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(sword);

            entity.getEquipment().setItemInHand(sword);
        }

        // Gives random HELMET
        if (AltQuest.rand(0, 32) < level) {
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            if (AltQuest.rand(0, 128) < level) helmet = new ItemStack(Material.CHAINMAIL_HELMET);
            if (AltQuest.rand(0, 128) < level) helmet = new ItemStack(Material.IRON_HELMET);
            if (AltQuest.rand(0, 128) < level) helmet = new ItemStack(Material.DIAMOND_HELMET);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(helmet);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(helmet);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(helmet);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(helmet);

            entity.getEquipment().setHelmet(helmet);
        }

        // Gives random CHESTPLATE
        if (AltQuest.rand(0, 32) < level) {
            ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
            if (AltQuest.rand(0, 128) < level) chest = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
            if (AltQuest.rand(0, 128) < level) chest = new ItemStack(Material.IRON_CHESTPLATE);
            if (AltQuest.rand(0, 128) < level) chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(chest);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(chest);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(chest);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(chest);

            entity.getEquipment().setChestplate(chest);
        }

        // Gives random Leggings
        if (AltQuest.rand(0, 128) < level) {
            ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
            if (AltQuest.rand(0, 128) < level) leggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
            if (AltQuest.rand(0, 128) < level) leggings = new ItemStack(Material.IRON_LEGGINGS);
            if (AltQuest.rand(0, 128) < level) leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(leggings);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(leggings);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(leggings);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(leggings);

            entity.getEquipment().setLeggings(leggings);
        }

        // Gives Random BOOTS
        if (AltQuest.rand(0, 128) < level) {
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
            if (AltQuest.rand(0, 128) < level) boots = new ItemStack(Material.CHAINMAIL_BOOTS);
            if (AltQuest.rand(0, 128) < level) boots = new ItemStack(Material.IRON_BOOTS);
            if (AltQuest.rand(0, 128) < level) boots = new ItemStack(Material.DIAMOND_BOOTS);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(boots);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(boots);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(boots);
            if (AltQuest.rand(0, 128) < level) randomEnchantItem(boots);

            entity.getEquipment().setBoots(boots);
        }
    }

    // Random Enchantment
    public static void randomEnchantItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Enchantment enchantment = null;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.ARROW_FIRE;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.DAMAGE_ALL;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.ARROW_DAMAGE;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.ARROW_INFINITE;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.ARROW_KNOCKBACK;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.DAMAGE_ARTHROPODS;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.DAMAGE_UNDEAD;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.DIG_SPEED;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.DURABILITY;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.FIRE_ASPECT;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.KNOCKBACK;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.LOOT_BONUS_BLOCKS;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.LOOT_BONUS_MOBS;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.LUCK;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.LURE;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.OXYGEN;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.PROTECTION_ENVIRONMENTAL;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.PROTECTION_EXPLOSIONS;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.PROTECTION_FALL;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.PROTECTION_PROJECTILE;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.PROTECTION_FIRE;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.SILK_TOUCH;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.THORNS;
        if (AltQuest.rand(0, 64) == 0) enchantment = Enchantment.WATER_WORKER;

        if (enchantment != null) {
            int level = AltQuest.rand(enchantment.getStartLevel(), enchantment.getMaxLevel());
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);

        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        Block b = event.getClickedBlock();
        Player p = event.getPlayer();
        if(b!=null && PROTECTED_BLOCKS.contains(b.getType())) {
            if(!altQuest.canBuild(b.getLocation(),event.getPlayer())) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED+"You don't have permission to do that");
            }
        }

    }

    @EventHandler
    void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player p = event.getPlayer();
        if (!altQuest.canBuild(event.getBlockClicked().getLocation(), event.getPlayer())) {
            p.sendMessage(ChatColor.RED+"You don't have permission to do that");
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player p = event.getPlayer();
        if (!altQuest.canBuild(event.getBlockClicked().getLocation(), event.getPlayer())) {
            p.sendMessage(ChatColor.RED+"You don't have permission to do that");
            event.setCancelled(true);
        }
    }

    @EventHandler
	void onExplode(EntityExplodeEvent event) {
		event.setCancelled(true);
	}

}

