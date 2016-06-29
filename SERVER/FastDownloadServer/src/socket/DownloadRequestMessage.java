package socket;

import java.io.Serializable;

public class DownloadRequestMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4941848502160749067L;
	public String fileName;
	public int partitionsCount;
	public int partitionIndex;
	public long offset;
	
	public DownloadRequestMessage(String fileName, int partitionsCount, int partitionIndex, long offset) {
		super();
		this.fileName = fileName;
		this.partitionsCount = partitionsCount;
		this.partitionIndex = partitionIndex;
		this.offset = offset;
	}
	
	@Override
	public String toString() {
		return fileName + "; count: " + partitionsCount + "; index: " + partitionIndex + "; offset: " + offset;
	}
}
