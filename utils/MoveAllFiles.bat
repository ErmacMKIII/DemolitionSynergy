:: Purpose is simplify usage of adding library natives, sources & javadocs to the Apache NetBeans IDE.
:: When creating library with 'Ant library manager'.

:: Make directories if not exist

if not exist ".\classpath" mkdir "classpath"
if not exist ".\sources" mkdir "sources"
if not exist ".\javadoc" mkdir "javadoc"
if not exist ".\licenses" mkdir "licenses"

:: Move each file with matching filename to the directories  

for /r . %%x in (*natives*) do move %%x ".\classpath"
for /r . %%x in (*sources*) do move %%x ".\sources"
for /r . %%x in (*javadoc*) do move %%x ".\javadoc"
for /r . %%x in (*license*) do move %%x ".\licenses"