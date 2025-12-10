package logic;

import database.ConectionDDBB;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Logic 
{
	public static ArrayList<Measurement> getDataFromDB()
	{
		ArrayList<Measurement> values = new ArrayList<Measurement>();
		
		ConectionDDBB conector = new ConectionDDBB();
		Connection con = null;
		try
		{
			con = conector.obtainConnection(true);
			Log.log.info("Database Connected");
			
			PreparedStatement ps = ConectionDDBB.GetDataBD(con);
			Log.log.info("Query=>" + ps.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				Measurement measure = new Measurement();
				measure.setValue(rs.getInt("value"));
				measure.setDate(rs.getTimestamp("time"));
				values.add(measure);
			}	
		} catch (SQLException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (NullPointerException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (Exception e)
		{
			Log.log.error("Error:" + e);
			values = new ArrayList<Measurement>();
		}
		conector.closeConnection(con);
		return values;
	}

	public static void setDataToDB(int value) throws SQLException {
    ConectionDDBB conector = new ConectionDDBB();
    Connection con = null;
    try {
        con = conector.obtainConnection(true);
        PreparedStatement ps = ConectionDDBB.SetDataBD(con);
        ps.setInt(1, value);
        ps.setTimestamp(2, new Timestamp((new Date()).getTime()));
        ps.executeUpdate();
    } finally {
        conector.closeConnection(con);
    }
}
	
	
}
