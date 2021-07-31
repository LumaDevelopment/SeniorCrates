package net.lumadevelopment.crates;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;

public class UserDataMgr {
	
	private ConfigMgr cmgr;
	
	/*
	 * Assists initialization of UserDataMgr instances
	 * Instances allow internal methods to be used without resorting to 'static' for complex methods
	 */
	public UserDataMgr(ConfigMgr cmgr) {
		this.cmgr = cmgr;
	}
	
	//Function to provide a SQL connection;
	public Connection establishConnection() {
		
		SQLInfo info = cmgr.getSQLInfo();
		
		try {
			
			Connection dbc = DriverManager.getConnection(
					
					//SQL URL Header
					"jdbc:mysql://" +
							
					//Connection IP, Port, and Database
					info.getIP() + ":" +
					info.getPort() + "/" +
					info.getDatabase() +
					
					//Set Other Connection Settings
					"?useSSL=false",
					//"?useSSL=false&serverTimezone=UTC",
					
					//Authentication
					info.getUser(),
					info.getPassword() );
			
			return dbc;
			
		} catch(SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("SQL Connection Issue! Please verify that your SQL credentials are correct!");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return null;
			
		}
		
	}
	
	//Closes connection without having to put in try/catches everywhere SQL is used
	public void closeConn(Connection conn) {
		try {
			
			//If connection is null (like would be at first launch), don't throw more errors
			if(conn != null) {
				conn.close();
			}
			
		} catch(SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Error closing SQL connection! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return;
			
		}
	}
	
	//Function to make sure the 'seniorcrates' table is present
	public void initializeTable(Connection conn) {
		
		try {
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return;
			}
			
			PreparedStatement create = conn.prepareStatement(
					"CREATE TABLE IF NOT EXISTS seniorcrates ("
					+ "uuid VARCHAR(37) NOT NULL, "
					+ "leftinday INT NOT NULL, "
					+ "delayover BIGINT NOT NULL, "
					+ "PRIMARY KEY ( uuid ));");
			
			create.executeUpdate();
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue initializing SQL table! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return;
			
		}
		
	}
	
	//Initializes player if they don't have a crate entry
	public void initializePlayer(Connection conn, UUID u) {
		
		try {
			
			String uuid = u.toString();
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return;
			}
			
			//Here we're just grabbing a random value to see if the player is in the database
			PreparedStatement statement_one = conn.prepareStatement("SELECT leftinday FROM seniorcrates WHERE uuid='" + uuid + "';");
			
			ResultSet result_one = statement_one.executeQuery();
			
			boolean in_db = false;
			
			if(result_one != null) {
				while(result_one.next()) {
					in_db = true;
				}
			}
			
			if(!in_db) {
				
				String leftinday = String.valueOf(cmgr.getCratesPerDay());
				String delayover = String.valueOf(new Date().getTime());
				
				PreparedStatement statement_two = conn.prepareStatement("INSERT INTO seniorcrates (uuid, leftinday, delayover) VALUES ('" + uuid + "', '" + leftinday + "', '" + delayover + "')");
				
				statement_two.executeUpdate();
			}
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue initializing player data! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return;
			
		}
		
	}
	
	//Get the amount of crates the user can open 
	public Integer getLeftInDay(Connection conn, UUID u) {
		
		try {
			
			String uuid = u.toString();
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return null;
			}
			
			PreparedStatement statement = conn.prepareStatement("SELECT leftinday FROM seniorcrates WHERE uuid='" + uuid + "';");
			
			ResultSet result = statement.executeQuery();
			
			int results = 0;
			int leftinday = 1;
			
			if(result != null) {
				while(result.next()) {
					if(results < 1) {
						leftinday = result.getInt("leftinday");
					}
					
					results++;
				}
			}
			
			if(results != 1) {
				return null;
			} else {
				return leftinday;
			}
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue fetching crates left in the day! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return null;
			
		}
		
	}
	
	//Setting the amount of crates the user can open today
	public void setLeftInDay(Connection conn, Integer leftinday, UUID u) {
		
		try {
			
			String uuid = u.toString();
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return;
			}
			
			PreparedStatement statement = conn.prepareStatement("UPDATE seniorcrates SET leftinday='" + leftinday.toString() + "' WHERE uuid='" + uuid + "'");
			
			statement.executeUpdate();
			
			return;
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue setting crates left in the day! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return;
			
		}
		
	}
	
	//Resets the amount of crates every user can open today
	public void resetLeftInDay(Connection conn) {
		try {
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return;
			}
			
			String leftinday = String.valueOf(cmgr.getCratesPerDay());
			
			PreparedStatement statement = conn.prepareStatement("UPDATE seniorcrates SET leftinday='" + leftinday + "'");
			
			statement.executeUpdate();
			
			return;
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue setting crates left in the day! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return;
			
		}
	}
	
	//Checks if the delay to open a crate is over
	public boolean isDelayOver(Connection conn, UUID u) {
		
		try {
			
			String uuid = u.toString();
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return true;
			}
			
			PreparedStatement statement = conn.prepareStatement("SELECT delayover FROM seniorcrates WHERE uuid='" + uuid + "';");
			
			ResultSet result = statement.executeQuery();
			
			int results = 0;
			long delayover = 1;
			
			if(result != null) {
				while(result.next()) {
					if(results < 1) {
						delayover = result.getLong("delayover");
					}
					
					results++;
				}
			}
			
			if(results != 1) {
				return true;
			} else {
				long currenttime = new Date().getTime();
				
				if(currenttime >= delayover) {
					return true;
				}else{
					return false;
				}
			}
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue fetching delay time! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return true;
			
		}
		
	}
	
	//Function that allows retrieval of the seconds left in the delay
	public long leftInDelay(Connection conn, UUID u) {
		
		try {
			
			String uuid = u.toString();
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return 0;
			}
			
			PreparedStatement statement = conn.prepareStatement("SELECT delayover FROM seniorcrates WHERE uuid='" + uuid + "';");
			
			ResultSet result = statement.executeQuery();
			
			int results = 0;
			long delayover = 1;
			
			if(result != null) {
				while(result.next()) {
					if(results < 1) {
						delayover = result.getLong("delayover");
					}
					
					results++;
				}
			}
			
			if(results != 1) {
				return 0;
			} else {
				long currenttime = new Date().getTime();
				
				long secondsRemaining = (long) Math.ceil((double) (delayover - currenttime) / 1000);
				
				if(secondsRemaining < 0) {
					return 0;
				}else {
					return secondsRemaining;
				}
			}
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue fetching delay time! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return 0;
			
		}
		
	}
	
	//Sets the crate open delay time for a player
	public void setDelayTime(Connection conn, UUID u) {
		try {
			
			String uuid = u.toString();
			
			if(conn == null) {
				//No error handling here because an error will be thrown in establishConnection() if there's an issue
				return;
			}
			
			long currenttime = new Date().getTime();
			int delay = cmgr.getCooldown();
			String delayover = String.valueOf(currenttime + (delay * 1000));
			
			PreparedStatement statement = conn.prepareStatement("UPDATE seniorcrates SET delayover='" + delayover + "' WHERE uuid='" + uuid + "'");
			
			statement.executeUpdate();
			
			return;
			
		} catch (SQLException e) {
			
			//Error Handling
			Bukkit.getLogger().severe("Issue setting delay time! Please ensure your SQL server is online and responding.");
			
			if(cmgr.debugEnabled()) {
				e.printStackTrace();
			}
			
			return;
			
		}
	}

}
