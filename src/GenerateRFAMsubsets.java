//  Created by Martin A. Smith in Dec 2013.
// 	Distributed under GPL
//	martinalexandersmith[at]gmail[dot]com

/*  TO DO 
	-Build treeMap for individual families. If less than X sampled, discard
	-Extract structure mask from consensus
*/
import java.util.*; import java.io.*; import java.math.*;

public class GenerateRFAMsubsets {

	static String	OUT_PATH = 			"./rfam_subset/", 
					DB_PATH = 			"./input/";

	static boolean 	VERBOSE = 			false ; 
	
	static int	MAX_PER_FAM = 			40,
				TOTAL_SEQS = 			250,
				MAX_WIN = 				175,
				MIN_WIN = 				125,
				MIN_PI = 				10,
				MAX_PI = 				80;
//				LEN_DIFF =				10;     

/*******************************************************************************
*** 						 GATHER INPUT 
********************************************************************************/
	public static void main(String[] Args) throws IOException {
		String[] FileNames = new String [1] ; 
		File [] Files = new File [1] ; 
		//usage info
		if (Args.length == 0 ){
			System.out.println(
				"\n***************************************************************************************\n" +
				"***   GenerateRFAMsubsets\n"+
				"***   Selects random (sub)sequences from a reference database \n"+
			    "**************************************************************************************\n"+
			    "Usage:  java GenerateRFAMsubsets [options] -i [path to .fasta database folder]\n"+
			    "\t*** INPUT folder must contain only fasta files***\n"+
			    "Options:\n"+
   				"  -v\tVerbose output\n"+
			    "  -min_win (int)\tminimum sequence/window length (default 125)\n"+
			    "  -max_win (int)\tmaximum sequence/window length (default 175)\n"+
				"  -min_pi (int)\tMinimum pairwise identity percentage (default 10)\n"+
				"  -max_pi (int)\tMaximum pairwise identity percentage (default 80)\n"+
				"  -f (int)\tMaximum sequences per RFAM family/input file (default 40)\n"+
				"  -t (int)\tMaximum total sequences to include (default 250)\n"+
				"          \t**N.B.** There is no minimum value at the moment\n"+
//				"  -len   (int)\tMaximum percentage of length difference (default 10) \n"+
			    "  -o\t/path/to/output/dir\n");
 			System.exit(0); 
		}
		// parse arguments
		flags: for (int i = 0 ; i != Args.length ; i++ ) {
			if ( Args[ i ].equals("-max_win") ) { 
				MAX_WIN = Integer.parseInt( Args[ i+1 ] ) ;
				i++ ; 
				continue flags; 
			}
			else if ( Args[ i ].equals("-min_win") ) { 
				MIN_WIN = Integer.parseInt( Args[ i+1 ] ) ;
				i++ ; 
				continue flags; 
			}
			else if ( Args[ i ].equals("-min_pi") ) {  
				MIN_PI = Integer.parseInt( Args[ i+1 ] ) ;
				i++ ; 
				continue flags; 
			}
			else if ( Args[ i ].equals("-max_pi") ) {  
				MAX_PI = Integer.parseInt( Args[ i+1 ] ) ;
				i++ ; 
				continue flags; 
			}
			else if ( Args[ i ].equals("-f") ) {  
				MAX_PER_FAM = Integer.parseInt( Args[ i+1 ] ) ;
				i++ ; 
				continue flags; 
			}
			else if ( Args[ i ].equals("-t") ) {  
				TOTAL_SEQS = Integer.parseInt( Args[ i+1 ] ) ;
				i++ ; 
				continue flags; 
			}
			else if ( Args[ i ].equals("-o") ) {  // out dir
				OUT_PATH= Args[ i+1 ] +"/";
				if ( !(new File(OUT_PATH)).isDirectory() )
					(new File(OUT_PATH)).mkdirs() ;	
				i++ ; 
				continue flags;
			}
			/*else if ( Args[ i ].equals("-len") ) {  // out dir
				LEN_DIFF = Integer.parseInt(Args[ i +1 ]); 
				i++;
				continue flags;
			}*/
			else if ( Args[ i ].equals("-v") ) 
				VERBOSE=true;
			else if ( Args[ i ].equals("-i") ) { 
				DB_PATH= Args[ i+1 ] +"/";
				File dir = new File(DB_PATH);
				Files = dir.listFiles(new FilenameFilter() {
    				@Override
    				public boolean accept(File dir, String name) {
        				return name.endsWith(".fasta");
    				}
				});
				FileNames = new String [ Files.length ] ; 
				// Files = dir.listFiles();
				if (Files == null) {
					System.out.println("Either dir does not exist or argument isin't a directory");
					System.exit(0); 
				}
				else {
					for (int j = 0 ; j < Files.length ; j++ ) {
						//if (VERBOSE)
						//	System.out.println( "Importing file: "+Files[ j ]);
						FileNames[j]=Files[j].toString();
					}
					i++ ;
				}	
			}
			else { 
				System.out.println("Couldn't parse argument: "+Args[ i ]+"\nPlease try again." );
				System.exit(0); 
			}
		}		
		/*******************************************************************************
		*** 						 Load data
		********************************************************************************/
		if (VERBOSE) // print out some basic information
			System.err.println(	"[ Checkpoint ] Loading data... "  ); 

		String [][] 	SeqNames = new String [ Files.length ][] ; 						// Array of sequence IDs
		String [][] 	SeqChars = new String [ Files.length ][] ;  					// Array of sequences 
		boolean [][] 	beenSampled = new boolean [ Files.length ][  ] ; 				// marker array

		for ( int i = 0 ; i != Files.length  ; i++  ) {
			if (VERBOSE)
				System.err.println("Parsing file: "+Files[i]);

			//read in individual files
			BufferedReader ReadFile = new BufferedReader(new FileReader( Files[i] )); 
			String Line = "" ; 
			Map RfamAln = new HashMap() ; 
			RfamAln = new TreeMap() ; 
			while ( (Line = ReadFile.readLine()) != null ) {
				RfamAln.put( Line , ReadFile.readLine().toUpperCase() ) ;  			
			}
			ReadFile.close(); 
			Object [] Names = RfamAln.keySet().toArray();								// returns an array of keys
			Object [] Sequences = RfamAln.values().toArray();							// returns an array of values

			SeqNames [ i ] = new String [ Names.length ] ; 								// Array of sequence IDs
			SeqChars [ i ] = new String [ Names.length ] ; 	 							// Array of sequences (as char[]) 

			//concatenate files into meta-arrays
			for ( int j = 0 ; j != Names.length ; j++ ) {
				SeqNames[ i ][ j ] = Names[ j ].toString() ; 							// converts object to string
				SeqChars[ i ][ j ] = Sequences[ j ].toString() ; 						// " 
			}			
			beenSampled[ i ] = new boolean [ Names.length ] ; 							// implements 2nd dimension array
			RfamAln.clear() ; 	
		}
		/*******************************************************************************
		*** 						 START SAMPLING
		********************************************************************************/
		int	seqs = 0 ; 
		BufferedWriter SampleOutput = new BufferedWriter(new FileWriter( OUT_PATH+"output.fasta" )); // CHANGE THIS TO INCLUDE SOME STATS
		
		if (VERBOSE) // print out some basic information
			System.err.println(	"[ Checkpoint ] Sampling data... "  ); 
		// iterate through files non-randomly
		families: for ( int fam = 0 ; fam != Files.length ; fam++ ) {
			int fam_seqs = 0 ;
			Map SampledSeqs = new HashMap() ; 
			SampledSeqs = new TreeMap() ; 
			//RFAM family name
			String RFAM_ID = FileNames[ fam ].substring( FileNames[ fam ].indexOf( "RF0" ), FileNames[ fam ].indexOf("."));
			if (VERBOSE) // print out some basic information
				System.err.println(	"[ Checkpoint ] Processing entry: "+ RFAM_ID  ); 
			
			//get a random sequence to initialise the sampling
			int random =  (int) (Math.random() * (double) SeqNames[ fam ].length) ; 
			int query_len = SeqChars[ fam ][ random ].replaceAll("[\\.]","").length();
			int counter = 0 ; 
			while ( query_len < MIN_WIN || query_len > MAX_WIN ) {
				random =  (int) (Math.random() * (double) SeqNames[ fam ].length) ; 
				query_len = SeqChars[ fam ][ random ].replaceAll("[\\.]","").length();
				counter++ ; 
				if (counter == 250 ) {
					if (VERBOSE)
						System.err.println("[ WARNING ] Random selections out of size range after 250 tries");
					continue families; 
				}
			}
			// Add to hash before printing out whole family; ensures >1 seq in output. 
			// [ NOTE ] The index can be swapped to the sequence, thus ensuring only unique sequences are output. 
			SampledSeqs.put( ">"+RFAM_ID+"_"+SeqNames[ fam ] [ random ].substring(1)+"_"+(fam_seqs+1), SeqChars[ fam ][ random ]);
			beenSampled [ fam ][ random ] = 	true ; 
			fam_seqs++ ; 
			
			scan : for ( int query = 2 ; query != SeqNames[ fam ].length ; query++ ) {
				//length of sequence without gaps
				query_len = SeqChars[ fam ][ query ].replaceAll("[\\.]","").length() ; 
				if ( query_len <= MAX_WIN && query_len >= MIN_WIN  ) {
					if ( fam_seqs == MAX_PER_FAM) 
						continue families;
					// all other sequences in a family
					double pid = 0; 
					for (int sampled = 2 ; sampled != query ; sampled++ ) {
						// compare a query to all previously included sequences  
						if ( beenSampled[ fam ][ sampled ] ) { 
							pid = GetPID( SeqChars[ fam ][ sampled ], SeqChars[ fam ][ query ]) * 100 ;
							if ( !((int) pid <= MAX_PI && (int) pid >= MIN_PI )) 
								continue scan ; 
						}
					}
					if (pid != 0) {
						if (VERBOSE) 												// print out some basic information
								System.err.println(	RFAM_ID +"\t"+ SeqNames[ fam ] [ query ] +"\t"+ (int) pid +"\t"+ query_len  ); 
						// Looks good, add it to output hashmap
						SampledSeqs.put( ">"+RFAM_ID+"_"+SeqNames[ fam ] [ query ].substring(1)+"_"+(fam_seqs+1), SeqChars[ fam ][ query ])	;	
						// mark it as sampled
						beenSampled [ fam ][ query ] = 	true ; 
						fam_seqs++ ; 
					}
				}
				// query is outside sequence length restrictions, let's ignore it in the future
				else beenSampled[ fam ][ query ] = true ; 
			}	 
			
			if ( fam_seqs  >= 3 ) {
				Object [] Names = SampledSeqs.keySet().toArray();								// returns an array of keys
				Object [] Sequences = SampledSeqs.values().toArray();							// returns an array of values
				if ( fam_seqs != Names.length )
					System.err.println( "[ ERROR ] Shit... something's broken (fam_seqs: "+ fam_seqs +" Hash.length: "+ Names.length);
				// print out all sampled names and sequences 
				for ( int x = 0 ; x != Names.length ; x++ ) {
					if ( seqs + x > TOTAL_SEQS && x >= 3 ) {
						System.err.println("[ NOTE ] Enough sequences have been sampled. ");
						System.err.println(	"[ NOTE ] Sampled " + (seqs + x -1)  + " to file "+OUT_PATH+"output.fasta"    ); 
						System.exit(0) ;		
					}
					// Write name out
					SampleOutput.write( Names[ x ].toString() + "\n");
					// Strip gaps form consensus structure
					String Aligned = Sequences[ x ].toString() ; 
					String SeqStripped = new String() ;
					String StructureStrip = new String() ; 
					for ( int y = 0 ; y != Aligned.length() ; y ++ ) {
						 if ( Aligned.charAt( y ) != '.') {
						 	SeqStripped = SeqStripped + Aligned.charAt( y ); 

						 	StructureStrip = ( SeqNames[ fam ][ 1 ].equals(">#=GC SS_cons") )? 
						 						StructureStrip + SeqChars[ fam ][ 1 ].charAt( y ) :
						 						StructureStrip + SeqChars[ fam ][ 0 ].charAt( y ) ;			 						 
						 }
					}
					SampleOutput.write( SeqStripped +"\n" + StructureStrip.replaceAll("<","(").replaceAll(">",")").replaceAll("[A-Z0-9]","\\.") + "\n"  ); 
					//clear hashmap

				}
				System.err.println(	"[ NOTE ] Added " + Names.length + " sequences from "+ RFAM_ID  ); 
				seqs = seqs + Names.length ; 
			} 
			else if (VERBOSE)
				System.err.println( "[ WARNING ] Insufficient (≤ 3) sequences sampled for "+ RFAM_ID );
			if ( seqs >= TOTAL_SEQS ) {
				System.err.println("[ NOTE ] Enough sequences have been sampled. ");
				System.err.println(	"[ NOTE ] Sampled " + seqs + " to file "+OUT_PATH+"output.fasta"    ); 
				System.exit(0) ;		
			}
			SampledSeqs.clear(); 
		}
		System.err.println(	"[ NOTE ] Sampled " + seqs + " to file "+OUT_PATH+"output.fasta"    );
		SampleOutput.close(); 
	} 
	//####################################################################################################
	//     method to calculate pairwise indentity of 2 aligned sequences
	//####################################################################################################
	public static double GetPID ( String Seq1, String Seq2) throws IOException {
		int 	matches = 		0,
				seq1_len = 		0,
				seq2_len = 		0;
		if ( Seq1.length() != Seq2.length()) {
			System.err.println(" Sequences have uneven length! Exiting..."); 
			System.exit(0); 
		}
		for (int col = 0 ; col != Seq1.length() ; col ++ ) {
			if ( Seq1.substring(col, col+1).matches("[ACTGU]") ) 
				seq1_len++ ; 
			if ( Seq2.substring(col, col+1).matches("[ACTGU]") ) 
				seq2_len++ ; 
			if ( Seq1.charAt( col ) == Seq2.charAt( col ) && Seq1.substring(col, col+1).matches("[ACTGU]") ) 
				matches++ ; 
		}
		return (double) matches / Math.min( seq1_len, seq2_len ); 
	}
			
}





