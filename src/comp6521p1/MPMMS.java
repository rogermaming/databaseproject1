package comp6521p1;

/**
 * Multi-phase multiway merge-sort parameters
 *
 */
public class MPMMS {

	int numOfSubLists;
	int numOfIntInMemory;
	int numOfIntInASubList;

	public MPMMS(int numOfSubLists, int numOfIntInMemory) {
		this.numOfSubLists = numOfSubLists;
		this.numOfIntInMemory = numOfIntInMemory;
		this.numOfIntInASubList = numOfIntInMemory;
	}

	public int calculateNumOfInputBuffers() {
		// let's try a 2 pass
		return (int) Math.ceil((double) numOfSubLists / 2);
	}

	public int calculateNumOfIntInAInputBuffer() {
		return 64;
	}

	public int calculateNumOfIntInOutputBuffer() {
		return 64;
	}

	public int calculateNumOfStagesInPhase2() {
		// let's try a 2 pass
		return 2;
	}

}
