# Demolition Synergy

This repository is renewed.
Old one is taken down. Because of severe malware infection.
Please see ***Very Important Notice*** located at the bottom.

Voxel Engine using latest LWJGL (The Lightweight Java Game Library).
Audio material was provided from freesound.org by Erokia.

Please checkout Erokia & (his) work at: https://freesound.org/people/erokia

Demolition Synergy showing multiplayer setup.
![Alt text](/misc/DSynergy8.png?raw=true "Demolition Synergy showing multiplayer setup")

# Java Version
Game (client) requires Java 11 LTS (11.0.1 or later).
Java 11 Installer could be downloaded from:
https://mega.nz/file/DYUwUS5J#ftHxZFKlsyaIlJJQ9E4cmpTrZG9byW9P_QeQNRNrIhQ

# Verbose Description
Robust (yet still uncomplete) Game Engine writen in Java. Free & Open-Source.
Project started back in a day on October 15, 2017. And possible earlier.

Key features: 
- Dynamic lights.
- Water Reflections.
- Shadow Effects.
- Voxel based. Minecraft-like. Not a Minecraft-clone.
- Varying update with one tick per update or two ticks per update.
  (inspired by classic Doom port Zandronum)
	
Unique features:
- Console (similar to Quake III Arena) with ingame commands. 
- Ambient soundtracks (credits to Erokia). 
- Random level building & manual level building via Editor.
- Tweaked and unique renderer. Instanced rendering.
- Highly modular and support visual & GLSL modifications.
- Editor, Single player & Multiplayer as different modes.
- Player Guid oriented protocol where Guid is based on hardware properties.

![Alt text](/misc/DSynergy1.png?raw=true "Light trough air")
![Alt text](/misc/DSynergy2.png?raw=true "Random Level Medium underwater")
![Alt text](/misc/DSynergy3.png?raw=true "Random Level Huge")
![Alt text](/misc/DSynergy4.png?raw=true "RPG Camera #1")
![Alt text](/misc/DSynergy5.png?raw=true "RPG Camera #2")
![Alt text](/misc/DSynergy6.png?raw=true "Various effects")
![Alt text](/misc/DSynergy9.png?raw=true "Multiplayer #1")
![Alt text](/misc/DSynergy10.png?raw=true "Multiplayer #2")

# How To Build
Build was coded in Apache NetBeans IDE 21. Requires Java JDK 11 (or later).
In order to build the project you are gonna need NetBeans IDE 16 (or later) and following libraries:
1. LWJGL 3.3.5 with JOML 1.10.7 (not installed, follow the steps below),
2. Jorbis OGG Decoder (for OGG audio files) (installed),
3. GapList & BigList (for Block lists) (installed),
4. Apache logger (Log4J) with dependecies (installed),
5. Google Gson (installed),
6. Apache MINA, for networking (installed).

Download latest library LWJGL (3.3.4 at this time) from:
https://www.lwjgl.org/customize (this project is using Minimal OpenGL)

Put [MoveAllFiles.bat](/utils/MoveAllFiles.bat) inside the directory with downloaded & extracted zip content.
Run [MoveAllFiles.bat](/utils/MoveAllFiles.bat) and wait brief amount of time (less than 3 seconds) to sort out the files under directories.
Put rest unsorted  files into classpath directory.

In Apache NetBeans IDE 16 (or later) create new library
by specifying classpath, sources & javadocs in that order.

Add library to the project.

Done.
```
# Source Code Structure
└───rs
    └───alexanderstojanovich
        └───evg
            ├───audio
            ├───cache
            ├───chunk
            ├───core
            ├───critter
            ├───intrface
            ├───level
            ├───light
            ├───location
            ├───main
            ├───models
            ├───net
            ├───resources
            ├───shaders
            ├───texture
            ├───util
            └───weapons
```
- audio		=> Music/soundFX effects.
- cache 	=> Contains chunk ssd/hdd disk caching.
- chunk 	=> Series of blocks for instanced rendering.
- core  	=> Camera, frame buffer, water reflection, window class etc.
- critter 	=> Observer (camera interface), player & npc.
- intrface 	=> Ingame interface & 2d rendering components.
- level 	=> Level container, level actors, level editor, random level generator.
- light 	=> Light projection on the screen, light source(s).
- location 	=> Block location matrix 256 x 256 x 256 on X,Z & Y axis.
- main 		=> Main inependable classes, `Game` (client with main loop), `GameRenderer` (separate thread) & `GameObject` links them together.
			   `GameServer` is relevant class for hosting a server on the PC. UDP (User datagram) protocol is being used with 13667-13668 ports. 
- models 	=> 3d looking models and their part(s) - vertex, mesh, material.
- net 		=> DSObject to be send over network - Request and Response. Stuff for multiplayer.
- resources => Ingame block (models) cube.txt (deprecated) & cubex.txt.
- shaders 	=> Shader - Vertex & Fragment GLSL, Shader Program - binds them together.
- texture 	=> Ingame textures & texture store hashmap.
- util 		=> Various utils & auxillary methods including logging, math, image & model utils.
- weapons 	=> Planned First person shooter weapons & gameplay (TODO).

# Game Assets
Project assets come from various sources.

Character models are from Minecraft.
Guns are originating from potentially leaked or ripped sources from Call of Duty 4 (COD4).
Blender project could be downloaded from Author's mediafire account:
https://www.mediafire.com/file/rilu34pbbzr48zc/DS42Weapons.zip/file

Author recently created player models which could obtained from:
https://www.mediafire.com/file/bmqvia3qvwpee0s/DS51PlayerModels.zip/file

Sounds were made by Erokia and modified by Author.
Font is 'Jet Brains Mono', free & open typeface.
As font (image) atlas Author used, program 'F2IBuilder' which could be obtained from following Url:
https://f2ibuilder.dukitan.com.br/

# Mentions
Author: Ermac(MKIII); 
Testers: 13;
Credits: Erokia

# Very Important Notice
Old repository of Demolition Synergy suffered severe malware Trojan infection and was unsalvagable.
Author is certain that malware was not prior date July 12, 2024.
This renewed repository stands since August 6, 2024.
Author apologizes for concerns and any damage.