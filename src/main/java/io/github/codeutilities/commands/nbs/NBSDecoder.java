package io.github.codeutilities.commands.nbs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import io.github.codeutilities.commands.nbs.exceptions.OutdatedNBSException;

// Credit to https://github.com/koca2000/NoteBlockAPI/blob/master/src/main/java/com/xxmicloxx/NoteBlockAPI/NBSDecoder.java
public class NBSDecoder {

	public static SongData parse(File songFile) throws IOException, OutdatedNBSException {
		try {
			return parse(new FileInputStream(songFile), songFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public SongData parse(InputStream inputStream) throws IOException, OutdatedNBSException {
		return parse(inputStream, null);
	}

	private static SongData parse(InputStream inputStream, File songFile) throws IOException, OutdatedNBSException {
		String title = "";
		String author = "";
		String file = songFile.getName();
		float speed = 0f;
		float actualSpeed = 0f;
		short timeSignature = 4;
		int loopTick = 0;
		int loopCount = 0;
		int vanillaInstruments = 0;

		StringBuilder stringBuilder = new StringBuilder();
		StringBuilder layerStringBuilder = new StringBuilder();

        DataInputStream dataInputStream = new DataInputStream(inputStream);
        short length = readShort(dataInputStream);
        int nbsversion = 0;
        nbsversion = dataInputStream.readByte();

        if(nbsversion != 4) {
            throw new OutdatedNBSException();
        }
        vanillaInstruments = dataInputStream.readByte();
        if (length == 0) {
            length = readShort(dataInputStream);
        }
        short layers = readShort(dataInputStream);
        title = readString(dataInputStream);
        author = readString(dataInputStream);
        readString(dataInputStream);
        String description = readString(dataInputStream);
        actualSpeed = readShort(dataInputStream);
        speed = actualSpeed / 100f;
        dataInputStream.readBoolean();
        dataInputStream.readByte();
        timeSignature = (short) dataInputStream.readByte();
        readInt(dataInputStream);
        readInt(dataInputStream);
        readInt(dataInputStream);
        readInt(dataInputStream);
        readInt(dataInputStream);
        readString(dataInputStream);
        dataInputStream.readByte();
        loopCount = dataInputStream.readByte();
        loopTick = readShort(dataInputStream);
        short tick = -1;
        String[][] addStringList = new String[layers][length + 1];
        int[][] instrumentList = new int[layers][length + 1];
        int[][] pitchList = new int[layers][length + 1];
        int[][] finepitchList = new int[layers][length + 1];
        int[][] velocityList = new int[layers][length + 1];
        int[][] panningList = new int[layers][length + 1];
        boolean[] columnExistence = new boolean[length + 1];
        boolean[][] noteExistence = new boolean[layers][length + 1];
        boolean firstNoted = false;
        while (true) { //Read notes
            short t = readShort(dataInputStream);
            if (t == 0) {
                break;
            }
            tick += t;

            columnExistence[tick] = true;
            
            short layer = -1;
            while (true) {
                short jumpLayers = readShort(dataInputStream);
                if (jumpLayers == 0) {
                    break;
                }
                layer += jumpLayers;
                byte instrument = dataInputStream.readByte();
                byte note = dataInputStream.readByte();
                byte velocity = dataInputStream.readByte();
                int panning = Byte.toUnsignedInt(dataInputStream.readByte());
                short finepitch = readShort(dataInputStream);   
                
                //System.out.println("==NOTE #" + debugNoteCount + "==");
                //System.out.println("  Tick: " + tick);
                //System.out.println("  Instrument ID: " + instrument);
                //System.out.println("  Jump to the next tick: " + tCopy);
                //System.out.println("  Jump to the next layer: " + jumpLayers);
                
                instrumentList[layer][tick] = instrument;
                pitchList[layer][tick] = note;
                finepitchList[layer][tick] = finepitch;
                velocityList[layer][tick] = velocity;
                panningList[layer][tick] = panning;
                noteExistence[layer][tick] = true;
            }
        }

        for (int i = 0; i < layers; i++) { //Read layer data

            String name = readString(dataInputStream);
            dataInputStream.readByte();


            byte volume = dataInputStream.readByte();
            int panning = Byte.toUnsignedInt(dataInputStream.readByte());
            
            for(int currentTick = 0; currentTick < length + 1; currentTick++) {
            	boolean noteExists = noteExistence[i][currentTick];
            	if (noteExists == true) {
            		
            		int noteVelocity = velocityList[i][currentTick];
                	int notePanning = panningList[i][currentTick];
                	
                	double averageVelocity = noteVelocity * (volume/100d);
                	double averagePanning = (notePanning + panning)/2d;
                	
                	double preFinalPanning = (averagePanning - 100)/50;
                	
                	String finalVelocity = new BigDecimal(averageVelocity).setScale(3,BigDecimal.ROUND_FLOOR).stripTrailingZeros().toPlainString();
                	String finalPanning = new BigDecimal(preFinalPanning).setScale(3,BigDecimal.ROUND_FLOOR).stripTrailingZeros().toPlainString();

                	//COOL DEBUG CODE STARTS FROM HERE
                		//System.out.println("Pre-Final Panning: " + preFinalPanning);
                		//System.out.println("Final Panning: " + finalPanning);
                	//COOL DEBUG CODE ENDS HERE
                	
                	String finalString;
                	if (preFinalPanning == 0) {
                		finalString = "," + finalVelocity;
                	}else {
                		finalString = "," + finalVelocity + "," + finalPanning;
                	}
                	addStringList[i][currentTick] = finalString;
            	}
            }
            
            String finalLayerVolume = new BigDecimal(volume).setScale(3,BigDecimal.ROUND_FLOOR).stripTrailingZeros().toPlainString();
            String finalLayerPanning = new BigDecimal(panning).setScale(3,BigDecimal.ROUND_FLOOR).stripTrailingZeros().toPlainString();
            
            layerStringBuilder.append("=" + finalLayerVolume + "," + finalLayerPanning);
        }
        
        int customInstruments = 0;
        customInstruments = dataInputStream.readByte();
        
        int[] customPitchList = new int[customInstruments];
        
        if (customInstruments >= 1) {
        	for (int i = 0; i < customInstruments; i++) {
        		int instrumentOffset = vanillaInstruments + customInstruments;
        		int instrumentPitch = 0;
        		
        		readString(dataInputStream); //Instrument name
        		readString(dataInputStream); //Sound file
        		
        		instrumentPitch = dataInputStream.readByte(); //Sound pitch
        		
        		customPitchList[i] = instrumentPitch;
        		
        		dataInputStream.readByte();	//Press key
        	}
        }
        
        //EPIC DEBUG CODE STARTS FROM HERE
        	//System.out.println("List length: " + jumpTickList.size());
        	//System.out.println("Array length: " + jumpTickArray.length);
        //EPIC DEBUG CODE ENDS HERE

        dataInputStream.close();
        
        for (int currentTick = 0; currentTick < length + 1; currentTick++) {
        	boolean columnExists = columnExistence[currentTick];
        	if (columnExists == true) {
        		StringBuilder columnStringBuilder = new StringBuilder();
        		if (!firstNoted) {
                	columnStringBuilder.append(currentTick + 1);
                	firstNoted = true;
                }else {
                	columnStringBuilder.append("=" + (currentTick + 1));
                }
        		boolean firstAppend = true;
        		for (int i = 0; i < layers ; i++) {
        			boolean noteExists = noteExistence[i][currentTick];
            		if (noteExists == true) {
            			String laterNoteString = addStringList[i][currentTick];
                     		
            			int noteInstrument = instrumentList[i][currentTick];
            			int noteKey = pitchList[i][currentTick];
            			int noteFinePitch = finepitchList[i][currentTick];
            			
            			//ANOTHER EPIC DEBUG CODE STARTS FROM HERE
            				//System.out.println("Note ID: " + noteID);
            			//ANOTHER EPIC DEBUG CODE ENDS HERE
                    
            			if (noteInstrument >= vanillaInstruments) {
            				int instrumentId = noteInstrument - vanillaInstruments;
            				int noteKeyOffset = customPitchList[instrumentId] - 45;

            				noteKey += noteKeyOffset; 
            			}
            			if (firstAppend == true) {
            				columnStringBuilder.append(":" + (noteInstrument + 1 ) + "," + getMinecraftPitch(noteKey + (double)noteFinePitch/100d) + laterNoteString);
            				firstAppend = false;
            			}else {
            				columnStringBuilder.append(";" + (noteInstrument + 1 ) + "," + getMinecraftPitch(noteKey + (double)noteFinePitch/100d) + laterNoteString);
            			}
            		}
        		}
        		stringBuilder.append(columnStringBuilder.toString());
        	}
        }
        
        //YET ANOTHER EPIC DEBUG CODE STARTS FROM HERE
        	//System.out.println("Note Data: " + stringBuilder.toString());
        	//System.out.println("Layer Data: " + layerStringBuilder.toString());
        //YET ANOTHER EPIC DEBUG CODE ENDS HERE
        
        return new SongData(title, author, speed, (int)((Math.ceil((length+1) / timeSignature) + 1) * timeSignature), stringBuilder.toString(), file, layerStringBuilder.toString(), (loopTick + 1), loopCount, customInstruments);
	}




	private static short readShort(DataInputStream dataInputStream) throws IOException {
		int byte1 = dataInputStream.readUnsignedByte();
		int byte2 = dataInputStream.readUnsignedByte();
		return (short) (byte1 + (byte2 << 8));
	}


	private static int readInt(DataInputStream dataInputStream) throws IOException {
		int byte1 = dataInputStream.readUnsignedByte();
		int byte2 = dataInputStream.readUnsignedByte();
		int byte3 = dataInputStream.readUnsignedByte();
		int byte4 = dataInputStream.readUnsignedByte();
		return (byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24));
	}

	private static String readString(DataInputStream dataInputStream) throws IOException {
		int length = readInt(dataInputStream);
		StringBuilder builder = new StringBuilder(length);
		for (; length > 0; --length) {
			char c = (char) dataInputStream.readByte();
			if (c == (char) 0x0D) {
				c = ' ';
			}
			builder.append(c);
		}
		return builder.toString();
	}
	
	private static int getMinecraftPitch(double key) {
		
		if (key < 33) key -= 9;
		else if (key > 57) key -= 57;
		else key -= 33;
		
		double finalValue = (0.5 * (Math.pow(2,(key/12)))) * 1000;
		
		return (int) finalValue;
	}
}