/**
 *  ****************  Schedule A Switch Event  ****************
 *
 *  Copyright 2017 Andrew Parker
 *  
 *  This SmartApp is free!
 *  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm happy for you to use this without a donation (but, if you find it useful then it would be nice to get a 'shout out' on the forum!) -  @Cobra
 *
 *  Have an idea to make this app better?  - Please let me know :)
 *
 *  Website: http://securendpoint.com/smartthings
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below -  @Cobra
 *
 *
 *
 *  Last Update: 01/11/2018
 *
 *  Changes:
 *
 * 
 *
 *
 *  
 *  V1.0.0 - POC
 *
 */
 
 
 
 
 
 
 
 
 
definition(
    name: "Daily Switch Event Child",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Schedule a time to turn on a switch then schedule a time to turn it off again",
    category: "Convenience",
    
  parent: "Cobra:Daily Switch Event",
    
    iconUrl: "",
	iconX2Url: "",
    iconX3Url: "",
    )

preferences {
	display()

    section("") {
		input (name: "startTime", title: "Action time (On/Off)", type: "time",  required: true)
		input name: "actionMode", type: "bool", title: "Turn on or off", required: true, defaultValue: false, submitOnChange: true  
        if(actionMode == true){
	    input name: "action1", type: "bool", title: "Turn switch off as well? ", required: true, defaultValue: false, submitOnChange: true  
        }
   		if(action1 == true){
		input (name: "stopTime", title: "Stop time (Off)", type: "time",  required: false)
	    }
	}    
        
    section("") {
        input "days", "enum", title: "Select Days of the Week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
	
	 section(){
		input "switch1",  "capability.switch", title: "Control switch(es)", multiple: true, required: false
	}    
   
      section(){input "debugMode", "bool", title: "Enable logging", required: true, defaultValue: false }  
    
    
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {	 
 version()
     schedule(startTime, checkNow1)
    if(action1 == true){schedule(stopTime, checkNow2)}
    
    logCheck()
    setDefaults()
    version()
    log.info "Initialised with settings: ${settings}"

    
     }



def checkNow1 (evt) {
   
LOGDEBUG("Operation is scheduled for $startTime")
    
    if(days){
     def df = new java.text.SimpleDateFormat("EEEE")
   df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the input 'days', contain today?
    def dayCheck = days.contains(day)
    if (dayCheck) {
    
LOGDEBUG("Scheduled for operation today")
LOGDEBUG("Switching now...")
        
        if(state.modeAction == true){ switch1.on() }    
        if(state.modeAction == false){ switch1.off() }    
        

       
               
    			  }
    else {
LOGDEBUG("Not scheduled for today!")
		 } 
    }
    
    if(!days){
       if(state.modeAction == true){ switch1.on() }    
       if(state.modeAction == false){ switch1.off() }    
    }
        
}

def checkNow2 (evt) {
	
LOGDEBUG("Operation is scheduled for $stopTime")
    if(days){
 def df = new java.text.SimpleDateFormat("EEEE")
   df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the input 'days', contain today?
    def dayCheck = days.contains(day)
    if (dayCheck) {
        
LOGDEBUG("Scheduled for operation today")
    	
LOGDEBUG("Switching now...")       
        if(state.actionType == true){switch1.off()}
    }
    else {
 LOGDEBUG("Not scheduled for today!")
		 } 
   
    }
     if(!days){
         if(state.actionType == true){switch1.off()}
     }
 				  
}

// define debug action
def logCheck(){
state.checkLog = debugMode
if(state.checkLog == true){
log.info "All Logging Enabled"
}
else if(state.checkLog == false){
log.info "Further Logging Disabled"
}

}
def LOGDEBUG(txt){
    try {
    	if (settings.debugMode) { log.debug("${app.label.replace(" ","_").toUpperCase()}  (App Version: ${state.version}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}



def version(){
	schedule("0 0 9 ? * FRI *", updateCheck) //  Check for updates at 9am every Friday
//	updateCheck()  
    
   
    
}

def display(){
 //   setDefaults()
  	
    
	if(state.status){
	section{paragraph "<img src='http://update.hubitat.uk/icons/cobra3.png''</img> Version: $state.version <br><font face='Lucida Handwriting'>$state.Copyright </font>"}
       
        }
   

    if(state.status != "<b>** This app is no longer supported by $state.author  **</b>"){
     section(){ input "updateBtn", "button", title: "$state.btnName"}
    }
    
    section(){
   //     log.info "app.label = $app.label"
    input "pause1", "bool", title: "Pause This App", required: true, submitOnChange: true, defaultValue: false  
     }
       
    if(state.status != "Current"){
	section{ 
	paragraph "<b>Update Info:</b> <BR>$state.UpdateInfo <BR>$state.updateURI"
     }
    }
	section(" ") {
      input "updateNotification", "bool", title: "Send a 'Pushover' message when an update is available", required: true, defaultValue: false, submitOnChange: true 
      if(updateNotification == true){ input "speaker", "capability.speechSynthesis", title: "PushOver Device", required: true, multiple: true}
    }
    

}

def checkButtons(){
    LOGDEBUG("Running checkButtons")
    appButtonHandler("updateBtn")
}


def appButtonHandler(btn){
    state.btnCall = btn
    if(state.btnCall == "updateBtn"){
       log.info "Checking for updates now..."
        updateCheck()
        pause(3000)
  		state.btnName = state.newBtn
        runIn(2, resetBtnName)
    }
    if(state.btnCall == "updateBtn1"){
    state.btnName1 = "Click Here" 
    httpGet("https://github.com/CobraVmax/Hubitat/tree/master/Apps' target='_blank")
    }
    
}   
def resetBtnName(){
//    log.info "Resetting Update Button Name"
    if(state.status != "Current"){
	state.btnName = state.newBtn
    }
    else{
 state.btnName = "Check For Update" 
    }
}    
    
def pushOverNow(inMsg){
    if(updateNotification == true){  
     newMessage = inMsg
  log.info "Message = $newMessage " 
     state.msg1 = '[L]' + newMessage
	speaker.speak(state.msg1)
    }
}

def pauseOrNot(){
LOGDEBUG(" Calling 'pauseOrNot'...")
    state.pauseNow = pause1
        if(state.pauseNow == true){
            state.pauseApp = true
            if(app.label){
            if(app.label.contains('red')){
                log.warn "Paused"}
            else{app.updateLabel(app.label + ("<font color = 'red'> (Paused) </font>" ))
              LOGDEBUG("App Paused - state.pauseApp = $state.pauseApp ")   
                }
    
            }
        }
    
     if(state.pauseNow == false){
         state.pauseApp = false
         if(app.label){
     if(app.label.contains('red')){ app.updateLabel(app.label.minus("<font color = 'red'> (Paused) </font>" ))
     LOGDEBUG("App Released - state.pauseApp = $state.pauseApp ")                          
                                  }
         }
  }    
    
}


def updateCheck(){
    setVersion()
	def paramsUD = [uri: "http://update.hubitat.uk/cobra.json"]
       	try {
        httpGet(paramsUD) { respUD ->
 //  log.warn " Version Checking - Response Data: ${respUD.data}"   // Troubleshooting Debug Code 
       		def copyrightRead = (respUD.data.copyright)
       		state.Copyright = copyrightRead
            def updateUri = (respUD.data.versions.UpdateInfo.GithubFiles.(state.InternalName))
            state.updateURI = updateUri   
            
            def newVerRaw = (respUD.data.versions.Application.(state.InternalName))
            def newVer = (respUD.data.versions.Application.(state.InternalName).replace(".", ""))
       		def currentVer = state.version.replace(".", "")
      		state.UpdateInfo = (respUD.data.versions.UpdateInfo.Application.(state.InternalName))
                state.author = (respUD.data.author)
           
		if(newVer == "NLS"){
            state.status = "<b>** This app is no longer supported by $state.author  **</b>"  
             log.warn "** This app is no longer supported by $state.author **" 
            
      		}           
		else if(currentVer < newVer){
        	state.status = "<b>New Version Available (Version: $newVerRaw)</b>"
        	log.warn "** There is a newer version of this app available  (Version: $newVerRaw) **"
        	log.warn "** $state.UpdateInfo **"
             state.newBtn = state.status
            def updateMsg = "There is a new version of '$state.ExternalName' available (Version: $newVerRaw)"
            pushOverNow(updateMsg)
       		} 
		else{ 
      		state.status = "Current"
       		log.info "You are using the current version of this app"
       		}
      					}
        	} 
        catch (e) {
        	log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI -  $e"
    		}
    if(state.status != "Current"){
		state.newBtn = state.status
        
    }
    else{
        state.newBtn = "No Update Available"
    }
        
        
}



def setVersion(){
		state.version = "1.0.0"	 
		state.InternalName = "DailySwitchEventChild"
    	state.ExternalName = "Daily Switch Event Child"
}

def setDefaults(){
    log.info "Initialising defaults..." 
    checkButtons()
    resetBtnName()
    pauseOrNot()
    if(pause1 == null){pause1 = false}
    if(state.pauseApp == null){state.pauseApp = false}  


 // add any further default settings below here               
 state.modeAction = actionMode
 state.actionType = action1  


}


