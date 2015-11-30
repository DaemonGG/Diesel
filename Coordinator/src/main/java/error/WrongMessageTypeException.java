package error;

import message.MessageTypes;

/**
 * Created by xingchij on 11/27/15.
 */
public class WrongMessageTypeException extends Exception {
	int actual;
	int shouldbe;
	String err_msg;

	public WrongMessageTypeException(int actualType, int shouldbe) {
		actual = actualType;
		this.shouldbe = shouldbe;
		this.err_msg = String.format(
				"Message type should be %s but actually %s\n",
				MessageTypes.explain(shouldbe),
				MessageTypes.explain(actualType));

	}

	public String toString() {
		return err_msg;
	}
}
