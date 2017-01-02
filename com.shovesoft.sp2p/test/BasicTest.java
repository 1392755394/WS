import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import business.Bill;
import play.mvc.Http;
import play.test.UnitTest;
import utils.ErrorInfo;

public class BasicTest extends UnitTest {
	
	@Test
	public void ceshi(HttpServletRequest quest)  {
//		String zz = "^[^']*$";
//		String name="wqef\\额而活";
//		
//		System.out.println(name.matches(zz));
		
		ErrorInfo error = new ErrorInfo();
		Bill bill = new Bill();
		bill.systemMakeOverdue(error);
		
		bill.upadateOverdueFee(error);
		   Http.Request request = Http.Request.current();
		   request.params.data.keySet();
		
		
		
	}
}
