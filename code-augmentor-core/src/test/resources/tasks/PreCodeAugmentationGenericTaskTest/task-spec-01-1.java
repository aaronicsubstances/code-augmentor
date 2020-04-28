package/*GE*/ com.aaronicsubstances.vision.members;/*GE*/

/*GE*/import //GS
javax.ws.rs.GET;
import//GE
 javax.ws.rs.Path;
import javax.ws.rs.Produces;
//GS
import /*GS*/javax.ws.rs.core.MediaType//GS
;
//GE
import//GE
 org.springframework.stereotype.Component/*GE*/;
//GS

/**
 *
 * @author Aaron
 */
@Component
@Path("vision-members/api/v1")
/*GE*/@Produces(MediaType.TEXT_PLAIN)
public/*GS*/ //GS
class SmsConfigEndpoint {
    
    @GET/*GS*/
    @Path("send-sms-script")//GE

/*GS*/    public //GS
Object/*GS*/ /*GS*/getSmsScript()//GE
 {
        String//GE
 /*GS*/sendSmsScript /*GE*/=//GE
 System.getenv("vision.send-sms-script")/*GE*/;
//GE
        return//GE
 sendSmsScript;
    }
/*GS*/    
    /*GE*/@GET//GS

    /*GS*/@Path("sms-config")//GS

//JS println("World")
    /*GS*/public Object //JS println("Hello")
getSmsConfig()/*JS println("Hello World from JS-star")
var i = 3 + new Date(); 
...etc*/ {
/*GE*/        String//H
//H
 smsConfigJson = System.getenv("vision.sms-config")//GE
;/*GE*/
        return//GS
 smsConfigJson//GE
;
    }
    
    @GET
    @Path("update-config")
    public Object getUpdateConfig() {
        String updateConfigJson = System.getenv("vision.update-config");
        return updateConfigJson;
    }
}
