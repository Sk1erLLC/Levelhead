## General Information
Levelhead is a Minecraft Mod created for the Hypixel Network `mc.hypixel.net` to show a player's network level or other stats above their head.

## Configuration
`/levelhead` - Primary configuration command. Opens configuration GUI

`/levelhead dumpcache` - Dumps local cache for players’ levels. Reloads from servers.

`/levelhead reauth` - Refreshes the current purchase data for your player

### Gui 

`MasterToggle` - Toggle mod on and off

`Offset` - Shifts the head displays further upwards (only for above head)

#### Text / Level configuration

`Show self` - Show your own level above your head

`Type` - Stat shown for a given player

`Prefix` - Allows users to specify a custom prefix instead of `Level`

#### Colors

`Dropdown` - Color presets based on Minecraft's chat colors. Includes RGB and Chroma for above head and tab

`Color Picker` - Full color picker which allows for direct control over the rgb values of the color. (Only for above head and tab)

## Technical Information
Due to the nature of the mod, millions of requests are sent to my server every day. As a result, heavy caching rules are in place. All levels are cached for a minimum of 2 days on Cloudflare’s network. The cache time on my website is based on how many requests it is receiving. Less requests will result is shorter cache times. On average, levels are cached for 4 days on my website. When a cache update is called, the player’s level is retrieved from the [Hypixel Public Api](https://api.hypixel.net) and updated across Cloudflare’s network. 

## Support the project
You can support the project by purchasing a custom Levelhead message. More information and purchase details can be found here: https://sk1er.club/customlevelhead

## Analytics
This mod tracks who uses it. General information is sent to my analytic servers. This information includes: Minecraft UUID, Minecraft Version, Mod Version and Minecraft Forge Version

All analytics can be viewed here: https://sk1er.club/graphs/level_head

## Yourkit
YourKit supports open source projects with innovative and intelligent tools 
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/) [YourKit .NET Profiler]("https://www.yourkit.com/.net/profiler/) and[YourKit YouMonitor](https://www.yourkit.com/youmonitor/)

