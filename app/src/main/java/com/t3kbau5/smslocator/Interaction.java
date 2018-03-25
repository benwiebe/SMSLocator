package com.t3kbau5.smslocator;

public class Interaction {
	private int id;
	private String number;
	private String command;
	private String response;
	private long date;
	
	public Interaction(int id, String number, String command, String response, long date){
		this.id = id;
		this.number = number;
		this.command = command;
		this.response = response;
		this.date = date;
	}
	
	public Interaction(int id, String number, String command, long date){
		this.id = id;
		this.number = number;
		this.command = command;
		this.response = "";
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
