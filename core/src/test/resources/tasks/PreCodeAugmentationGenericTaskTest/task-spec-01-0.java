package com.aaronicsubstances.vision.members//GE
;//GE
;
//GE

/**
 *
 * @author Aaron
 */
@Component
@Path("vision-members/api/v1")
/*GS*/@Produces(MediaType.TEXT_PLAIN)
public//JS println("World")
 class /*GS*/SmsConfigEndpoint//GS
 {
    //GE

    @GET
    @Path("send-sms-script")
    public//GS
 /*GS*/Object//GS
 getSmsScript() {
/*GE*/        String sendSmsScript//GE
 //GE
= System.getenv("vision.send-sms-script")/*GS*/;
//GS
        //GS
return /*GS*/sendSmsScript;/*GS*/
//JS println("Hello World from JS-star")
//JS var i = 3 + new Date(); 
//JS ...etc*/

    
    //GS
 }
/*GS*/    @Path("sms-config")
    /*GE*/public Object /*GE*/getSmsConfig() {//JS println("Hello")

//GE*/        String//H
//H
 /*GS*/smsConfigJson //GE
= System.getenv("vision.sms-config");/*GE*/
        return smsConfigJson/*GE*/;
    }/*GE*/
/*GE*/    
    @GET
    @Path("update-config")
/*GE*/    public //GE
Object getUpdateConfig() {
        String updateConfigJson = System.getenv("vision.update-config");
        return updateConfigJson;
    }
}
