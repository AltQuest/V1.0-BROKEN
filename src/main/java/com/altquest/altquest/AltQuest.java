package com.altquest.altquest;

import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;


import com.evilmidget38.UUIDFetcher;
import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by explodi on 11/1/15.
 */

public class  AltQuest extends JavaPlugin {
    // Connecting to REDIS
    // Links to the administration account via Environment Variables
    public final static String ALTQUEST_ENV = System.getenv("ALTQUEST_ENV") != null ? System.getenv("ALTQUEST_ENV") : "development";

    public final static UUID ADMIN_UUID = System.getenv("ADMIN_UUID") != null ? UUID.fromString(System.getenv("ADMIN_UUID")) : null;
    public final static String BLOCKCYPHER_API_KEY = System.getenv("BLOCKCYPHER_API_KEY") != null ? System.getenv("BLOCKCYPHER_API_KEY") : null;

    public final static String BITCOIN_ADDRESS = System.getenv("BITCOIN_ADDRESS") != null ? System.getenv("BITCOIN_ADDRESS") : null;
    public final static String BITCOIN_PRIVATE_KEY = System.getenv("BITCOIN_PRIVATE_KEY") != null ? System.getenv("BITCOIN_PRIVATE_KEY") : null;
    public final static String LAND_BITCOIN_ADDRESS = System.getenv("LAND_BITCOIN_ADDRESS") != null ? System.getenv("LAND_BITCOIN_ADDRESS") : null;
    public final static String MINER_FEE_ADDRESS = System.getenv("MINER_FEE_ADDRESS") != null ? System.getenv("MINER_FEE_ADDRESS") : null;
    public final static String MINER_FEE_PRIVATE_KEY = System.getenv("MINER_FEE_PRIVATE_KEY") != null ? System.getenv("MINER_FEE_PRIVATE_KEY") : null;

    public final static String DOGE_ADDRESS = System.getenv("DOGE_ADDRESS") != null ? System.getenv("DOGE_ADDRESS") : null;
    public final static String DOGE_PRIVATE_KEY = System.getenv("DOGE_PRIVATE_KEY") != null ? System.getenv("DOGE_PRIVATE_KEY") : null;
    public final static String LAND_DOGE_ADDRESS = System.getenv("LAND_DOGE_ADDRESS") != null ? System.getenv("LAND_DOGE_ADDRESS") : null;
    public final static String DOGE_FEE_ADDRESS = System.getenv("DOGE_FEE_ADDRESS") != null ?  System.getenv("DOGE_FEE_ADDRESS") : null;
    public final static String DOGE_FEE_PRIVATE_KEY = System.getenv("DOGE_FEE_PRIVATE_KEY") != null ? System.getenv("DOGE_FEE_PRIVATE_KEY") : null;

    public final static String LTC_ADDRESS = System.getenv("LTC_ADDRESS") != null ? System.getenv("LTC_ADDRESS") : null;
    public final static String LTC_PRIVATE_KEY = System.getenv("LTC_PRIVATE_KEY") != null ? System.getenv("LTC_PRIVATE_KEY") : null;
    public final static String LAND_LTC_ADDRESS = System.getenv("LAND_LTC_ADDRESS") != null ?   System.getenv("LAND_LTC_ADDRESS") : null;
    public final static String LTC_FEE_ADDRESS = System.getenv("LTC_FEE_ADDRESS") != null ? System.getenv("LTC_FEE_ADDRESS") : null;
    public final static String LTC_FEE_PRIVATE_KEY = System.getenv("LTC_FEE_PRIVATE_KEY") != null ? System.getenv("LTC_FEE_PRIVATE_KEY") : null;

    // if BLOCKCHAIN is set, users can choose a blockchain supported by BlockCypher (very useful for development on testnet, or maybe DogeQuest?)
    public final static String BLOCKCHAIN = System.getenv("BLOCKCHAIN") != null ? System.getenv("BLOCKCHAIN") : "btc/main";

    // Support for the bitcore full node and insight-api.
    public final static String BITCORE_HOST = System.getenv("BITCORE_HOST") != null ? System.getenv("BITCORE_HOST") : null;

    // Support for statsd is optional but really cool
    public final static String STATSD_HOST = System.getenv("STATSD_HOST") != null ? System.getenv("STATSD_HOST") : null;
    public final static String STATSD_PREFIX = System.getenv("STATSD_PREFIX") != null ? System.getenv("STATSD_PREFIX") : "altquest";
    public final static String STATSD_PORT = System.getenv("STATSD_PORT") != null ? System.getenv("STATSD_PORT") : "8125";
    // Support for mixpanel analytics
    public final static String MIXPANEL_TOKEN = System.getenv("MIXPANEL_TOKEN") != null ? System.getenv("MIXPANEL_TOKEN") : null;
    public MessageBuilder messageBuilder;
    // REDIS: Look for Environment variables on hostname and port, otherwise defaults to localhost:6379
    public final static String REDIS_HOST = System.getenv("REDIS_1_PORT_6379_TCP_ADDR") != null ? System.getenv("REDIS_1_PORT_6379_TCP_ADDR") : "localhost";
    public final static Integer REDIS_PORT = System.getenv("REDIS_1_PORT_6379_TCP_PORT") != null ? Integer.parseInt(System.getenv("REDIS_1_PORT_6379_TCP_PORT")) : 6379;
    public final static Jedis REDIS = new Jedis(REDIS_HOST, REDIS_PORT);
    // FAILS
    // public final static JedisPool REDIS_POOL = new JedisPool(new JedisPoolConfig(), REDIS_HOST, REDIS_PORT);


    // TODO: Find out why this crashes the server
    // public static ScoreboardManager manager = Bukkit.getScoreboardManager();
    // public static Scoreboard scoreboard = manager.getNewScoreboard();
    public final static int LAND_PRICE=20000;
    // utilities: distance and rand
    public static int distance(Location location1, Location location2) {
        return (int) location1.distance(location2);
    }

    public static int rand(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }
    public StatsDClient statsd;
    public Wallet wallet=null;
    public Wallet miner_wallet=null;
    public LTC_Wallet LTC_wallet=null;
    public LTC_Wallet LTC_miner_wallet=null;
    public DOGE_Wallet DOGE_wallet=null;
    public DOGE_Wallet DOGE_miner_wallet=null;


