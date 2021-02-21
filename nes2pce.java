;
; Copyright (c) 2021 Peter McQuillan 
; 
; All Rights Reserved. 
; 
; Distributed under the BSD Software License (see license.txt) 
; 
; 

import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;


    public class nes2pce
    {
        public static void main(String[] args)
        {
            String[] palarr = {
"$333","$013","$104","$104","$203","$201","$200","$210","$110","$020","$020","$020","$012","$000","$000","$000",
"$555","$125","$226","$316","$415","$413","$411","$420","$330","$230","$130","$031","$033","$000","$000","$000",
"$777","$357","$447","$537","$637","$735","$743","$642","$551","$461","$362","$264","$256","$222","$000","$000",
"$777","$567","$667","$667","$757","$756","$765","$765","$664","$664","$575","$566","$566","$555","$000","$000"
                };

            int startingAddress = 4096; //1000 hex - this is a VRAM address, VRAM storage is in words
            int bank2offset = 4096;
            int pointerAddress;

            int doublebankCHR = 0;

            if (args.length == 0)
            {
                System.out.println("This program needs 5 parameters: ");
                System.out.println("A palette file, a CHR file, a Nametable file and two integer values");
                System.out.println("For the first integer, 0 means use first page of CHR file, 1 means use second page.");
                System.out.println("For the second integer, enter the decimal version (not hex) of your desired VRAM starting address - 4096 is recommended.");
                return;
            }

            String palFilename = args[0];
            String chrFilename = args[1];
            String ntFilename = args[2];
            int chrToUse = 0;
            int passedStartingAddress = 0;

            try
            {
				chrToUse = Integer.parseInt(args[3]);
            }
            catch(Exception e)
            {
                chrToUse = 0;
            }

            if (chrToUse != 0 && chrToUse !=1)
            {
                System.out.println("Only values 0 and 1 are supported for selecting which CHR page to use.");
                return;
            }

            try
            {
				passedStartingAddress = Integer.parseInt(args[4]);
            }
            catch (Exception e)
            {
                System.out.println("Invalid VRAM starting address - defaulting to 4096 (1000 hex) ");
                passedStartingAddress = 4096;
            }

            if (passedStartingAddress<1024)
            {
                System.out.println("You have entered a value for the VRAM starting address that is too low.");
                return;
            }

            if (passedStartingAddress > 28672) //7000 hex
            {
                System.out.println("You have entered a value for the VRAM starting address that is too high.");
                return;
            }

            startingAddress = passedStartingAddress;
			
			File chrfi = new File(chrFilename);
			if (!chrfi.exists() || !chrfi.isFile())
			{
                System.out.println("Incorrect CHR filename provided.");
                return;
            }

            long chrsize = chrfi.length();

            if (chrsize != 4096 && chrsize != 8192)
            {
                System.out.println("Incorrect CHR provided.");
                return;
            }

            if(chrsize==4096 && chrToUse==1)
            {
                System.out.println("You have selected second page of a 1 page CHR file - defaulting to 1st page.");
                chrToUse = 0;
            }

            if(chrsize==8192)
            {
                doublebankCHR = 1;
            }
			
			File palfi = new File(palFilename);
			if (!palfi.exists() || !palfi.isFile())
			{
                System.out.println("Incorrect palette file filename provided.");
                return;
            }

            long palsize = palfi.length();

            if (palsize != 16)
            {
                System.out.println("Incorrect palette file provided - this program only supports a set of 4 NES palettes.");
                return;
            }

			File ntfi = new File(ntFilename);
			if (!ntfi.exists() || !ntfi.isFile())
			{
                System.out.println("Incorrect nametable filename provided.");
                return;
            }

            long ntsize = ntfi.length();

            if (ntsize!=1024)
            {
                System.out.println("The nametable needs to include the attributes. This program also does not support RLE encoded NES nametables.");
                return;
            }

			byte[] ntBuffer = new byte[960];
			byte[] attrBuffer = new byte[64];
			byte[] palBuffer = new byte[16];
			byte[] chrBuffer = new byte[8192];
		
			try
			{
				DataInputStream palReader = new DataInputStream(new FileInputStream(palFilename));
				DataInputStream chrReader = new DataInputStream(new FileInputStream(chrFilename));
				DataInputStream ntReader = new DataInputStream(new FileInputStream(ntFilename));
				
				// A nametable is 1024 bytes in size, 960 bytes (32x30) is for tile data, the remaining 64 bytes are for attributes
				ntReader.read(ntBuffer, 0, 960);

				ntReader.read(attrBuffer, 0, 64);

				palReader.read(palBuffer, 0, 16);

				if (doublebankCHR == 0)
				{
					chrReader.read(chrBuffer, 0, 4096);
				}
				else
				{
					chrReader.read(chrBuffer, 0, 8192);
				}
			}
			catch(Exception e)
			{
				System.out.println("Error when reading data from files : Error thrown :" + e.toString());
				return;
			}

            StringBuilder sb = new StringBuilder();

            // First we write the BAT data

            sb.append("importedbatdata:\n");
            sb.append("\t.db ");
            int processedNTPosition = 0;
            int formattedAddress;
            int lsb;
            int msb;
            int attrIdx,attrVal;
            int x, y;
            int attrSection;    // 0 = top left, 1 = top right, 2 = bottom left, 3 = bottom right
            int paletteToUse;
            int tmpCalc;

            if(chrToUse == 1)
            {
                pointerAddress = startingAddress + bank2offset;
            }
            else
            {
                pointerAddress = startingAddress;
            }

            while (processedNTPosition<960)
            {
                // we add data in the order LSB.MSB,LSB etc


				// and FF to value from buffer as need an unsigned value
                formattedAddress = (pointerAddress + ((ntBuffer[processedNTPosition] & 0xFF) * 16)) >> 4;
                lsb = (formattedAddress & 255);
                msb = (formattedAddress & 3840) >> 8;   // 3840 = F00 in decimal

                x = processedNTPosition % 32;
                y = processedNTPosition / 32;


                // each attribute controls the palette for a 4x4 tile section of the nametable, so 8 attributes cover the width of the screen
                attrIdx = (x / 4) + ((y / 4)*8);

                attrVal = attrBuffer[attrIdx];

                // ok, we now have the correct attribute byte, but only 2 bits of this are relevant for this tile
                attrSection = 0;

                tmpCalc = x / 2;
                if(tmpCalc%2==1)
                {
                    attrSection = attrSection + 1;
                }
                tmpCalc = y / 2;
                if (tmpCalc % 2 == 1)
                {
                    attrSection = attrSection + 2;
                }

                paletteToUse = 0;
                if (attrSection==0)
                {
                    // top left
                    paletteToUse = (attrVal & 3);
                }
                else if (attrSection == 1)
                {
                    // top right
                    paletteToUse = (attrVal & 12) >> 2;
                }
                if (attrSection == 2)
                {
                    // bottom left
                    paletteToUse = (attrVal & 48) >> 4;
                }
                if (attrSection == 3)
                {
                    // bottom right
                    paletteToUse = (attrVal & 192) >> 6;
                }


                // now that we know the palette, merge with the MSB

                paletteToUse = paletteToUse << 4;
                msb = msb + paletteToUse;

                sb.append("$" + String.format("%02X", lsb) + ",$" + String.format("%02X", msb));


                processedNTPosition++;
                if (processedNTPosition % 8 != 0)
                {
                    sb.append(",");
                }

                if (processedNTPosition%8==0 && processedNTPosition!=960)
                {
                    sb.append("\n");
                    sb.append("\t.db ");
                }
            }
            sb.append("\n\n");

            // Now write the palette data

            int palnum = 1;
            for (int i = 0; i < 4; i++)
            {
                sb.append("importedpalette" + (palnum + i) + ": .defpal  ");
                for (int j = 0; j < 4; j++)
                {
                    sb.append(palarr[palBuffer[(i * 4) + j]] + ",");
                }
                sb.append("$000,$000,$000,$000,$000,$000,$000,$000,$000,$000,$000,$000\n");
            }
            sb.append("\n\n");







            // Finally we write the actual graphic data

            int chunk1, chunk2, chunkResult;
            int shiftval;
            byte[] tmpBuffer = new byte[16];    //16 bytes per NES tile

            for (int i= 0;i< 256;i++)
            {
                    sb.append("\n");
                    sb.append("importedtileA" + i + ":" + "\t.defchr $" + String.format("%04X", startingAddress) + ",0,\\");
                    sb.append("\n");
                    startingAddress += 16;

                for(int j=0; j<16;j++)
                {
                    tmpBuffer[j] = chrBuffer[(i * 16) + j];
                }

                for(int j=0;j<8;j++)    //8 rows of pixels per tile
                {
                    int bitp1 = tmpBuffer[j];
                    int bitp2 = tmpBuffer[j + 8];

                    sb.append("\t$");

                    for (int k=0;k<8;k++)    // 8 columns of pixels per tile
                    {
                        shiftval = 7 - k;
                        chunk1 = (bitp1 & (1 << shiftval)) >> shiftval;
                        chunk2 = (bitp2 & (1 << shiftval)) >> shiftval;
                        chunkResult = chunk1 + (2 * chunk2);
                        sb.append(chunkResult);
                    }

                    if(j!=7)
                    {
                        sb.append(",\\");
                    }
                    sb.append("\n");
                }
            }

            // if the CHR has 2 banks of CHR, lets print them

            if(doublebankCHR==1)
            {
                for (int i = 0; i < 256; i++)
                {
                    sb.append("\n");
                    sb.append("importedtileB" + i + ":" + "\t.defchr $" + String.format("%04X", startingAddress) + ",0,\\");
                    sb.append("\n");
                    startingAddress += 16;

                    for (int j = 0; j < 16; j++)
                    {
                        tmpBuffer[j] = chrBuffer[4096 + (i * 16) + j];
                    }

                    for (int j = 0; j < 8; j++)    //8 rows of pixels per tile
                    {
                        int bitp1 = tmpBuffer[j];
                        int bitp2 = tmpBuffer[j + 8];

                        sb.append("\t$");

                        for (int k = 0; k < 8; k++)    // 8 columns of pixels per tile
                        {
                            shiftval = 7 - k;
                            chunk1 = (bitp1 & (1 << shiftval)) >> shiftval;
                            chunk2 = (bitp2 & (1 << shiftval)) >> shiftval;
                            chunkResult = chunk1 + (2 * chunk2);
                            sb.append(chunkResult);
                        }

                        if (j != 7)
                        {
                            sb.append(",\\");
                        }
                        sb.append("\n");
                    }
                }

            }


            String content = sb.toString();
			try
			{
				BufferedWriter writer = new BufferedWriter(new FileWriter("output.asm"));
				writer.write(content);
				writer.close();
			}
			catch(Exception e)
			{
				System.out.println("Failed to write output file: Error: " + e.toString());
			}
			

        }
    }
