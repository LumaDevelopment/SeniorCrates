package net.lumadevelopment.crates;

public class SQLInfo {

	/*
	 * A class that condenses multiple variables into a single object for easier reference in methods that require access to a SQL database.
	 */
	
	private String ipi;
	private String porti;
	private String databasei;
	private String useri;
	private String passwordi;
	
	public SQLInfo(String ip, String port, String database, String user, String password) {
		ipi = ip;
		porti = port;
		databasei = database;
		useri = user;
		passwordi = password;
	}
	
	public String getIP() {
		return ipi;
	}
	
	public String getPort() {
		return porti;
	}
	
	public String getDatabase() {
		return databasei;
	}
	
	public String getUser() {
		return useri;
	}
	
	public String getPassword() {
		return passwordi;
	}
	
}
