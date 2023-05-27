# DemolitionSynergy
Voxel Engine (Incomplete Game) using latest LWJGL.
Audio material was provided from freesound.org by Erokia.

Verbose Description:
Lightwight Java Game Library engine. 
Features: Lighting, Water Reflections, Voxel based. 
Has it's own Console & ingame commands. 
Has ambient soundtracks. 
Random level building & manual level building via Editor.
Many tweaked optimizations in rendering.

![Alt text](/misc/DSynergyNight.png?raw=true "DSynergyNight")
![Alt text](/misc/DSynergyUnderWater.png?raw=true "DSynergyUnderWater")

Build was coded in Apache NetBeans 16. Requires Java 1.8 (or later).
In order to build the project you are gonna need NetBeans IDE 8.2 (or later) and following libraries:
1. LWJGL 3.3.2 with JOML 1.10.5,
2. Jorbis OGG Decoder (for OGG audio files) [installed],
3. GapList & BigList (for Block lists) [installed],
4. Apache logger (Log4J) [installed],

Download lastest library LWJGL (3.3.2 at this time) from:
https://www.lwjgl.org/customize (this project is using Minimal OpenGL)

Put CleanCache.bat inside the directory with downloaded & extracted zip content.
Run CleanCache.bat and wait brief amount of time (less than 3 seconds) to sort out the files under directories.
Put rest unsorted  files into classpath directory.

In Apache NetBeans IDE 16 (or later) create new library
by specifying classpath, sources & javadocs in that order.

Add library to the project.

Done.


Testers: 13, Hellblade64;
Credits: Erokia