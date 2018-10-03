## General Information
Levelhead is a Minecraft Mod created for the Hypixel Network `mc.hypixel.net` to show a player's network level above their head.  The mod can be found in [Hyperium Client](https://hyperium.cc).  

## Configuration
`/levelhead` - Primary configuration command. Opens configuration GUI

`/levelhead dumpcache` - Dumps local in ram cache for players’ levels. Reloads from servers.


### Gui 
`Levelhead: [State]` - Toggle mod on and off

`Show self` - Show your own level above your head


#### Text / Level configuration 

`Text: Level` - Format used for displaying level

**Modes**

`Chroma` - Toggles chroma. When disabled the `Rotate` button is available.  This will rotate the color between all colors in the Minecraft Color Code system.

`RGB` - Use RGB for color configuration.

`Standard` - Use Minecraft color codes for color configuration.



`Set Prefix` - Allows users to specify a custom prefix instead of `Level`

## Technical Information
Due to the nature of the mod, millions of requests are sent to my server every day. As a result, heavy caching rules are in place. All levels are cached for a minimum of 2 days on Cloudflare’s network. The cache time on my website is based on how many requests it is receiving. Less requests will result is shorter cache times. On average, levels are cached for 4 days on my website. When a cache update is called, the player’s level is retrieved from the [Hypixel Public Api](https://api.hypixel.net) and updated across Cloudflare’s network. 

## Support the project
You can support the project by purchasing a custom Levelhead message. More information and purchase details can be found here: https://sk1er.club/customlevelhead

## Analytics
This mod tracks who uses it. General information is sent to my analytic servers. This information includes: Minecraft UUID, Minecraft Version, Mod Version and Minecraft Forge Version

All analytics can be viewed here: https://sk1er.club/graphs/level_head

