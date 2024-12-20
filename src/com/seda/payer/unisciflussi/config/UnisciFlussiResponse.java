package com.seda.payer.unisciflussi.config;

public class UnisciFlussiResponse {
	
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
	public UnisciFlussiResponse() {}
	public UnisciFlussiResponse(String code, String message) {
		super();
		this.code = code;
		this.message = message;
	}
	
	public String toString() {
		return "UnisciFlussiResponse [code="+code+
		" ,message="+message+"]";
	}
}
