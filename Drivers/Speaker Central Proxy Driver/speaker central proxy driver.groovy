/**
 *  Speaker 'TTS' Proxy
 *
 *  Copyright 2019 Andrew Parker
 *
 *  Driver for use with 'Speaker Central'
 *  
 *  This driver is free!
 *
 *  Donations to support development efforts are welcomed via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm very happy for you to use this driver without a donation, but if you find it useful
 *  then it would be nice to get a 'shout out' on the forum! -  @Cobra
 *  Have an idea to make this driver better?  - Please let me know :)
 *
 *  
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
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @Cobra
 *
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  Last Update 13/05/2019
 *
 *
 *
 *  
 *
 *  V1.2.0 - added ActiveDevices as an attribute (data from new parent app) & debug "playTextAndRestore"
 *  V1.0.0 - POC (Initial Release)
 */


metadata {
	definition (name: "ProxySpeechPlayer", namespace: "Cobra", author: "Andrew Parker") {
		
		
        capability "Music Player"
		capability "Speech Synthesis"
		capability "Notification"
		
		
		command "InputDevice", ["string"]
		command "playTextAndRestore",["string"]
		command "setVolume", ["number"]
	 	command "activeDevices", ["string"]
		attribute "deviceNotification", "string"
		attribute "speak", "string"
		attribute "level", "number"
		attribute "volume", "number"
		attribute "playTextAndRestore", "string"
		attribute  " ", "string"
		attribute "DriverVersion", "string"
        attribute "DriverStatus", "string"
        attribute "DriverUpdate", "string" 
		attribute "ActiveDevices", "string"
	}
preferences() {
    
     section("") {
        input "debugMode", "bool", title: "Enable logging", required: true, defaultValue: false  
 		}  
 }   
}

def installed(){
    initialize()
}

def updated(){
    initialize()
}



def initialize() {
   logCheck()
    version()

}

def activeDevices(lst){
	def deviceList = lst
	sendEvent(name: "ActiveDevices", value: deviceList, isStateChange: true)
}


def playTextAndRestore(text, level = null){
	LOGDEBUG( "ProxySpeechPlayer sending playTextAndRestore: ${text}")
    sendEvent(name: "playTextAndRestore", value: text, isStateChange: true)    
	
}


def speak(text){
	LOGDEBUG("ProxySpeechPlayer sending speak: ${text}")
    sendEvent(name: "speak", value: text, isStateChange: true)   

}

def deviceNotification(text){
	LOGDEBUG("ProxySpeechPlayer sending deviceNotification: ${text}")
    sendEvent(name: "deviceNotification", value: text, isStateChange: true)   

}

def setLevel(level) {
	LOGDEBUG( "ProxySpeechPlayer sending setLevel: ${level}")
    sendEvent(name: "level", value: level, isStateChange: true)    
} 

def setVolume(level) {
	LOGDEBUG( "ProxySpeechPlayer sending setVolume: ${level}")
    sendEvent(name: "volume", value: level, isStateChange: true)   
} 




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
    	if (settings.debugMode) { log.debug("Device Version: ${state.version}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}        


def version(){
    updateCheck()
   schedule("0 0 9 ? * FRI *", updateCheck)
}
    

def updateCheck(){
    setVersion()
	def paramsUD = [uri: "http://update.hubitat.uk/json/${state.CobraAppCheck}"] 
       	try {
        httpGet(paramsUD) { respUD ->
//  log.warn " Version Checking - Response Data: ${respUD.data}"   // Troubleshooting Debug Code **********************
       		def copyrightRead = (respUD.data.copyright)
       		state.Copyright = copyrightRead
            def newVerRaw = (respUD.data.versions.Driver.(state.InternalName))
	//		log.warn "$state.InternalName = $newVerRaw"
  			def newVer = newVerRaw.replace(".", "")
//			log.warn "$state.InternalName = $newVer"
			state.newUpdateDate = (respUD.data.Comment)
       		def currentVer = state.version.replace(".", "")
      		state.UpdateInfo = "Updated: "+state.newUpdateDate + " - "+(respUD.data.versions.UpdateInfo.Driver.(state.InternalName))
            state.author = (respUD.data.author)
			state.icon = (respUD.data.icon)
           
		if(newVer == "NLS"){
            state.Status = "<b>** This driver is no longer supported by $state.author  **</b>"       
            log.warn "** This driver is no longer supported by $state.author **"      
      		}           
		else if(currentVer < newVer){
        	state.Status = "<b>New Version Available (Version: $newVerRaw)</b>"
        	log.warn "** There is a newer version of this driver available  (Version: $newVerRaw) **"
        	log.warn "** $state.UpdateInfo **"
       		} 
		else{ 
      		state.Status = "Current"
      		log.info "You are using the current version of this driver"
       		}
      					}
        	} 
        catch (e) {
        	log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI -  $e"
    		}
   		if(state.Status == "Current"){
			state.UpdateInfo = "N/A"
		    sendEvent(name: "DriverUpdate", value: state.UpdateInfo, isStateChange: true)
	 	    sendEvent(name: "DriverStatus", value: state.Status, isStateChange: true)
			}
    	else{
	    	sendEvent(name: "DriverUpdate", value: state.UpdateInfo, isStateChange: true)
	     	sendEvent(name: "DriverStatus", value: state.Status, isStateChange: true)
	    }   
 			sendEvent(name: " ", value: state.icon +"<br>" +state.Copyright +"<br> <br>", isStateChange: true)
    		sendEvent(name: "DriverVersion", value: state.version, isStateChange: true)
    
    
    	//	
}

def setVersion(){
    state.version = "1.2.0"
    state.InternalName = "SpeakerCentralDriver"
   	state.CobraAppCheck = "speakercentraldriver.json"
    
    
}  

