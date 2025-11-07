package com.barracudaroutes;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BarracudaRoutesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BarracudaRoutesPlugin.class);
		RuneLite.main(args);
	}
}