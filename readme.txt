////////////////////////////////////////////////////////////////////////////
//                NES Nametable and CHR converter to PC Engine            //
//                    Copyright (c) 2021 Peter McQuillan                  //
//                          All Rights Reserved.                          //
//      Distributed under the BSD Software License (see license.txt)      //
////////////////////////////////////////////////////////////////////////////

This package contains a Java program to convert NES graphic data to a format
where it can be used on the PC Engine console.
It also includes an example ASM file which shows how the produced data can be
used in a PC Engine assembly file.

===
To run this program you must have Java installed on your computer.
Example of calling the program:

java nes2pce input.pal graphics.chr splash.nam 0 4096

So there are 5 parameters:

First parameter is the name of the palette file.
Second parameter is the name of the NES CHR file.
Third parameter is the name of the NES nametable file.
Fourth parameter is the bank of CHR used by the nametable (possible values 0 or 1)
Fifth parameter is the address in VRAM you which the tile data to be stored - this
value is entered in decimal format not hex format, suggested value is 4096.

Assuming no errors, the program should output a file called output.asm


The nametable shouldn't be in RLE format, but in raw/normal format.

I use Shiru's "NES Screen Tool" (nesst) to view/edit my NES graphic resources.

Using the data from output.asm, if you want to ensure your graphic data has been
correctly converted, you can paste the data into the relevant sections of the
included example.asm file.
You can then use an assembler such as pceas to assemble the file and produce a
.pce file which you can then test on an emulator or a real console.

The example.asm file currently contains a version of the splash screen from the 
game Draiocht.

The nes2pce.java file contains a lookup table for the palette to match the colors,
but you may wish to make changes to this for personal preference.

No special parameters are needed when compiling the Java file to produce the class
file (should you wish to make changes to the Java file) so the class file can be
produced as per usual, i.e.

javac nes2pce.java

Please direct any questions or comments to beatofthedrum@gmail.com
