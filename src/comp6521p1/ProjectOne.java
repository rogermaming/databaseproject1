package comp6521p1;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

public class ProjectOne {

	static final String FILE = "dataInput.txt";
	static final boolean DEBUG = true;
	static long sampleSize = 0l;
	static int memorySizeInKB = 1024;
	static final int MEMORY_RESERVED_FOR_PROGRAM_IN_KB = 1020;
	
	public static void insertSort(int[] arr) {
		int j; // 已排序列表下标
		int t; // 待排序元素
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] < arr[i - 1]) { 
				t = arr[i]; // 赋值给待排序元素
				for (j = i - 1; j >= 0 && arr[j] > t; j--) {
					arr[j + 1] = arr[j]; // 从后往前遍历已排序列表，逐个和待排序元素比较，如果已排序元素较大，则将它后移
				}
				arr[j + 1] = t; // 将待排序元素插入到正确的位置
			}
		}
	}

	public static void main(String[] args) {
		
		//long startTime = System.currentTimeMillis();
		
		try {

			File file = new File(FILE);
			Scanner scanner = new Scanner(file);

			String firstLine = scanner.nextLine();
			sampleSize = Long.parseLong(firstLine.split(" ")[0]);		

			if (DEBUG) {
				System.out.println("sampleSize = " + sampleSize + " samples");
			}

			/*******************************************************************************
			 * ------------------------------- PHASE 1 -------------------------------------
			 *******************************************************************************/
			long phaseOneStartTime = System.currentTimeMillis();
			if (DEBUG) {
				System.out.println("PHASE 1");
			}
			
			int turn = 0;
		
			while(scanner.hasNext()) {
				
				File output = new File("output_phase_1_file_" + turn + ".txt");
				
				long totalMemory = Runtime.getRuntime().totalMemory();
				long available = Runtime.getRuntime().freeMemory();
				
				System.out.println("Total Memory(MB): " + (totalMemory / (1024*1024)));	
				System.out.println("Available Memory(MB): " + (available / (1024 * 1024)));
				
				int arrayLength = (int) available / 4;
				System.out.println("Array Length: " + arrayLength);
				
				int[] array = new int[arrayLength];
				for (int j = 0; j < arrayLength; j++) {
					if (scanner.hasNext()) {
						array[j] = Integer.parseInt(scanner.next());
					} else {
						break;
					}
				}
				
				ProjectOne.insertSort(array);

				output.createNewFile();
				FileWriter writer = new FileWriter(output);
				for (int k = 0; k < array.length; k++) {
					if (array[k] != 0) {
						//System.out.println(array[k]);
						writer.write(String.valueOf(array[k]) + " ");
					}
				}
				writer.close();
				turn += 1;
			}
			
			scanner.close();
			long phaseOneEndTime = System.currentTimeMillis();
			System.out.println("Phase One Time Cost: " + (phaseOneEndTime - phaseOneStartTime) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

			/*******************************************************************************
			 * ------------------------------- PHASE 2 -------------------------------------
			 *******************************************************************************/
	/*		
			long phaseTwoStartTime = System.currentTimeMillis();
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
			long phaseTwoEndTime = System.currentTimeMillis();
			System.out.println("Phase Two Time Cost: " + (phaseTwoEndTime - phaseTwoStartTime) + "ms");

		} catch (Exception e) {
			e.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		System.out.println("Total time cost: " + (endTime - startTime) + "ms");
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
}*/
}
