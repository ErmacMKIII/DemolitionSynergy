# DemolitionSynergy
Voxel Engine using latest LWJGL.
Audio material was provided from freesound.org by Erokia.

# Verbose Description:
Lightwight Game Engine writen in Java. 
Features: Lighting, Water Reflections, Voxel based. 
Has it's own Console & ingame commands. 
Has ambient soundtracks. 
Random level building & manual level building via Editor.
Many tweaked optimizations in rendering. Instanced rendering.

![Alt text](/misc/DSynergy1.png?raw=true "Doom Underwater")
![Alt text](/misc/DSynergy2.png?raw=true "Random Level Small")
![Alt text](/misc/DSynergy3.png?raw=true "Random Level Huge")

# How To Build
Build was coded in Apache NetBeans 16. Requires Java JDK 11 (or later).
In order to build the project you are gonna need NetBeans IDE 16 (or later) and following libraries:
1. LWJGL 3.3.2 with JOML 1.10.5,
2. Jorbis OGG Decoder (for OGG audio files) (installed),
3. GapList & BigList (for Block lists) (installed),
4. Apache logger (Log4J) with dependecies (installed),

Download latest library LWJGL (3.3.2 at this time) from:
https://www.lwjgl.org/customize (this project is using Minimal OpenGL)

Put [MoveAllFiles.bat](/utils/MoveAllFiles.bat) inside the directory with downloaded & extracted zip content.
Run [MoveAllFiles.bat](/utils/MoveAllFiles.bat) and wait brief amount of time (less than 3 seconds) to sort out the files under directories.
Put rest unsorted  files into classpath directory.

In Apache NetBeans IDE 16 (or later) create new library
by specifying classpath, sources & javadocs in that order.

Add library to the project.

Done.

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
            ├───resources
            ├───shaders
            ├───texture
            └───util

- audio		=> music/soundFX effects.
- cache 	=> contains chunk ssd/hdd disk caching.
- chunk 	=> series of blocks for instanced rendering.
- core  	=> camera, frame buffer, water reflection, window class etc.
- critter 	=> observer (camera interface), player & npc.
- intrface 	=> ingame interface & 2d rendering components.
- level 	=> level container, level actors, level editor, random level generator.
- light 	=> light projection on the screen, light source(s).
- location 	=> block location matrix 512x512x512 on X,Z & Y axis.
- main 		=> main inependable classes, Game (main loop), Game Renderer (separate thread) & Game Object links them together.
- models 	=> 3D looking models and their part(s) - vertex, mesh, material.
- resources => ingame block (models) cube.txt (deprecated) & cubex.txt.
- shaders 	=> Shader - Vertex & Fragment GLSL, Shader Program - binds them together.
- texture 	=> ingame textures & texture store hashmap.
- util 		=> various utils & auxillary methods including logging, math & image utils.

# Mentions
Author: Ermac(MKIII); 
Testers: 13, Hellblade64;
Credits: Erokia
