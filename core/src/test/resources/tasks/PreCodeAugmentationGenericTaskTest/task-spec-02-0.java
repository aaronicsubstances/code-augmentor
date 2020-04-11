package /*GS*/com.aaronicsubstances.vision.members;
//DISABLE
//GS
import/*GS*/ //GS
javax.ws.rs.GET;
/*GE*/import javax.ws.rs.Path;
/*GS*/import javax.ws.rs.Produces/*GS*/;/*GE*/
/*GE*/import/*GE*/ javax.ws.rs.core.MediaType/*GS*/;
import //GE
org.springframework.stereotype.Component/*GS*/;
//ENABLE
/**
 *
 * @author Aaron
 *///GE

 //JS*println("Hello World from JS-star")
  //JS*var i = 3 + new Date();
   //JS*...etc
@Path("vision-members/api/v1")
@Produces(MediaType.TEXT_PLAIN)
public //GE
class /*GE*/SmsConfigEndpoint //GS
{//GE

    /*GS*/
    //GS
@GET
    //GE
@Path("send-sms-script")
    public Object//GS
 //GS
getSmsScript() {/*GS*/
        /*GE*/String sendSmsScript/*GE*/ //GS
= System.getenv("vision.send-sms-script");//GS

        /*GE*/return sendSmsScript;//JS println("Hello")

    }
    
//GE
    @GET
    @Path("sms-config")
//GS
    public//GE
 //GE
Object getSmsConfig()/*GS*/ {

        String /*GE*/smsConfigJson //GS
=/*GS*/ System.getenv("vision.sms-config");
	//JS println( 
	//ES:World
		//JS )

		//GS*/
        //DISABLE
        return /*GE*/smsConfigJson;
 //GE
    }
//DISABLE    
    @GET
    //GS
    //ENABLE
@Path("update-config")
    public Object getUpdateConfig() {
        String updateConfigJson = System.getenv("vision.update-config");
        return updateConfigJson;
    }
}
	//DD print
    //EJS:[]
	