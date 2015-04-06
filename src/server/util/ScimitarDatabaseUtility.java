package server.util;
/*
 * This file is part of RuneSource.
 *
 * RuneSource is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RuneSource is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RuneSource.  If not, see <http://www.gnu.org/licenses/>.
 */


import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

import server.ScimitarEngine;

/**
 * 
 * @author Stuart Murphy
 *
 */
public class ScimitarDatabaseUtility {

	private static Properties props;
	private static final HashMap<Integer, ConnWrapper> connections = new HashMap<Integer, ConnWrapper>();
	private static boolean propsInited = false;
	private static long connectionTimeOut = 2 * 60 * 1000;

	public static Connection getConnection() {
		Thread t = Thread.currentThread();
		int id = (int) t.getId();
		ConnWrapper ret = connections.get(id);
		if (ret == null) {
			Connection retCon = connectToDB();
			ret = new ConnWrapper(retCon);
			connections.put(id, ret);
		}
		return ret.getConnection();
	}

	private static long getWaitTimeout(Connection con) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery("SHOW VARIABLES LIKE 'wait_timeout'");
			if (rs.next()) {
				return Math.max(1000, rs.getInt(2) * 1000 - 1000);
			} else {
				return -1;
			}
		} catch (SQLException ex) {
			return -1;
		} finally {
			close(rs);
			close(stmt);
		}
	}
	
	private static Connection connectToDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Connection con = DriverManager.getConnection("jdbc:mysql://" + ScimitarEngine.getSetting("db_host") + ":" + ScimitarEngine.getSetting("db_port") + "/" + ScimitarEngine.getSetting("db_name"), 
					ScimitarEngine.getSetting("db_user"), ScimitarEngine.getSetting("db_pass"));
			if (!propsInited) {
				long timeout = getWaitTimeout(con);
				if(timeout > -1) {
					connectionTimeOut = timeout;
				}
				propsInited = true;
			}
			return con;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void closeAll() throws SQLException {
		for (ConnWrapper con : connections.values()) {
			con.connection.close();
		}
		connections.clear();
	}
	
    public static void close(ResultSet rs) {
        try {
            rs.close();
        } catch (Exception e) {
        }
    }
    
    public static void close(Statement stmt) {
        try {
            stmt.close();
        } catch (Exception e) {
        }
    }
    
    public static void close(Connection conn) {
        try {
            conn.close();
        } catch (Exception e) {
        }
    }
    
    public static Properties getQueries() throws SQLException {
    	if(props == null) {
    		props = new Properties();
    		try {
    			props.load(new FileInputStream(new File("./data/queries.props")));
    		} catch(Exception e) {
    			throw new SQLException("Unable to load query property file " + e.getMessage());
    		}
    	}
    	return props;
    }
    
    public static String getQuery(String query) throws SQLException {
    	return getQueries().getProperty(query);
    }

	private static class ConnWrapper {
		
		private long lastAccessTime = 0;
		private Connection connection;

		public ConnWrapper(Connection con) {
			this.connection = con;
		}

		public Connection getConnection() {
			if (expiredConnection()) {
				try {
					connection.close();
				} catch (Throwable err) {
					
				}
				this.connection = connectToDB();
			}
			lastAccessTime = System.currentTimeMillis();
			return this.connection;
		}

		public boolean expiredConnection() {
			if (lastAccessTime == 0) {
				return false;
			}
			try {
				return System.currentTimeMillis() - lastAccessTime >= connectionTimeOut
						|| connection.isClosed();
			} catch (Throwable ex) {
				return true;
			}
		}
	}

}
