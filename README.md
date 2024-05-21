# DemolitionSynergy
Voxel Engine using latest LWJGL.
Audio material was provided from freesound.org by Erokia.

Demolition Synergy showing multiplayer setup.
![Alt text](/misc/DSynergy8.png?raw=true "Demolition Synergy showing multiplayer setup")

# Verbose Description
Lightwight Game Engine writen in Java. 
Features: Lighting, Water Reflections, Shadow Effects, Voxel based. 
With it's own Console & ingame commands. 
Has ambient soundtracks. 
Random level building & manual level building via Editor.
Many tweaked optimizations in rendering. Instanced rendering.
Highly modular and support visual & GLSL modifications.
Has Editor, Single player & Multiplayer as different modes.

![Alt text](/misc/DSynergy1.png?raw=true "Light trough air")
![Alt text](/misc/DSynergy2.png?raw=true "Random Level Medium underwater")
![Alt text](/misc/DSynergy3.png?raw=true "Random Level Huge")
![Alt text](/misc/DSynergy4.png?raw=true "RPG Camera #1")
![Alt text](/misc/DSynergy5.png?raw=true "RPG Camera #2")
![Alt text](/misc/DSynergy6.png?raw=true "Various effects")

# How To Build
Build was coded in Apache NetBeans 16. Requires Java JDK 11 (or later).
In order to build the project you are gonna need NetBeans IDE 16 (or later) and following libraries:
1. LWJGL 3.3.3 with JOML 1.10.5,
2. Jorbis OGG Decoder (for OGG audio files) (installed),
3. GapList & BigList (for Block lists) (installed),
4. Apache logger (Log4J) with dependecies (installed),
5. Google Gson (installed)

Download latest library LWJGL (3.3.3 at this time) from:
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
            └───util
```
- audio		=> Music/soundFX effects.
- cache 	=> Contains chunk ssd/hdd disk caching.
- chunk 	=> Series of blocks for instanced rendering.
- core  	=> Camera, frame buffer, water reflection, window class etc.
- critter 	=> Observer (camera interface), player & npc.
- intrface 	=> Ingame interface & 2d rendering components.
- level 	=> Level container, level actors, level editor, random level generator.
- light 	=> Light projection on the screen, light source(s).
- location 	=> Block location matrix 384x384x384 on X,Z & Y axis.
- main 		=> Main inependable classes, `Game` (client with main loop), `GameRenderer` (separate thread) & `GameObject` links them together.
			   `GameServer` is relevant class for hosting a server on the PC.
- models 	=> 3d looking models and their part(s) - vertex, mesh, material.
- net 		=> DSObject to be send over network - Request and Response.
- resources => Ingame block (models) cube.txt (deprecated) & cubex.txt.
- shaders 	=> Shader - Vertex & Fragment GLSL, Shader Program - binds them together.
- texture 	=> Ingame textures & texture store hashmap.
- util 		=> Various utils & auxillary methods including logging, math & image utils.

# Mentions
Author: Ermac(MKIII); 
Testers: 13;
Credits: Erokia
