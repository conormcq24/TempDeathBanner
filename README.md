## TempDeathBanner
Temp Death Banner is a spigot minecraft plugin created by Conor McQuillan, the goal of the plugin is to create a feeling of suspense when you are navigating throughout your minecraft server similar to hardcore but without the lasting consequences. This plugin has several configurable variables that allow you to set a temporary amount of time that players will be automatically banned for upon their death. This time increments or multiplys depending on configuration everytime the player dies. 1 death could be an hour, 2 could be 2 hours, 3 could be four. and so on

### Supported Minecraft Versions:
For TempDeathBanner 1.0.0 Minecraft Version 1.21.0 through 1.21.4 are supported at this moment.

### Commands
- /tdb resetdeathsall
- /tdb resetdeaths <playername>
- /tdb showdeathboard
- /tdb closedeathboard
### Permissions
- tempdeathbanner.use (access to all tdb commands)
- tempdeathbanner.resetdeaths (access to specific player reset)
- tempdeathbanner.resetdeathsall (access to resetting all players at once)
- tempdeathbanner.showdeathboard (access to show deathboard)
- tempdeathbanner.closedeathboard (access to close deathboard)
### Configuration File
##### incOrMultiply
###### (Variable that chooses whether you want to increment or multiply your ban lengths false for multiplier, true for increment)
##### incrementByXMilliseconds
###### (The length of your initial ban in milliseconds if you are using increment)
##### initialBanLength
###### (The length of your initial ban in milliseconds if you are using multiply)
##### multiplier
###### (The amount you multiply the last ban length by when using multiplier)
##### message/graceMessage/banLength container
###### (A series of configurable broadcast messages, with their own formatting shorthand symbols (can find guide in comments of configuration file)
##### graceLives
###### (Amount of lives the player is allowed to expend before bans are applied to them)
##### maximumBanLength
###### (An optional cap to how long players can be banned in milliseconds to prevent bans from growing too high)
##### scoreboardTitle
###### (Title that appears in the players showDeathBoard command can be renamed here)
##### permanenetDeathBoard
###### (decides whether or not the deathboard remains open or closed according to user preferences regardless of whether or not they have left and rejoined the server)






