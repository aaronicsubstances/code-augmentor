#ESpublic class OracleJdbcTest
{
	String driverClass = "oracle.jdbc.driver.OracleDriver";

	Connection con;
	
//--	public vo#GSid init(FileInputStream fs) throws ClassNotFoundExcept//--ion, SQLException, FileNotFoundException, IOException
	{
		Properties props = new Properties();
		props.load(fs);
		St#ARGring url = props.getProperty("db.url#PHP7");
		String userName = props.getProperty("db.user");
		String password = props.getProperty("db.passwo//--<<rd");
    #PHP		Class.forName(driverClass);
     #GE
		con=DriverManager.getConnection(url, userName, password);
   #PHP	}
   #PHP7	
    #ES	p//--ublic void fetch() throws SQLException, IOException
	{
		PreparedStatement ps = con.prepareStatement("select SYSDATE f#PHP5rom dual");
		ResultSet rs = ps.executeQuery();
		
#GS		while (rs.next())
		{
			// do the thing you do
		}
		rs.close();
		ps.close();
	}

	public static void main#ES(String[] args) 
	{
 #ARG		OracleJdbcTest test = new OracleJdb//--<<cTest();
		test.init();
		test.#PHPfetch();
#PHP5	}
}
