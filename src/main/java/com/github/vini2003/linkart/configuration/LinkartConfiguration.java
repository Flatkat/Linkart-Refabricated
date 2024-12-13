package com.github.vini2003.linkart.configuration;

import eu.midnightdust.lib.config.MidnightConfig;

public class LinkartConfiguration extends MidnightConfig {
    public static final String LINKART = "linkart";
    @Entry(category = LINKART) public static int pathfindingDistance = 6;
    @Entry(category = LINKART) public static float velocityMultiplier = 1F;
    @Entry(category = LINKART) public static int collisionDepth = 8;
    @Entry(category = LINKART) public static double distance = 1.2d;
    @Entry(category = LINKART) public static boolean chunkloading = false;
    @Entry(category = LINKART) public static int chunkloadingRadius = 3;
}
