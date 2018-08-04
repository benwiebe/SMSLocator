package com.t3kbau5.smslocator;

public class Interaction {
	private final String number;
	private final String command;
	private final String response;
	private final long date;
	
	Interaction(String number, String command, String response, long date){
		this.number = number;
		this.command = command;
		this.response = response;
		this.date = date;
	}


	public String getNumber(){
		return number;
	}
	
	public String getCommand(){
		return command;
	}
	
	public String getResponse(){
		return response;
	}
	
	public long getDate(){
		return date;
	}
	
}
