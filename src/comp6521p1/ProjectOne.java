package comp6521p1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ProjectOne {

	static final String FILE = "inputs/10e6_512kb.txt";
	static final boolean DEBUG = true;
	static final int INT_SIZE = 4;

	// If true, use the memory size in the input file to store data
	// If false, use runtime free memory
	static final boolean USE_GIVEN_MEMORY_SIZE = true;

	static long sampleSize;
	static long memorySizeInKB;

	public static void main(String[] args) {
		try {

			File file = new File(FILE);
			Scanner scanner = new Scanner(file);

			readSampleSizeAndMemorySize(scanner);

			long freeMemory = USE_GIVEN_MEMORY_SIZE ? memorySizeInKB * 1024 : Runtime.getRuntime().freeMemory();
			long numOfIntInMemory = freeMemory / INT_SIZE;

			if (DEBUG) {
				System.out.println("\nfreeMemory = " + (freeMemory / 1024) + " KB");
				System.out.println("numOfIntInMemory = " + numOfIntInMemory + " integers");
			}

			/*******************************************************************************
			 * ------------------------------- PHASE 1 -------------------------------------
			 *******************************************************************************/

			long startTime;

			if (DEBUG) {
				System.out.println("\nPHASE 1 START");
				startTime = System.nanoTime();
			}

			int numOfSubListsAfterPhaseOne = phaseOne(scanner, freeMemory, numOfIntInMemory);

			if (DEBUG) {
				long duration = System.nanoTime() - startTime;
				System.out.println("PHASE 1 END (" + TimeUnit.NANOSECONDS.toMillis(duration) + " ms)");
			}

			scanner.close();
			System.gc();

			/*******************************************************************************
			 * ------------------------------- PHASE 2 -------------------------------------
			 *******************************************************************************/

			if (DEBUG) {
				System.out.println("\nPHASE 2 START");
				startTime = System.nanoTime();
			}

			if (numOfSubListsAfterPhaseOne > 1) {
				phaseTwo(numOfSubListsAfterPhaseOne);
			}

			if (DEBUG) {
				long duration = System.nanoTime() - startTime;
				System.out.println("PHASE 2 END (" + TimeUnit.NANOSECONDS.toMillis(duration) + " ms)");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int phaseOne(Scanner scanner, long freeMemory, long numOfIntInMemory) throws IOException {

		// Assuming java sort (quick sort) using 200% memory of data size
		// e.g. sorting 100 using 2 * 100 * 4 bytes memory
		// hence, the number of int we can store in memory will be divided by 2
		numOfIntInMemory /= 2;

		// numOfRuns = num of output files
		int numOfRunsInPhase1 = (int) Math.ceil((double) sampleSize / numOfIntInMemory);

		for (int i = 0; i < numOfRunsInPhase1; i++) {

			int[] recordsInMemory = new int[(int) numOfIntInMemory];

			for (int j = 0; j < numOfIntInMemory && scanner.hasNext(); j++) {
				recordsInMemory[j] = scanner.nextInt();
			}

			// Quick sort, uses twice of memory as input data
			Arrays.sort(recordsInMemory);

			// Write to output files
			File output = new File("output_phase_1_file_" + i + ".txt");
			if (output.createNewFile()) {
				FileWriter writer = new FileWriter(output);
				for (int k : recordsInMemory) {
					if (k > 0) {
						writer.write(k + " ");
					}
				}
				writer.close();
			}

		}
		return numOfRunsInPhase1;
	}

	private static void phaseTwo(int numOfSubListsAfterPhaseOne) throws IOException, FileNotFoundException {

		long freeMemory = USE_GIVEN_MEMORY_SIZE ? memorySizeInKB * 1024 : Runtime.getRuntime().freeMemory();
		long numOfIntInMemory = freeMemory / INT_SIZE;

		MPMMS mpmms = new MPMMS(numOfSubListsAfterPhaseOne, (int) numOfIntInMemory);

		int numOfInputBuffers = mpmms.calculateNumOfInputBuffers();
		int numOfIntInAInputBuffer = mpmms.calculateBufferSize();
		int numOfIntInOutputBuffer = mpmms.calculateOutputBufferSize(numOfInputBuffers, numOfIntInAInputBuffer);

		if (DEBUG) {
			System.out.println("\tnumOfInputBuffers = " + numOfInputBuffers + "");
			System.out.println("\tnumOfIntInAInputBuffer = " + numOfIntInAInputBuffer + " integers");
			System.out.println("\tnumOfIntInOutputBuffer = " + numOfIntInOutputBuffer + " integers");
		}

		File[] phase1Files = getPhase1Files(numOfSubListsAfterPhaseOne);

		int numOfOutputFilesInPreviousStage = 0;

		for (int stage = 0; numOfOutputFilesInPreviousStage == 0 || numOfOutputFilesInPreviousStage > 1; stage++) {

			long startTime;
			if (DEBUG) {
				System.out.println("\n\tPHASE 2 STAGE " + stage + " START");
				startTime = System.nanoTime();
			}

			File[] inputFilesForThisStage = stage == 0 ? phase1Files
					: getPhase2Files(stage - 1, numOfOutputFilesInPreviousStage);
			numOfOutputFilesInPreviousStage = 0;

			int run = 0;
			for (int i = 0; i < inputFilesForThisStage.length; i += numOfInputBuffers) {

				File output = new File("output_phase_2_stage_" + stage + "_file_" + run + ".txt");
				FileWriter writer = new FileWriter(output);

				Scanner[] scanners = new Scanner[numOfInputBuffers];
				for (int j = 0; j < numOfInputBuffers && i + j < inputFilesForThisStage.length; j++) {
					File f = inputFilesForThisStage[i + j];
					if (f != null && f.exists()) {
						scanners[j] = new Scanner(f);
					}
				}
				int[][] inputBuffers = new int[numOfInputBuffers][numOfIntInAInputBuffer];
				int[] index = new int[numOfInputBuffers];
				int[] outputBuffer = new int[numOfIntInOutputBuffer];

				for (int j = 0; j < numOfInputBuffers && j < scanners.length && scanners[j] != null; j++) {
					// populate input buffers for the 1st time
					for (int k = 0; k < numOfIntInAInputBuffer && scanners[j].hasNext(); k++) {
						inputBuffers[j][k] = scanners[j].nextInt();
					}
				}

				int outputIndex = 0;

				while (true) {
					int min = 0;
					int bufferNo = 0;

					for (int j = 0; j < numOfInputBuffers; j++) {
						if (index[j] >= numOfIntInAInputBuffer) {
							if (scanners[j].hasNext()) {
								for (int k = 0; k < numOfIntInAInputBuffer; k++) { // populate input buffers
																					// when needed
									inputBuffers[j][k] = scanners[j].hasNext() ? scanners[j].nextInt() : 0;
								}
								index[j] = 0;
							}
							continue;
						}
						if (min == 0 && inputBuffers[j][index[j]] > 0) {
							min = inputBuffers[j][index[j]];
							bufferNo = j;
						} else if (inputBuffers[j][index[j]] > 0 && min > inputBuffers[j][index[j]]) {
							min = inputBuffers[j][index[j]];
							bufferNo = j;
						}
					}

					if (min == 0) {
						break;
					} else {
						index[bufferNo]++;
						outputBuffer[outputIndex] = min;
						outputIndex++;
						if (outputIndex + 1 == numOfIntInOutputBuffer) { // write output buffer data to file
							for (int j = 0; j < outputIndex; j++) {
								writer.write(outputBuffer[j] + " ");
								outputBuffer[j] = 0;
							}
							outputIndex = 0;
						}
					}
				}

				for (int j = 0; j < scanners.length && scanners[j] != null; j++) {
					scanners[j].close();
				}
				writer.close();
				run++;
				numOfOutputFilesInPreviousStage++;
			}

			if (DEBUG) {
				long duration = System.nanoTime() - startTime;
				System.out.println(
						"\tPHASE 2 STAGE " + stage + " END (" + TimeUnit.NANOSECONDS.toMillis(duration) + " ms)");
			}

		}

	}

	private static void readSampleSizeAndMemorySize(Scanner scanner) {
		if (scanner.hasNext()) {
			sampleSize = Long.parseLong(scanner.next());
		}

		if (scanner.hasNext()) {
			String size = scanner.next().toUpperCase();
			if (size.contains("MB")) {
				memorySizeInKB = Integer.parseInt(size.replace("MB", "")) * 1024;
			} else if (size.contains("KB")) {
				memorySizeInKB = Integer.parseInt(size.replace("KB", ""));
			} else {
				System.out.println("The input file doesn't contains correct memory size.");
				System.exit(0);
			}
		}

		if (DEBUG) {
			System.out.println("sampleSize = " + sampleSize + " samples");
			System.out.println("memorySizeInKB = " + memorySizeInKB + " KB");
		}
	}

	private static File[] getPhase2Files(int stage, int numOfFiles) {
		File[] files = new File[numOfFiles];
		for (int i = 0; i < numOfFiles; i++) {
			File file = new File("output_phase_2_stage_" + stage + "_file_" + i + ".txt");
			files[i] = file;
		}
		return files;

	}

	private static File[] getPhase1Files(int numOfOuputFilesInPhase1) {
		File[] files = new File[numOfOuputFilesInPhase1];
		for (int i = 0; i < numOfOuputFilesInPhase1; i++) {
			File file = new File("output_phase_1_file_" + i + ".txt");
			files[i] = file;
		}
		return files;
	}
}
