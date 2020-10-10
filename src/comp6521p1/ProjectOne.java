package comp6521p1;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

public class ProjectOne {

	static final String FILE = "input.txt";
	static final boolean DEBUG = true;
	static long sampleSize = 0l;
	static int memorySizeInKB = 1024;
	static final int MEMORY_RESERVED_FOR_PROGRAM_IN_KB = 1020;

	public static void main(String[] args) {
		try {

			File file = new File(FILE);
			Scanner scanner = new Scanner(file);

			if (scanner.hasNext()) {
				sampleSize = Long.parseLong(scanner.next());
			}

			if (scanner.hasNext()) {
				String size = scanner.next();
				if (size.contains("MB")) {
					memorySizeInKB = Integer.parseInt(size.replace("MB", "")) * 1024;
				} else if (size.contains("KB")) {
					memorySizeInKB = Integer.parseInt(size.replace("MB", ""));

				} else {
					System.out.println("The input file doesn't contains correct memory size.");
					System.exit(0);
				}
			}

			if (DEBUG) {
				System.out.println("sampleSize = " + sampleSize + " samples");
				System.out.println("memorySizeInKB = " + memorySizeInKB + " KB");
			}

			/*******************************************************************************
			 * ------------------------------- PHASE 1 -------------------------------------
			 *******************************************************************************/

			if (DEBUG) {
				System.out.println("PHASE 1");
			}

			int memoryForData = memorySizeInKB - MEMORY_RESERVED_FOR_PROGRAM_IN_KB;
			int numOfIntInMemory = memoryForData * 1024 / 4;

			// if it's 1, we don't need phase 2, one MS will finish the job
			int numOfRunsInPhase1 = (int) Math.ceil((double) sampleSize / numOfIntInMemory);

			if (DEBUG) {
				System.out.println("\tmemoryForData = " + memoryForData + " KB");
				System.out.println("\tnumOfIntInMemory = " + numOfIntInMemory + " integers");
				System.out.println("\tnumOfRunsInPhase1 = " + numOfRunsInPhase1 + " runs");
			}

			for (int i = 0; i < numOfRunsInPhase1; i++) {
				int[] recordsInMemory = new int[numOfIntInMemory];
				for (int j = 0; j < numOfIntInMemory; j++) {
					if (scanner.hasNext()) {
						recordsInMemory[j] = scanner.nextInt();
					} else {
						break;
					}
				}

				new MergeSort(recordsInMemory).run();

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

			scanner.close();

			/*******************************************************************************
			 * ------------------------------- PHASE 2 -------------------------------------
			 *******************************************************************************/

			if (DEBUG) {
				System.out.println("PHASE 2");
			}

			if (numOfRunsInPhase1 > 1) {

				MPMMS mpmms = new MPMMS(numOfRunsInPhase1, numOfIntInMemory);
				int numOfInputBuffers = mpmms.calculateNumOfInputBuffers();
				int numOfIntInAInputBuffer = mpmms.calculateNumOfIntInAInputBuffer();
				int numOfIntInOutputBuffer = mpmms.calculateNumOfIntInOutputBuffer();
				int numOfStagesInPhase2 = mpmms.calculateNumOfStagesInPhase2();

				if (DEBUG) {
					System.out.println("\tnumOfInputBuffers = " + numOfInputBuffers + "");
					System.out.println("\tnumOfIntInAInputBuffer = " + numOfIntInAInputBuffer + " integers");
					System.out.println("\tnumOfIntInOutputBuffer = " + numOfIntInOutputBuffer + " integers");
					System.out.println("\tnumOfStagesInPhase2 = " + numOfStagesInPhase2 + " stages");
				}

				Deque<File> phase1Files = getPhase1Files(numOfRunsInPhase1);

				int numOfOutputFilesInPreviousStage = 0;
				for (int stage = 0; stage < numOfStagesInPhase2; stage++) {
					if (DEBUG) {
						System.out.println("PHASE 2 STAGE " + stage);
					}

					Deque<File> inputFilesForThisStage = stage == 0 ? phase1Files
							: getPhase2Files(stage - 1, numOfOutputFilesInPreviousStage);
					numOfOutputFilesInPreviousStage = 0;

					int run = 0;
					while (!inputFilesForThisStage.isEmpty()) {

						File output = new File("output_phase_2_stage_" + stage + "_file_" + run + ".txt");
						FileWriter writer = new FileWriter(output);

						List<Scanner> scanners = new ArrayList<>(numOfInputBuffers);
						for (int j = 0; j < numOfInputBuffers; j++) {
							File f = inputFilesForThisStage.isEmpty() ? null : inputFilesForThisStage.pop();
							if (f != null && f.exists()) {
								scanners.add(new Scanner(f));
							}
						}
						int[][] inputBuffers = new int[numOfInputBuffers][numOfIntInAInputBuffer];
						int[] index = new int[numOfInputBuffers];
						int[] outputBuffer = new int[numOfIntInOutputBuffer];

						for (int j = 0; j < numOfInputBuffers && j < scanners.size(); j++) { // populate input buffers
																								// for the 1st time
							for (int k = 0; k < numOfIntInAInputBuffer && scanners.get(j).hasNext(); k++) {
								inputBuffers[j][k] = scanners.get(j).nextInt();
							}
						}

						int outputIndex = 0;

						while (true) {
							int min = 0;
							int bufferNo = 0;

							for (int j = 0; j < numOfInputBuffers; j++) {
								if (index[j] >= numOfIntInAInputBuffer) {
									if (scanners.get(j).hasNext()) {
										for (int k = 0; k < numOfIntInAInputBuffer; k++) { // populate input buffers
																							// when needed
											inputBuffers[j][k] = scanners.get(j).hasNext() ? scanners.get(j).nextInt()
													: 0;
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

						scanners.forEach(s -> s.close());
						writer.close();
						run++;
						numOfOutputFilesInPreviousStage++;
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Deque<File> getPhase2Files(int stage, int numOfFiles) {
		Deque<File> files = new ArrayDeque<>();
		for (int i = 0; i < numOfFiles; i++) {
			File file = new File("output_phase_2_stage_" + stage + "_file_" + i + ".txt");
			files.add(file);
		}
		return files;

	}

	private static Deque<File> getPhase1Files(int numOfOuputFilesInPhase1) {
		Deque<File> files = new ArrayDeque<>();
		for (int i = 0; i < numOfOuputFilesInPhase1; i++) {
			File file = new File("output_phase_1_file_" + i + ".txt");
			files.add(file);
		}
		return files;
	}
}
