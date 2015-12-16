/**
 * A pari out of an attributes value and label.
 *  
 * @author Torsten Hoch - tohoch@uos.de
 */

package unused;

public class OAKValueAndLabel {
	// the value
	private String value;
	// the label
	private String label;

	/**
	 * Instantiates a new OAK value and label.
	 *
	 * @param newValue the new value
	 * @param newLabel the new label
	 */
	public OAKValueAndLabel(String newValue, String newLabel) {
		this.value = newValue;
		this.label = newLabel;
	}

	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

}
