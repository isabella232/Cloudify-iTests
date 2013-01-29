package framework.utils.rest;

public class RestClientException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int status;
	private String messageId;
	private String message;
	
	public RestClientException(final int status, final String messageId, final String message) {
		this.message = message;
		this.messageId = messageId;
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getMessage() {
		return message;
	}
}
