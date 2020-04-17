package demo;

import org.testng.Assert;
import org.testng.annotations.*;

public class MainNGTest {
	
	@Test
	public void testDescription() {
		String expResult = "standalone";
		String actual = Main.description();
		Assert.assertEquals(actual, expResult);
	}
}