    @Override
    public void onEnable() {
        log("AltQuest starting");
        log("Using the "+AltQuest.BLOCKCHAIN+" blockchain");
        REDIS.set("STARTUP","1");
        REDIS.expire("STARTUP",300);
        if (ADMIN_UUID == null) {
            log("Warning: You haven't designated a super admin. Launch with ADMIN_UUID env variable to set.");
        }
        if(STATSD_HOST!=null && STATSD_PORT!=null && STATSD_PREFIX!=null) {
            statsd = new NonBlockingStatsDClient(STATSD_PREFIX, System.getenv("STATSD_HOST") , Integer.parseInt(System.getenv("STATSD_PORT")));
            System.out.println("StatsD support is on.");
        }
        // registers listener classes
        getServer().getPluginManager().registerEvents(new ChatEvents(this), this);
        getServer().getPluginManager().registerEvents(new BlockEvents(this), this);
        getServer().getPluginManager().registerEvents(new EntityEvents(this), this);
        getServer().getPluginManager().registerEvents(new InventoryEvents(this), this);
        getServer().getPluginManager().registerEvents(new SignEvents(this), this);
        getServer().getPluginManager().registerEvents(new ServerEvents(this), this);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule keepInventory on");

        // loads config file. If it doesn't exist, creates it.
        // get plugin config
        getDataFolder().mkdir();
        if (!new java.io.File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
// loads/creates LTC wallet
        if(LTC_ADDRESS !=null && LTC_PRIVATE_KEY!=null) {
            LTC_wallet=new LTC_Wallet(LTC_ADDRESS,LTC_PRIVATE_KEY);
            System.out.println("LTC wallet address is: "+LTC_ADDRESS);
            LTC_wallet.updateBalance();
            System.out.println("Balance: "+LTC_wallet.balance);
            System.out.println("Unconfirmed: "+LTC_wallet.unconfirmedBalance);
            System.out.println("Final Balance: "+LTC_wallet.final_balance());
        } else {
            System.out.println("Warning: world wallet address not defined in environment. A new one will be generated but it will cease to exists once the server stops. This is probably fine if you are running for development/testing purposes");
            System.out.println("Generating new address...");
            URL url = null;
            try {
                url = new URL("https://api.blockcypher.com/v1/ltc/main/addrs");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(con.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                LTC_wallet=new LTC_Wallet((String) jsonobj.get("address"),(String) jsonobj.get("private"));

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
        }
        //end LTC wallet
// loads/creates LTC_Fee wallet
        if(LTC_FEE_ADDRESS !=null && LTC_FEE_PRIVATE_KEY!=null) {
            LTC_miner_wallet=new LTC_Wallet(LTC_FEE_ADDRESS,LTC_FEE_PRIVATE_KEY);
            System.out.println("LTC Fee wallet address is: "+LTC_FEE_ADDRESS);
            LTC_miner_wallet.updateBalance();
            System.out.println("Balance: "+LTC_miner_wallet.balance);
            System.out.println("Unconfirmed: "+LTC_miner_wallet.unconfirmedBalance);
            System.out.println("Final Balance: "+LTC_miner_wallet.final_balance());
        } else {
            System.out.println("Warning: world wallet address not defined in environment. A new one will be generated but it will cease to exists once the server stops. This is probably fine if you are running for development/testing purposes");
            System.out.println("Generating new address...");
            URL url = null;
            try {
                url = new URL("https://api.blockcypher.com/v1/ltc/main/addrs");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(con.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                LTC_miner_wallet=new LTC_Wallet((String) jsonobj.get("address"),(String) jsonobj.get("private"));

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
        }
        //end LTC miner fee wallet
// loads/creates DOGE_Fee wallet
        if(DOGE_ADDRESS !=null && DOGE_PRIVATE_KEY!=null) {
            DOGE_wallet=new DOGE_Wallet(DOGE_ADDRESS,DOGE_PRIVATE_KEY);
            System.out.println("DOGE wallet address is: "+DOGE_ADDRESS);
            DOGE_wallet.updateBalance();
            System.out.println("Balance: "+(DOGE_wallet.balance/10000000));
            System.out.println("Unconfirmed: "+(DOGE_wallet.unconfirmedBalance/10000000));
            //System.out.println("Final Balance: "+(DOGE_wallet.final_balance()/10000000));
		
		
		
		
        } else {
            System.out.println("Warning: world wallet address not defined in environment. A new one will be generated but it will cease to exists once the server stops. This is probably fine if you are running for development/testing purposes");
            System.out.println("Generating new address...");
            URL url = null;
            try {
                url = new URL("https://api.blockcypher.com/v1/doge/main/addrs");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(con.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                DOGE_wallet=new DOGE_Wallet((String) jsonobj.get("address"),(String) jsonobj.get("private"));

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
        }
        //end DOGE wallet
// loads/creates DOGE_Fee wallet
        if(DOGE_FEE_ADDRESS !=null && DOGE_FEE_PRIVATE_KEY!=null) {
            DOGE_miner_wallet=new DOGE_Wallet(DOGE_FEE_ADDRESS,DOGE_FEE_PRIVATE_KEY);
            System.out.println("DOGE Fee wallet address is: "+DOGE_FEE_ADDRESS);
            DOGE_miner_wallet.updateBalance();
            System.out.println("Balance: "+(DOGE_miner_wallet.balance/10000000));
            System.out.println("Unconfirmed: "+(DOGE_miner_wallet.unconfirmedBalance/10000000));
	    //System.out.println("Final Balance: "+(DOGE_miner_wallet.final_balance()/10000000));
		
		
		
		
        } else {
            System.out.println("Warning: world wallet address not defined in environment. A new one will be generated but it will cease to exists once the server stops. This is probably fine if you are running for development/testing purposes");
            System.out.println("Generating new address...");
            URL url = null;
            try {
                url = new URL("https://api.blockcypher.com/v1/doge/main/addrs");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(con.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                DOGE_miner_wallet=new DOGE_Wallet((String) jsonobj.get("address"),(String) jsonobj.get("private"));

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
        }
        //end DOGE miner fee wallet
// loads/creates Miner_Fee wallet
        if(MINER_FEE_ADDRESS !=null && MINER_FEE_PRIVATE_KEY!=null) {
            miner_wallet=new Wallet(MINER_FEE_ADDRESS,MINER_FEE_PRIVATE_KEY);
            System.out.println("Miner Fee wallet address is: "+MINER_FEE_ADDRESS);
            miner_wallet.updateBalance();
            System.out.println("Balance: "+miner_wallet.balance);
            System.out.println("Unconfirmed: "+miner_wallet.unconfirmedBalance);
            System.out.println("Final Balance: "+miner_wallet.final_balance());
        } else {
            System.out.println("Warning: world wallet address not defined in environment. A new one will be generated but it will cease to exists once the server stops. This is probably fine if you are running for development/testing purposes");
            System.out.println("Generating new address...");
            URL url = null;
            try {
                url = new URL("https://api.blockcypher.com/v1/"+AltQuest.BLOCKCHAIN+"/addrs");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(con.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                miner_wallet=new Wallet((String) jsonobj.get("address"),(String) jsonobj.get("private"));

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
        }
        // loads/creates world wallet
        if(BITCOIN_ADDRESS!=null && BITCOIN_PRIVATE_KEY!=null) {
            wallet=new Wallet(BITCOIN_ADDRESS,BITCOIN_PRIVATE_KEY);
            System.out.println("World wallet address is: "+BITCOIN_ADDRESS);
            wallet.updateBalance();
            System.out.println("Balance: "+wallet.balance);
            System.out.println("Unconfirmed: "+wallet.unconfirmedBalance);
            System.out.println("Final Balance: "+wallet.final_balance());
        } else {
            System.out.println("Warning: world wallet address not defined in environment. A new one will be generated but it will cease to exists once the server stops. This is probably fine if you are running for development/testing purposes");
            System.out.println("Generating new address...");
            URL url = null;
            try {
                url = new URL("https://api.blockcypher.com/v1/"+AltQuest.BLOCKCHAIN+"/addrs");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(con.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            final JSONObject jsonobj;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                wallet=new Wallet((String) jsonobj.get("address"),(String) jsonobj.get("private"));

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
        }
        REDIS.configSet("SAVE","900 1 300 10 60 10000");
        if(MIXPANEL_TOKEN!=null) {
            messageBuilder = new MessageBuilder(MIXPANEL_TOKEN);
            System.out.println("Mixpanel support is on");
        }
        // Removes all entities on server restart. This is a workaround for when large numbers of entities grash the server. With the release of Minecraft 1.11 and "max entity cramming" this will be unnecesary.
        //     removeAllEntities();
        killAllVillagers();
        createScheduledTimers();

        // create scoreboards

    }
	public double getSwapValue(double Amount, String crypto, String crypto2)
	{	
		String S1=(getExchangeRate(crypto));
		double Rate1 = Double.parseDouble(S1);
		String S2=(getExchangeRate(crypto2));
		double Rate2 = Double.parseDouble(S2);
		double HowMuch = ((Rate1*Amount)/Rate2);
		return HowMuch;
		
		}
    public String getExchangeRate(String crypto)
	{
	String price="0000.00000000";
	 try {
            
                URL url=new URL("https://api.cryptonator.com/api/ticker/"+crypto+"-usd");
                
                System.out.println(url.toString());
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "Mozilla/1.22 (compatible; MSIE 2.0; Windows 3.1)");
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                int responseCode = con.getResponseCode();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
		JSONParser parser = new JSONParser();
            	final JSONObject jsonobj,jsonobj2;
            try {
                jsonobj = (JSONObject) parser.parse(response.toString());
                jsonobj2 = (JSONObject) parser.parse(jsonobj.get("ticker").toString());
		//double val=Double.parseDouble(jsonobj2.get("price").toString());
		

		price=jsonobj2.get("price").toString();
		//Sprice = BigDecimal.valueOf(jsonobj2.get("price").toString()).toPlainString();
		//price= Sprice;
                System.out.println(" "+price);

            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("[PRICE] problem updating price for "+crypto);
            System.out.println(e);
            // wallet might be new and it's not listed on the blockchain yet
        } 
	return price;
    }
    public void updateScoreboard(Player player) throws ParseException, org.json.simple.parser.ParseException, IOException {
        ScoreboardManager scoreboardManager;
        Scoreboard walletScoreboard;
        Objective walletScoreboardObjective;
        scoreboardManager = Bukkit.getScoreboardManager();
        walletScoreboard= scoreboardManager.getNewScoreboard();
	String TString="ExRate";
        walletScoreboardObjective = walletScoreboard.registerNewObjective("wallet","dummy");
        walletScoreboardObjective = walletScoreboard.registerNewObjective(TString, TString);
 	
	
        walletScoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        walletScoreboardObjective.setDisplayName(ChatColor.GOLD + ChatColor.BOLD.toString() + "Alt" + ChatColor.GRAY + ChatColor.BOLD.toString() + "Quest");
	if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("BTC"))
{          
	String BTC_RATE=getExchangeRate("btc");
	 if(REDIS.exists("final_balance:"+player.getUniqueId().toString())==false) {
            User user=new User(player);
            user.wallet.updateBalance();
            REDIS.set("final_balance:"+player.getUniqueId().toString(),String.valueOf(user.wallet.final_balance()));
            REDIS.expire("final_balance:"+player.getUniqueId().toString(),30);
        }        
	TString=(" 1BTC/"+BTC_RATE+"$US");
	walletScoreboardObjective.setDisplayName(ChatColor.GREEN + "ExRate:" +TString);   
	Score score = walletScoreboardObjective.getScore(ChatColor.GREEN +"BTC/Bits:"); //Get a fake offline player
	

        int final_balance=Integer.parseInt(REDIS.get("final_balance:"+player.getUniqueId().toString()));


        score.setScore(final_balance/100);
        player.setScoreboard(walletScoreboard);
	}
	else if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("LTC")){             
	String LTC_RATE=getExchangeRate("ltc");
	if(REDIS.exists("LTC_final_balance:"+player.getUniqueId().toString())==false) {
            User user=new User(player);
            user.LTC_wallet.updateBalance();
            REDIS.set("LTC_final_balance:"+player.getUniqueId().toString(),String.valueOf(user.LTC_wallet.final_balance()));
            REDIS.expire("LTC_final_balance:"+player.getUniqueId().toString(),30);
        }           
	TString=(" 1LTC/"+LTC_RATE+"$US");
	walletScoreboardObjective.setDisplayName(ChatColor.GREEN + "ExRate:" + TString);
	Score score = walletScoreboardObjective.getScore(ChatColor.GREEN +"LTC:"); //Get a fake offline player

        double LTC_final_balance=Double.parseDouble(REDIS.get("LTC_final_balance:"+player.getUniqueId().toString()));


        score.setScore((int)LTC_final_balance);
        player.setScoreboard(walletScoreboard);
	}
	else if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("DOGE")){        
	String DOGE_RATE=getExchangeRate("doge");  
	if(REDIS.exists("DOGE_final_balance:"+player.getUniqueId().toString())==false) {
            User user=new User(player);
            user.DOGE_wallet.updateBalance();
            REDIS.set("DOGE_final_balance:"+player.getUniqueId().toString(),String.valueOf(user.DOGE_wallet.final_balance()));
            REDIS.expire("DOGE_final_balance:"+player.getUniqueId().toString(),30);
        }   
	TString=(" 1DOGE/"+DOGE_RATE+"$US");      
	walletScoreboardObjective.setDisplayName(ChatColor.GREEN + "ExRate:"+TString); 
	Score score = walletScoreboardObjective.getScore(ChatColor.GREEN + "DOGE:"); //Get a fake offline player

        int DOGE_final_balance=Integer.parseInt(REDIS.get("DOGE_final_balance:"+player.getUniqueId().toString()));


        score.setScore((int)DOGE_final_balance);
        player.setScoreboard(walletScoreboard);
	}
	else if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("emerald")){ 
	TString = (" 1Em/1Bit");	
	walletScoreboardObjective.setDisplayName(ChatColor.GREEN + "ExRate:"+TString);
	Score score = walletScoreboardObjective.getScore(ChatColor.GREEN + "Emeralds:"); //Get a fake offline player
	
	int EmAmount=countEmeralds(player);
		
	
        //int final_balance=Integer.parseInt(REDIS.get("final_balance:"+player.getUniqueId().toString()));


        score.setScore(EmAmount);
        player.setScoreboard(walletScoreboard);
	}
    }
    public void createScheduledTimers() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getServer().getOnlinePlayers()){
                    User user= null;
                    try {
                        // user.createScoreBoard();
                        updateScoreboard(player);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (org.json.simple.parser.ParseException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO: Handle rate limiting
                    }
                }
            }
        }, 0, 120L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // A villager is born
                World world=Bukkit.getWorld("world");
                world.spawnEntity(world.getHighestBlockAt(world.getSpawnLocation()).getLocation(), EntityType.VILLAGER);
            }
        }, 0, 72000L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if(statsd!=null) {
                    sendWorldMetrics();
                }
            }
        }, 0, 1200L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if(statsd!=null) {
                    sendWalletMetrics();
                }
            }
        }, 0, 12000L);
        REDIS.set("lastloot","nobody");

        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if(AltQuest.BLOCKCHAIN.equals("bcy/test")) {
                    wallet.getTestnetCoins();
                }
            }
        }, 0, 72000L);
    }
    public void sendWorldMetrics() {
        statsd.gauge("players",Bukkit.getServer().getOnlinePlayers().size());
        statsd.gauge("entities_world",Bukkit.getServer().getWorld("world").getEntities().size());
        statsd.gauge("entities_nether",Bukkit.getServer().getWorld("world_nether").getEntities().size());
        statsd.gauge("entities_the_end",Bukkit.getServer().getWorld("world_the_end").getEntities().size());
    }
    public  void sendWalletMetrics() {
        statsd.gauge("wallet_balance",wallet.balance());
        statsd.gauge("DOGE_wallet_balance",wallet.balance());
        statsd.gauge("LTC_wallet_balance",wallet.balance());

	

    }
    public void removeAllEntities() {
        World w=Bukkit.getWorld("world");
        List<Entity> entities = w.getEntities();
        int entitiesremoved=0;
        for ( Entity entity : entities){
            entity.remove();
            entitiesremoved=entitiesremoved+1;

        }
        System.out.println("Killed "+entitiesremoved+" entities");
    }
    public void killAllVillagers() {
        World w=Bukkit.getWorld("world");
        List<Entity> entities = w.getEntities();
        int villagerskilled=0;
        for ( Entity entity : entities){
            if ((entity instanceof Villager)) {
                villagerskilled=villagerskilled+1;
                ((Villager)entity).remove();
            }
        }
        System.out.println("Killed "+villagerskilled+" villagers");

    }
    public void log(String msg) {
        Bukkit.getLogger().info(msg);
    }

    public void success(Player recipient, String msg) {
        recipient.sendMessage(ChatColor.GREEN + msg);
    }

    public void error(Player recipient, String msg) {
        recipient.sendMessage(ChatColor.RED + msg);
    }

    public void claimLand(String name, Chunk chunk, Player player) throws ParseException, org.json.simple.parser.ParseException, IOException {
        // check that land actually has a name
        final int x = chunk.getX();
        final int z = chunk.getZ();
        System.out.println("[claim] "+player.getDisplayName()+" wants to claim "+x+","+z+" with name "+name);

        if (!name.isEmpty()) {
            // check that desired area name is alphanumeric
            boolean hasNonAlpha = name.matches("^.*[^a-zA-Z0-9 ].*$");
            if (!hasNonAlpha) {
                // 32 characters max
                if (name.length() <= 32) {


                    if (name.equalsIgnoreCase("the wilderness")) {
                        player.sendMessage(ChatColor.RED + "You cannot name your land that.");
                        return;
                    }
                    if (REDIS.get("chunk" + x + "," + z + "owner") == null) {
			if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("BTC")){                        
			final User user = new User(player);
                        player.sendMessage(ChatColor.YELLOW + "Claiming land...");
                        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                        AltQuest altQuest = this;
                        scheduler.runTask(this, new Runnable() {
                            @Override
                            public void run() {
                                // A villager is born
                                try {
                                    Wallet paymentWallet;
                                    if (AltQuest.LAND_BITCOIN_ADDRESS != null) {
                                        paymentWallet = new Wallet(AltQuest.LAND_BITCOIN_ADDRESS);
                                    } else {
                                        paymentWallet = altQuest.wallet;
                                    }
					
                                    if ((player.getUniqueId().toString().equals(AltQuest.ADMIN_UUID.toString()))||(user.wallet.transaction(AltQuest.LAND_PRICE, paymentWallet))) {

                                        AltQuest.REDIS.set("chunk" + x + "," + z + "owner", player.getUniqueId().toString());
                                        AltQuest.REDIS.set("chunk" + x + "," + z + "name", name);
                                        player.sendMessage(ChatColor.GREEN + "Congratulations! You're now the owner of " + name + "!");
                                        if (altQuest.messageBuilder != null) {

                                            // Create an event
                                            org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Claim", null);
                                            org.json.JSONObject sentCharge = altQuest.messageBuilder.trackCharge(player.getUniqueId().toString(), AltQuest.LAND_PRICE / 100, null);


                                            ClientDelivery delivery = new ClientDelivery();
                                            delivery.addMessage(sentEvent);
                                            delivery.addMessage(sentCharge);


                                            MixpanelAPI mixpanel = new MixpanelAPI();
                                            mixpanel.deliver(delivery);
                                        }
                                    } 
					
					else {
                                        int balance = new User(player).wallet.balance();
                                        if (balance < AltQuest.LAND_PRICE) {
                                            player.sendMessage(ChatColor.RED + "You don't have enough money! You need " +
                                                    ChatColor.BOLD + Math.ceil((AltQuest.LAND_PRICE - balance) / 100) + ChatColor.RED + " more Bits.");
                                        } else {
                                            player.sendMessage(ChatColor.RED + "Claim payment failed. Please try again later.");
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                } catch (org.json.simple.parser.ParseException e) {
                                    e.printStackTrace();
                                }
                                ;
                            }
                        });
			}//end of BTC buy land start of emerald buy land by @bitcoinjake09
			else if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("emerald")){        

		//final User user = new User(player);
                        player.sendMessage(ChatColor.YELLOW + "Claiming land...");
                        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                        AltQuest altQuest = this;
                        scheduler.runTask(this, new Runnable() {
                            @Override
                            public void run() {
                                // A villager is born
                                try {
                                  
                                    if ((player.getUniqueId().toString().equals(AltQuest.ADMIN_UUID.toString()))||((removeEmeralds(player,(AltQuest.LAND_PRICE / 100))) == true)) {

                                        AltQuest.REDIS.set("chunk" + x + "," + z + "owner", player.getUniqueId().toString());
                                        AltQuest.REDIS.set("chunk" + x + "," + z + "name", name);
                                        player.sendMessage(ChatColor.GREEN + "Congratulations! You're now the owner of " + name + "!");
                                        if (altQuest.messageBuilder != null) {

                                            // Create an event
                                            org.json.JSONObject sentEvent = altQuest.messageBuilder.event(player.getUniqueId().toString(), "Claim", null);
                                            //org.json.JSONObject sentCharge = altQuest.messageBuilder.trackCharge(player.getUniqueId().toString(), AltQuest.LAND_PRICE / 100, null);


                                            ClientDelivery delivery = new ClientDelivery();
                                            delivery.addMessage(sentEvent);
                                            //delivery.addMessage(sentCharge);


                                            MixpanelAPI mixpanel = new MixpanelAPI();
                                            mixpanel.deliver(delivery);
                                        }
                                    } 
					
					else {
                                        //int balance = new User(player).wallet.balance();
                                        if (countEmeralds(player) < (AltQuest.LAND_PRICE/100)) {
                                            player.sendMessage(ChatColor.RED + "You don't have enough money! You need " + ChatColor.BOLD + Math.ceil(((AltQuest.LAND_PRICE/100) - countEmeralds(player))) + ChatColor.RED + " more emeralds.");
                                        } else {
                                            player.sendMessage(ChatColor.RED + "Claim payment failed. Please try again later.");
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                };
                            }
                        });		

                }// end of emerald buy land by @bitcoinjake09
                    } else if (REDIS.get("chunk" + x + "," + z + "owner").equals(player.getUniqueId().toString()) || (isModerator(player) == true)) {
                        if (name.equals("abandon")) {
                            // Abandon land
                            AltQuest.REDIS.del("chunk" + x + "," + z + "owner");
                            AltQuest.REDIS.del("chunk" + x + "," + z + "name");
                        } else if (name.startsWith("transfer ") && name.length() > 9) {
                            // If the name starts with "transfer " and has at least one more character,
                            // transfer land
                            final String newOwner = name.substring(9);
                            player.sendMessage(ChatColor.YELLOW + "Transfering land to " + newOwner + "...");

                            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                            scheduler.runTaskAsynchronously(this, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        UUID newOwnerUUID = UUIDFetcher.getUUIDOf(newOwner);
                                        AltQuest.REDIS.set("chunk" + x + "," + z + "owner", newOwnerUUID.toString());
                                        player.sendMessage(ChatColor.GREEN + "This land now belongs to " + newOwner);
                                    } catch (Exception e) {
                                        player.sendMessage(ChatColor.RED + "Could not find " + newOwner + ". Did you misspell their name?");
                                    }
                                }
                            });

                        } else if (AltQuest.REDIS.get("chunk" + x + "," + z + "name").equals(name)) {
                            player.sendMessage(ChatColor.RED + "You already own this land!");
                        } else {
                            // Rename land
                            player.sendMessage(ChatColor.GREEN + "You renamed this land to " + name + ".");
                            AltQuest.REDIS.set("chunk" + x + "," + z + "name", name);
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED+"Your land name must be 16 characters max");
                }
            } else {
                player.sendMessage(ChatColor.RED+"Your land name must contain only letters and numbers");
            }
        } else {
            player.sendMessage(ChatColor.RED+"Your land must have a name");
        }
    }
    public boolean isOwner(Location location, Player player) {
        if (landIsClaimed(location)) {
            if (REDIS.get("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "owner").equals(player.getUniqueId().toString())) {
                // player is the owner of the chunk
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
// the isPvP function by @bitcoinjake09
public boolean isPvP(Location location) {
		if ((landPermissionCode(location).equals("v")==true)||(landPermissionCode(location).equals("pv")==true))
               return true;// returns true. it is a pvp or public pvp
		else
               return false;//not pvp
    }
// end isPvP by @bitcoinjake09
public static int countEmeralds(Player player) {

        ItemStack[] items = player.getInventory().getContents();
        int amount = 0;
        for (int i=0; i<player.getInventory().getSize(); i++) {
	ItemStack TempStack = items[i];	
	if ((TempStack != null) && (TempStack.getType() != Material.AIR)){          
	if (TempStack.getType().toString() == "EMERALD_BLOCK") {
                amount += (TempStack.getAmount()*9);
            }
	else if (TempStack.getType().toString() == "EMERALD") {
                amount += TempStack.getAmount();
            }
		}
        }
        return amount;
    }//end count emerald in player inventory by @bitcoinjake09
public boolean removeEmeralds(Player player,int amount){
	 int EmCount = countEmeralds(player);
	 int LessEmCount = countEmeralds(player)-amount;
	 double TempAmount=(double)amount;
	int EmsBack=0;
	ItemStack[] items = player.getInventory().getContents();
	if (countEmeralds(player)>=amount){	
		while(TempAmount>0){		
		for (int i=0; i<player.getInventory().getSize(); i++) {
			ItemStack TempStack = items[i];	
			
			if ((TempStack != null) && (TempStack.getType() != Material.AIR)){          	
			
			if ((TempStack.getType().toString() == "EMERALD_BLOCK")&&(TempAmount>=9)) {
		    player.getInventory().removeItem(new ItemStack(Material.EMERALD_BLOCK, 1));	
        			TempAmount=TempAmount-9;
				}
			if ((TempStack.getType().toString() == "EMERALD_BLOCK")&&(TempAmount<9)) {
		    player.getInventory().removeItem(new ItemStack(Material.EMERALD_BLOCK, 1));	
				EmsBack=(9-(int)TempAmount);  //if 8, ems back = 1      		
				TempAmount=TempAmount-TempAmount;
				if (EmsBack>0) {player.getInventory().addItem(new ItemStack(Material.EMERALD, EmsBack));}
				}
			if ((TempStack.getType().toString() == "EMERALD")&&(TempAmount>=1)) {
      		          player.getInventory().removeItem(new ItemStack(Material.EMERALD, 1));		
        			TempAmount=TempAmount-1;
				}
			
			}//end if != Material.AIR
			
		
	}// end for loop
	}//end while loop
	}//end (EmCount>=amount)
	EmCount = countEmeralds(player);
	if ((EmCount==LessEmCount)||(TempAmount==0))
	return true;	
	return false;
}//end of remove emeralds
//start addemeralds to inventory
public boolean addEmeralds(Player player,int amount){
	int EmCount = countEmeralds(player);
	 int moreEmCount = countEmeralds(player)+amount;
	 double bits = (double)amount;
	 double TempAmount=(double)amount;
	int EmsBack=0;
		while(TempAmount>=0){		
			    	if (TempAmount>=9){		
				TempAmount=TempAmount-9;
				player.getInventory().addItem(new ItemStack(Material.EMERALD_BLOCK, 1));
				}
				if (TempAmount<9){	
				TempAmount=TempAmount-1;
				player.getInventory().addItem(new ItemStack(Material.EMERALD, 1));
				}
			EmCount = countEmeralds(player);
			if ((EmCount==moreEmCount))
			return true;
			}//end while loop
	return false;
}
    public boolean canBuild(Location location, Player player) {
        // returns true if player has permission to build in location
        // TODO: Find out how are we gonna deal with clans and locations, and how/if they are gonna share land resources
        if(isModerator(player)) {
            return true;
        } else if (!location.getWorld().getEnvironment().equals(Environment.NORMAL)) {
            // If theyre not in the overworld, they cant build
            return false;
        } else if (landIsClaimed(location)) {
            if(isOwner(location,player)) {
                return true;
            } else if(landPermissionCode(location).equals("p")==true) {
                return true;
            } else if(landPermissionCode(location).equals("pv")==true) {
                return true;// add land permission pv for public Pvp by @bitcoinjake09
            } else if(landPermissionCode(location).equals("c")==true) {
                String owner_uuid=REDIS.get("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "owner");
                System.out.println(owner_uuid);
                String owner_clan=REDIS.get("clan:"+owner_uuid);
                System.out.println(owner_clan);
                String player_clan=REDIS.get("clan:"+player.getUniqueId().toString());
                System.out.println(player_clan);
                if(owner_clan.equals(player_clan)==true) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
    public String landPermissionCode(Location location) {
        // permission codes:
        // p = public
        // c = clan
	// v = PvP(private cant build) by @bitcoinjake09
	// pv= public PvP(can build) by @bitcoinjake09
        // n = no permissions (private)
        if(REDIS.exists("chunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"permissions")) {
            return REDIS.get("chunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"permissions");
        } else {
            return "n";
        }
    }

    public boolean createNewArea(Location location, Player owner, String name, int size) {
        // write the new area to REDIS
        JsonObject areaJSON = new JsonObject();
        areaJSON.addProperty("size", size);
        areaJSON.addProperty("owner", owner.getUniqueId().toString());
        areaJSON.addProperty("name", name);
        areaJSON.addProperty("x", location.getX());
        areaJSON.addProperty("z", location.getZ());
        areaJSON.addProperty("uuid", UUID.randomUUID().toString());
        REDIS.lpush("areas", areaJSON.toString());
        // TODO: Check if redis actually appended the area to list and return the success of the operation
        return true;
    }

    public boolean isModerator(Player player) {
        if(REDIS.sismember("moderators",player.getUniqueId().toString())) {
            return true;
        } else if(ADMIN_UUID!=null && player.getUniqueId().toString().equals(ADMIN_UUID.toString())) {
            return true;
        }
        return false;

    }
    public void updatePlayerTeam(Player player) {

    }

    public void sendWalletInfo(User user) throws ParseException, org.json.simple.parser.ParseException, IOException {
        // int chainHeight = user.wallet.getBlockchainHeight();
        user.wallet.updateBalance();
        // AltQuest.REDIS.del("balance:"+user.player.getUniqueId().toString());
        if(REDIS.get("currency"+user.player.getUniqueId().toString()).equalsIgnoreCase("BTC")) {
            user.player.sendMessage(ChatColor.BOLD+""+ChatColor.GREEN + "Your Bitcoin Address: "+ChatColor.WHITE+user.wallet.address);
		user.player.sendMessage(ChatColor.GREEN + "Confirmed Balance: " +ChatColor.WHITE+ user.wallet.balance/100 + " Bits");
        user.player.sendMessage(ChatColor.GREEN + "Unconfirmed Balance: " +ChatColor.WHITE+user.wallet.unconfirmedBalance/100 + " Bits");
        user.player.sendMessage(ChatColor.GREEN + "Final Balance: "+ChatColor.WHITE + user.wallet.final_balance()/100 + " Bits");
        user.player.sendMessage(ChatColor.BLUE+""+ChatColor.UNDERLINE + "blockchain.info/address/" + user.wallet.address);
	
        }
	else if (REDIS.get("currency"+user.player.getUniqueId().toString()).equalsIgnoreCase("LTC")){ 	
		user.player.sendMessage(ChatColor.BOLD+""+ChatColor.GREEN + "Your LTC Address: "+ChatColor.WHITE+user.LTC_wallet.address);
	user.player.sendMessage(ChatColor.GREEN + "Confirmed Balance: " +ChatColor.WHITE+ user.LTC_wallet.balance + " LTC");
        user.player.sendMessage(ChatColor.GREEN + "Unconfirmed Balance: " +ChatColor.WHITE+user.LTC_wallet.unconfirmedBalance + " LTC");
        user.player.sendMessage(ChatColor.GREEN + "Final Balance: "+ChatColor.WHITE + user.LTC_wallet.final_balance() + " LTC");
        // user.player.sendMessage(ChatColor.YELLOW + "On-Chain Wallet Info:");
        //  user.player.sendMessage(ChatColor.YELLOW + " "); // spacing to let these URLs breathe a little
        user.player.sendMessage(ChatColor.BLUE+""+ChatColor.UNDERLINE + "http://ltc.blockr.io/address/info/" + user.LTC_wallet.address);
		}
	else if (REDIS.get("currency"+user.player.getUniqueId().toString()).equalsIgnoreCase("DOGE")){ 	
		user.player.sendMessage(ChatColor.BOLD+""+ChatColor.GREEN + "Your DOGE Address: "+ChatColor.WHITE+user.DOGE_wallet.address);
		user.player.sendMessage(ChatColor.GREEN + "Confirmed Balance: " +ChatColor.WHITE+ user.DOGE_wallet.balance + " DOGE");
        user.player.sendMessage(ChatColor.GREEN + "Unconfirmed Balance: " +ChatColor.WHITE+user.DOGE_wallet.unconfirmedBalance + " DOGE");
        user.player.sendMessage(ChatColor.GREEN + "Final Balance: "+ChatColor.WHITE + user.DOGE_wallet.final_balance() + " DOGE");
        // user.player.sendMessage(ChatColor.YELLOW + "On-Chain Wallet Info:");
        //  user.player.sendMessage(ChatColor.YELLOW + " "); // spacing to let these URLs breathe a little
        user.player.sendMessage(ChatColor.BLUE+""+ChatColor.UNDERLINE + "https://www.dogechain.info/address/" + user.DOGE_wallet.address);
		}
	else {
            user.player.sendMessage(ChatColor.BOLD+""+ChatColor.GREEN + "Your Testnet Address: "+ChatColor.WHITE+user.wallet.address);
        }
        
        //    user.player.sendMessage(ChatColor.YELLOW + " ");
        //      user.player.sendMessage(ChatColor.BLUE+""+ChatColor.UNDERLINE + "live.blockcypher.com/btc/address/" + user.wallet.address);
        //      user.player.sendMessage(ChatColor.YELLOW + " ");
//        user.player.sendMessage(ChatColor.YELLOW+"Blockchain Height: " + Integer.toString(chainHeight));

    };
    public boolean landIsClaimed(Location location) {
        return REDIS.exists("chunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"owner");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // we don't allow server commands (yet?)
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            // PLAYER COMMANDS
            if(cmd.getName().equalsIgnoreCase("land")) {
                if(args[0].equalsIgnoreCase("claim")) {
                    Location location=player.getLocation();
                    try {
                        claimLand(args[1],location.getChunk(),player);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        player.sendMessage(ChatColor.RED+"Land claim failed. Please try again later.");
                        return true;
                    } catch (org.json.simple.parser.ParseException e) {
                        e.printStackTrace();
                        player.sendMessage(ChatColor.RED+"Land claim failed. Please try again later.");
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        player.sendMessage(ChatColor.RED+"Land claim failed. Please try again later.");
                        return true;
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("permission")) {
                    Location location=player.getLocation();
                    int x=location.getChunk().getX();
                    int z=location.getChunk().getZ();
                    if(isOwner(location,player)) {
                        String landname= REDIS.get("chunk"+x+","+z+"name");

                        if(args[1].equalsIgnoreCase("public")) {
                            REDIS.set("chunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"permissions","p");
                            player.sendMessage(ChatColor.GREEN+"the land "+landname+" is now public");
                            return true;// added pvp and public pvp by @bitcoinjake09
                        } else if(args[1].equalsIgnoreCase("pvp")) {
                            REDIS.set("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "permissions", "v");
                            player.sendMessage(ChatColor.GREEN + "the land " + landname + " is now pvp");
                            return true;
                        } else if((args[1].equalsIgnoreCase("pvp"))&&(args[2].equalsIgnoreCase("public"))){
                            REDIS.set("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "permissions", "pv");
                            player.sendMessage(ChatColor.GREEN + "the land " + landname + " is now public pvp");
                            return true;// end pvp by @bitcoinjake09
                        } else if(args[1].equalsIgnoreCase("clan")) {
                            REDIS.set("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "permissions", "c");
                            player.sendMessage(ChatColor.GREEN + "the land " + landname + " is now clan-owned");
                            return true;
                        } else if(args[1].equalsIgnoreCase("private")) {
                            REDIS.del("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "permissions");
                            player.sendMessage(ChatColor.GREEN + "the land " + landname + " is now private");
                            return true;
                        } else {
                            return false;
                        }

                    }
			else {
                        player.sendMessage(ChatColor.RED+"Only the owner of this location can change its permissions.");
                        return true;
                    }
                }
            }
	//CHANGE CURRENCY BY BITCOINJAKE09
		 if(cmd.getName().equalsIgnoreCase("currency")) {
			
			if(args[0].equalsIgnoreCase("emerald")) {
				REDIS.set("currency"+player.getUniqueId().toString(), "emerald");
			player.sendMessage(ChatColor.GREEN+"Currency changed to " + REDIS.get("currency"+player.getUniqueId().toString()));                        
				
				}
			else if(args[0].equalsIgnoreCase("btc")) {
				REDIS.set("currency"+player.getUniqueId().toString(), "BTC");
				player.sendMessage(ChatColor.GREEN+"Currency changed to " + REDIS.get("currency"+player.getUniqueId().toString()));
	                                               
				}
			else if(args[0].equalsIgnoreCase("ltc")) {
				REDIS.set("currency"+player.getUniqueId().toString(), "LTC");
				player.sendMessage(ChatColor.GREEN+"Currency changed to " + REDIS.get("currency"+player.getUniqueId().toString()));
	                                               
				}
			else if(args[0].equalsIgnoreCase("eth")) {
				REDIS.set("currency"+player.getUniqueId().toString(), "ETH");
				player.sendMessage(ChatColor.GREEN+"Currency changed to " + REDIS.get("currency"+player.getUniqueId().toString()));
	                                               
				}
			else if(args[0].equalsIgnoreCase("doge")) {
				REDIS.set("currency"+player.getUniqueId().toString(), "DOGE");
				player.sendMessage(ChatColor.GREEN+"Currency changed to " + REDIS.get("currency"+player.getUniqueId().toString()));
	                                               
				}
		  else {
                    player.sendMessage(ChatColor.RED+"There was a problem changing your currency.");
                } 

		return true; 	
		}
		if(cmd.getName().equalsIgnoreCase("getcurrency")) {player.sendMessage(ChatColor.GREEN+"Currency currently set to " + REDIS.get("currency"+player.getUniqueId().toString()));			return true;}
		//CHANGE CURRENCY ENDS HERE	
		// start get ExRate
		if(cmd.getName().equalsIgnoreCase("exrate")) {
			double amount = Double.parseDouble(args[0]);
			if(args[1].equalsIgnoreCase("btc"))
			{
				if (args[2].equalsIgnoreCase("doge"))
				{
					player.sendMessage(ChatColor.YELLOW+""+getSwapValue((amount*0.00000001), "btc", "doge"));
				}
				else if (args[2].equalsIgnoreCase("ltc"))
				{
					player.sendMessage(ChatColor.YELLOW+""+getSwapValue((amount*0.00000001), "btc", "ltc"));
				}

			}
			else if(args[1].equalsIgnoreCase("doge")) 
			{
				if (args[2].equalsIgnoreCase("btc"))
				{
					player.sendMessage(ChatColor.YELLOW+""+getSwapValue(amount, "doge", "btc"));
				}
				else if (args[2].equalsIgnoreCase("ltc"))
				{
					player.sendMessage(ChatColor.YELLOW+""+getSwapValue(amount, "doge", "ltc"));
				}	
	                	                               
			}
			else if(args[1].equalsIgnoreCase("ltc")) 
			{
				if (args[2].equalsIgnoreCase("btc"))
				{
					player.sendMessage(ChatColor.YELLOW+""+getSwapValue(amount, "ltc", "btc"));
				}
				else if (args[2].equalsIgnoreCase("doge"))
				{
					player.sendMessage(ChatColor.YELLOW+""+getSwapValue(amount, "ltc", "doge"));
				}	
	                	                               
			}
	return true;
}//end get ExRate
            if(cmd.getName().equalsIgnoreCase("clan")) {

                if(args[0].equals("new")) {
                    if(!args[1].isEmpty()) {
                        // check that desired clan name is alphanumeric
                        boolean hasNonAlpha = args[1].matches("^.*[^a-zA-Z0-9 ].*$");
                        if(!hasNonAlpha) {
                            // 16 characters max
                            if(args[1].length()<=16) {

                                if(REDIS.get("clan:"+player.getUniqueId().toString())==null) {
                                    if(!REDIS.sismember("clans",args[1])) {
                                        REDIS.sadd("clans",args[1]);
                                        REDIS.set("clan:"+player.getUniqueId().toString(),args[1]);
                                        player.sendMessage(ChatColor.GREEN+"Congratulations! you are the founder of the "+args[1]+" clan");
                                        return true;
                                    } else {
                                        player.sendMessage(ChatColor.RED+"A clan with the name '"+args[1]+"' already exists.");
                                        return true;
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED+"You already belong to the clan "+REDIS.get("clan"+player.getUniqueId().toString()));
                                    return true;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED+"Error: clan name must have 16 characters max");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED+"Your clan name must only contain letters and numbers");
                            return true;
                        }

                    } else {
                        player.sendMessage(ChatColor.RED+"Usage: /clan new <your desired name>");
                        return true;
                    }

                }
                if(args[0].equals("invite")) {
                    // check that argument is not empty


                    if(!args[1].isEmpty()) {
                        // TODO: you shouldn't be able to invite yourself
                        // check that player is in a clan
                        if(REDIS.exists("clan:"+player.getUniqueId().toString())) {
                            String clan=REDIS.get("clan:"+player.getUniqueId().toString());
                            // check if user is in the uuid database
                            if (REDIS.exists("uuid:" + args[1]) == true) {
                                // check if player already belongs to a clan
                                String uuid=REDIS.get("uuid:"+args[1]);
                                if (REDIS.exists("clan:" + uuid) == false) {
                                    // check if player is already invited to the clan
                                    if (REDIS.sismember("invitations:"+clan,uuid)==false) {
                                        REDIS.sadd("invitations:" + clan, uuid);
                                        player.sendMessage(ChatColor.GREEN+"You invited " +args[1]+ " to the "+clan+" clan.");
                                        if(Bukkit.getPlayerExact(args[1])!=null) {
                                            Player invitedplayer = Bukkit.getPlayerExact(args[1]);
                                            invitedplayer.sendMessage(ChatColor.GREEN+player.getDisplayName()+" invited you to the "+clan+" clan");
                                        }
                                        return true;
                                    } else {
                                        player.sendMessage(ChatColor.RED+"Player "+args[1]+" is already invited to the clan and must accept the invitation");
                                        return true;
                                    }

                                } else {
                                    if(REDIS.get("clan:"+uuid).equals(clan)==true) {
                                        player.sendMessage(ChatColor.RED + "Player " + args[1] + " already belongs to the clan "+clan);

                                    } else {
                                        player.sendMessage(ChatColor.RED + "Player " + args[1] + " already belongs to a clan.");

                                    }
                                    return true;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "User " + args[1] + " does not play on this server");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED+"You don't belong to a clan");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED+"Usage: /clan invite <player nickname>");
                        return true;
                    }
                }
                if(args[0].equals("join")) {
                    // check that argument is not empty
                    if(!args[1].isEmpty()) {
                        // check that player is invited to the clan he wants to join
                        if(REDIS.sismember("invitations:"+args[1],player.getUniqueId().toString())) {
                            // user is invited to join
                            if(REDIS.get("clan:"+player.getUniqueId().toString())==null) {
                                // user is not part of any clan
                                REDIS.set("clan:"+player.getUniqueId().toString(),args[1]);
                                player.sendMessage(ChatColor.GREEN+"You are now part of the "+REDIS.get("clan:"+player.getUniqueId().toString())+" clan!");
                                return true;
                            } else {
                                player.sendMessage(ChatColor.RED+"You already belong to the clan "+REDIS.get("clan:"+player.getUniqueId().toString()));
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED+"You are not invited to join the "+args[1]+" clan.");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED+"Usage: /clan join <clan name>");
                        return true;
                    }
                }
                if(args[0].equals("kick")) {
                    if(!args[1].isEmpty()) {
                        // check if player is in the uuid database

                        if(REDIS.exists("uuid:"+args[1])==true) {
                            String uuid=REDIS.get("uuid:"+args[1]);
                            // check if player belongs to a clan
                            if(REDIS.exists("clan:"+player.getUniqueId().toString())==true) {
                                String clan=REDIS.get("clan:"+player.getUniqueId().toString());
                                // check that kicker and player are in the same clan
                                if(REDIS.get("clan:"+uuid).equals(clan)) {
                                    REDIS.del("clan:"+REDIS.get("uuid"+args[1]));
                                    player.sendMessage(ChatColor.GREEN+"Player "+args[1]+" was kicked from the "+clan+" clan.");
                                    return true;
                                } else {
                                    player.sendMessage(ChatColor.RED+"Player "+args[1]+" is not a member of the clan "+clan);
                                    return true;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED+"You don't belong to any clan.");
                                return true;
                            }

                        } else {
                            player.sendMessage(ChatColor.RED+"Player "+args[1]+" does not play on this server.");
                            return true;
                        }
                    }
                }
                if(args[0].equals("leave")) {
                    if(REDIS.exists("clan:"+player.getUniqueId().toString())==true) {
                        // TODO: when a clan gets emptied, should be removed from the "clans" set
                        player.sendMessage(ChatColor.GREEN+"You are no longer part of the "+REDIS.get("clan:"+player.getUniqueId().toString())+" clan");
                        REDIS.del("clan:"+player.getUniqueId().toString());
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED+"You don't belong to a clan.");
                        return true;
                    }
                }
                return false;
            }
		
            if(cmd.getName().equalsIgnoreCase("wallet")) {
                try {
                    User user=new User(player);
                    sendWalletInfo(user);

                } catch (ParseException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED+"There was a problem reading your wallet.");
                } catch (org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED+"There was a problem reading your wallet.");

                } catch (IOException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED+"There was a problem reading your wallet.");

                }

                return true;
            }
            if(cmd.getName().equalsIgnoreCase("transfer")) {
               if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("BTC")){
		if(args.length == 2) {
                    final int sendAmount = Integer.valueOf(args[0])*100;
		    Wallet fromWallet = null;
                    try {
                        fromWallet = new User(player).wallet;
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    } catch (org.json.simple.parser.ParseException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        if(fromWallet != null && fromWallet.balance() >= sendAmount) {
                            player.sendMessage(ChatColor.YELLOW+"Sending " + args[0] + " Bits to "+args[1]+"...");
                            for(final OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                System.out.println(offlinePlayer);
                                if(offlinePlayer.getName()!=null && args[1]!=null && offlinePlayer.getName().equals(args[1])) {
                                    final Wallet finalFromWallet = fromWallet;
                                    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                                    final Wallet toWallet = new User(offlinePlayer.getPlayer()).wallet;
                                    REDIS.expire("balance"+player.getUniqueId().toString(),5);
                                    scheduler.runTaskAsynchronously(this, new Runnable() {
                                        @Override
                                        public void run() {
                                            try {

                                                if (finalFromWallet.transaction(sendAmount, toWallet)) {
                                                    player.sendMessage(ChatColor.GREEN + "Succesfully sent " + sendAmount / 100 + " Bits to " + offlinePlayer.getName() + ".");
                                                    if (offlinePlayer.isOnline()) {
                                                        offlinePlayer.getPlayer().sendMessage(ChatColor.GREEN + "" + player.getName() + " just sent you " + sendAmount / 100 + " Bits!");
                                                    }
                                                } else {
                                                    player.sendMessage(ChatColor.RED + "Transaction failed. Please try again in a few moments.");
                                                }

                                            } catch (IOException e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                    });

                                    updateScoreboard(player);
                                    return true;
                                }
				
                            }
                            // validate e-mail address
                            String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
                            java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
                            java.util.regex.Matcher m = p.matcher(args[0]);
                            if(m.matches()) {
                                // TODO: send money through xapo

                            } else {
                                try {

                                    Wallet toWallet = new Wallet(args[1]);

                                    if(fromWallet.transaction(sendAmount,toWallet)) {
                                        player.sendMessage(ChatColor.GREEN+"Succesfully sent "+args[0]+" Bits to external address.");
                                        updateScoreboard(player);
                                    } else {
                                        player.sendMessage(ChatColor.RED+"Transaction failed. Please try again in a few moments.");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (org.json.simple.parser.ParseException e) {
                                    e.printStackTrace();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                            }
                            return true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (org.json.simple.parser.ParseException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }//end of transfer bitcoin
		else if (REDIS.get("currency"+player.getUniqueId().toString()).equalsIgnoreCase("emerald")){
		if(args.length == 2) {
                    final int sendAmount = Integer.valueOf(args[0]);
		    
                    try {
                        if(countEmeralds(player) >= sendAmount) {
                            player.sendMessage(ChatColor.YELLOW+"Sending " + args[0] + " Emerlads to "+args[1]+"...");
                            for(final OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                System.out.println(offlinePlayer);
                                if(offlinePlayer.getName()!=null && args[1]!=null && offlinePlayer.getName().equals(args[1])) {

                                    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                                    scheduler.runTaskAsynchronously(this, new Runnable() {
                                        @Override
                                        public void run() {
                                            

                                                if (((removeEmeralds(player,sendAmount))&&(addEmeralds(offlinePlayer.getPlayer(),sendAmount)))) {
                                                    player.sendMessage(ChatColor.GREEN + "Succesfully sent " + sendAmount + " Emeralds to " + offlinePlayer.getName() + ".");
                                                    if (offlinePlayer.isOnline()) {
                                                        offlinePlayer.getPlayer().sendMessage(ChatColor.GREEN + "" + player.getName() + " just sent you " + sendAmount + " Emeralds!");
                                                    }
                                                } else {
                                                    player.sendMessage(ChatColor.RED + "Transaction failed. Please try again in a few moments.");
                                                }

                                           
                                        }
                                    });

                                    updateScoreboard(player);
                                    return true;
                                }
				
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (org.json.simple.parser.ParseException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }//end of transfer emeralds
		}

            // MODERATOR COMMANDS
            if (isModerator(player)) {
                // COMMAND: MOD
                if (cmd.getName().equalsIgnoreCase("butcher")) {
                    for(World w:Bukkit.getWorlds()) {
                        List<Entity> entities = w.getEntities();
                        int villagerskilled=0;
                        for ( Entity entity : entities){
                            if ((entity instanceof Villager)) {
                                villagerskilled=villagerskilled+1;
                                ((Villager)entity).remove();
                                System.out.println("[butcher] removed "+entity.getName());

                            }
                        }
                    }

                }
                if (cmd.getName().equalsIgnoreCase("killAllVillagers")) {
                    killAllVillagers();
                }
                if (cmd.getName().equalsIgnoreCase("mod")) {
                    Set<String> allplayers=REDIS.smembers("players");
                    if(args[0].equals("add")) {
                        // Sub-command: /mod add
                        if(REDIS.get("uuid"+args[1])!=null) {
                            UUID uuid=UUID.fromString(REDIS.get("uuid"+args[1]));
                            REDIS.sadd("moderators",uuid.toString());
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED+"Cannot find player "+args[1]);
                            return true;
                        }
                    } else if(args[0].equals("del")) {
                        // Sub-command: /mod del
                        if(REDIS.get("uuid"+args[1])!=null) {
                            UUID uuid=UUID.fromString(REDIS.get("uuid"+args[1]));
                            REDIS.srem("moderators",uuid.toString());
                            return true;
                        }
                        return false;
                    } else if(args[0].equals("list")) {
                        // Sub-command: /mod list
                        Set<String> moderators=REDIS.smembers("moderators");
                        for(String uuid:moderators) {
                            sender.sendMessage(ChatColor.YELLOW+REDIS.get("name"+uuid));
                        }
                        return true;
                    }
                }
                if (cmd.getName().equalsIgnoreCase("ban") && args.length==1) {
                    if(REDIS.get("uuid"+args[0])!=null) {
                        UUID uuid=UUID.fromString(REDIS.get("uuid"+args[0]));
                        REDIS.sadd("banlist",uuid.toString());
                        Player kickedout=Bukkit.getPlayer(args[0]);
                        if(kickedout!=null) {
                            kickedout.kickPlayer("Sorry.");
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED+"Can't find player "+args[0]);
                        return true;
                    }

                }
                if (cmd.getName().equalsIgnoreCase("unban") && args.length==1) {
                    if(REDIS.get("uuid"+args[0])!=null) {
                        UUID uuid=UUID.fromString(REDIS.get("uuid"+args[0]));
                        REDIS.srem("banlist",uuid.toString());

                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED+"Can't find player "+args[0]);
                        return true;
                    }

                }
                if (cmd.getName().equalsIgnoreCase("banlist")) {
                    Set<String> banlist=REDIS.smembers("banlist");
                    for(String uuid:banlist) {
                        sender.sendMessage(ChatColor.YELLOW+REDIS.get("name"+uuid));
                    }
                    return true;

                }
                if (cmd.getName().equalsIgnoreCase("spectate") && args.length == 1) {

                    if(Bukkit.getPlayer(args[0]) != null) {
                        ((Player) sender).setGameMode(GameMode.SPECTATOR);
                        ((Player) sender).setSpectatorTarget(Bukkit.getPlayer(args[0]));
                        success(((Player) sender), "You're now spectating " + args[0] + ".");
                    } else {
                        error(((Player) sender), "Player " + args[0] + " isn't online.");
                    }
                    return true;
                }
                if (cmd.getName().equalsIgnoreCase("emergencystop")) {
                    StringBuilder message = new StringBuilder();
                    message.append(sender.getName())
                            .append(" has shut down the server for emergency reasons");

                    if (args.length > 0) {
                        message.append(": ");
                        for (String word : args) {
                            message.append(word).append(" ");
                        }
                    }
                    for (Player currentPlayer : Bukkit.getOnlinePlayers()) {
                        currentPlayer.kickPlayer(message.toString());
                    }

                    Bukkit.shutdown();
                    return true;
                }
		if (cmd.getName().equalsIgnoreCase("emergencyrestart")) {
                    StringBuilder message = new StringBuilder();
                    message.append(sender.getName())
                            .append(" has restarted the server for emergency reasons, should be back up shortly.");

                    if (args.length > 0) {
                        message.append(": ");
                        for (String word : args) {
                            message.append(word).append(" ");
                        }
                    }
                    for (Player currentPlayer : Bukkit.getOnlinePlayers()) {
                        currentPlayer.kickPlayer(message.toString());
                    }

                    Bukkit.reload();
                    return true;
                }
            } else {
                sender.sendMessage("You don't have enough permissions to execute this command!");
            }
            if (cmd.getName().equalsIgnoreCase("faucet")) {
                User user= null;
                try {
                    user = new User(player);
                    if(user.wallet.getTestnetCoins()==true) {
                        player.sendMessage(ChatColor.GREEN+"Some testnet coins were delivered to your wallet.");
                    } else {
                        player.sendMessage(ChatColor.RED+"There was an error getting testnet coins.");
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}

