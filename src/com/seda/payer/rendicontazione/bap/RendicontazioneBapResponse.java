/**
 * 
 */
package com.seda.payer.rendicontazione.bap;

public class RendicontazioneBapResponse {

	private String code;
	private String message;
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public RendicontazioneBapResponse() {}
	public RendicontazioneBapResponse(String code, String message) {
		super();
		this.code = code;
		this.message = message;
	}
	
	public String toString() {
		return "RendicontazioneBapResponse [code="+code+
		" ,message="+message+"]";
	}



}
