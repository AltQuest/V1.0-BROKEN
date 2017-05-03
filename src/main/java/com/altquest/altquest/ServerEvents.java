package com.altquest.altquest;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

/**
 * Created by cristian on 3/20/16.
 */
public class ServerEvents implements Listener {
    AltQuest altQuest;

    public ServerEvents(AltQuest plugin) {

        altQuest = plugin;

    }
    @EventHandler
    public void onServerListPing(ServerListPingEvent event)
    {

        event.setMotd(ChatColor.GOLD + ChatColor.BOLD.toString() + "Alt" + ChatColor.GRAY + ChatColor.BOLD.toString() + "Quest"+ChatColor.RESET+" - The server that runs on Altcoin ");
    }
}
