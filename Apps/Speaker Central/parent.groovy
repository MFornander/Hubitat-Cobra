/**
 *  Design Usage:
 *  This is the 'Parent' app for 'Speaker Central'
 *
 *
 *  Copyright 2019 Andrew Parker
 *  
 *  This SmartApp is free!
 *
 *  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm very happy for you to use this app without a donation, but if you find it useful
 *  then it would be nice to get a 'shout out' on the forum! -  @Cobra
 *  Have an idea to make this app better?  - Please let me know :)
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
 *  Last Update: 13/05/2019
 *
 *  Changes:
 *
 *
 *  V1.3.0 - Added switch to be able to use existing proxy device
 *  V1.2.0 - Added auto creation of & subscription to virtual device
 *  V1.1.0 - Moved vdevice to parent & Added 'active speakers' sent to driver
 *  V1.0.0 - POC
 *
 */



definition(
    name:"Speaker Central",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Parent App for Average All ChildApps ",
     
   parent: "Cobra:Cobra Apps",  // ******** Comment this out if not using the 'Cobra Apps' container  ***************
    
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
    )







preferences {page name: "mainPage", title: "", install: true, uninstall: true}
def installed() {initialize()}
def updated() {initialize()}
def initialize() {
	unsubscribe()
	if(useOld == false){addDevices()}
    version()
    log.debug "Initialised with settings: ${settings}"
    log.info "There are ${childApps.size()} child apps"
    childApps.each {child ->
    log.info "Child app: ${child.label}"
    } 

 	state.activeDeviceList = null //  if enabled, active device list will reset each time app is saved

	subscribe(state.vdevice, "speak", speakHandler) 
	subscribe(state.vdevice, "setLevel", volumeHandler)
	subscribe(state.vdevice, "playTextAndRestore", playTextHandler)
	subscribe(state.vdevice, "deviceNotification", notifyHandler)
	// Ready for later feature....
	state.default = false
}

def mainPage() {
    dynamicPage(name: "mainPage") {   
	installCheck()
	if(state.appInstalled == 'COMPLETE'){
	display()
		section (){	
	input "useOld", "bool", title: "Use Existing Speech Proxy Device", required: true, defaultValue: false, submitOnChange: true}	
		if(useOld == true){section("Proxy Device") {input "vDevice1", "device.ProxySpeechPlayer", title: "Proxy Speaker Virtual Device"}}	
			

	section (){app(name: "speakerApp", appName: "Speaker Central Child", namespace: "Cobra", title: "<b>Add a new child</b>", multiple: true)}
	displayDisable()
	}
	}
  }


def version(){
//	unschedule(updateCheck)
    resetBtnName()
    schedule("0 0 9 ? * FRI *", updateCheck) //  Check for updates at 9am every Friday
    updateCheck()  
    checkButtons()
   
}


def installCheck(){         
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
	section{paragraph "Please hit 'Done' to load this app into the Cobra Apps container"}
		addDevices()
	  }
	else{
 //      log.info "Parent Installed OK"  
    }
	}

def display(){
	if(state.status){section(){paragraph "<img src='http://update.hubitat.uk/icons/cobra3.png''</img> Version: $state.version <br><font face='Lucida Handwriting'>$state.Copyright </font>"}}
	if(state.status != "<b>** This app is no longer supported by $state.author  **</b>"){section(){input "updateBtn", "button", title: "$state.btnName"}}
	if(state.status != "Current"){
		section(){paragraph "<hr><b>Updated: </b><i>$state.Comment</i><br><br><i>Changes in version $state.newver</i><br>$state.UpdateInfo<hr><b>Update URL: </b><font color = 'red'> $state.updateURI</font><hr>"}
		}
		section(){
		input "updateNotification", "bool", title: "Send a 'Pushover' message when an update is available for either the parent or the child app", required: true, defaultValue: false, submitOnChange: true 
		if(updateNotification == true){ input "speakerUpdate", "capability.speechSynthesis", title: "PushOver Device", required: true, multiple: true}
		}
	
}

def displayDisable(){
	if(app.label){
	section("<hr>"){
		input "disableAll1", "bool", title: "Disable <b>all</b> <i>'${app.label}'</i> child apps", required: true, defaultValue: false, submitOnChange: true
		state.allDisabled1 = disableAll1
		stopAll()
	}
	section("<hr>"){}
	}
	else{
	section("<hr>"){
		input "disableAll1", "bool", title: "Disable <b><i>ALL</i></b> child apps ", required: true, defaultValue: false, submitOnChange: true
		state.allDisabled1 = disableAll1
		stopAll()
	}
	section("<hr>"){}
	}
	
}


def addDevices() {
	log.debug("Running addDevices")
	try { 
		hub = location.hubs[0] 
	} catch (error) { 
		log.error "Hub not detected.  You must have a hub to install this app."
		return
	}
	def hubId = hub.id
	def rnd = new Random()
	def newDNI = (rnd.nextInt(200))
	def virtualDni = "ProxySpeechPlayer"
	def isChild = getChildDevice(virtualDni)

	if (!isChild) {
		addChildDevice(
			"Cobra",	
			"ProxySpeechPlayer", 
			virtualDni,
			hubId, [
				"label" : "Speaker Central TTS Device",
				"name" : "ProxySpeechPlayer"
			]
		)
			log.info "Installed Speech Proxy Device"
		
	}
	state.vdevice = getChildDevice(virtualDni)
		log.info "Installed Device = $state.vdevice "
}

def speakHandler(evt){
	def msgIn = evt.value.toString()
	if(state.default == false){
	childApps.each { child ->
			child.speakHandler(msgIn)}
		log.info "Message = $msgIn"
	}
	
	
}

def playTextHandler(evt){
	def msgIn = evt.value.toString()
	if(state.default == false){
	childApps.each { child ->
			child.playTextHandler(msgIn)}
	}
	
}

def notifyHandler(evt){
	def msgIn = evt.value.toString()
	if(state.default == false){
	childApps.each { child ->
			child.notifyHandler(msgIn)}
	}
	
}



def volumeHandler(evt){
	def volIn = evt.value // .toString()
	if(state.default == false){
	childApps.each { child ->
			child.volumeHandler(volIn)}
	}
}


def askVDevice(){
	if(useOld){state.vdevice = vDevice1}
	else{
	if(state.vdevice != null){
	//	log.warn "askDevice - state.vdevice = $state.vdevice"
		childApps.each { child ->
			child.sendVdevice("${state.vdevice}")}
	}
	if(state.vdevice == null){log.warn "No Virtual Device Set"}
  }
}
	

def activeList(childDevice1, activeState1){
	if(state.activeDeviceList == null){state.activeDeviceList = " "}
//	log.warn "device = $childDevice1 - State = $activeState1"
	
	if(activeState1 == "active" && !state.activeDeviceList.contains("${childDevice1}")){
		state.activeDeviceList += "<br>${childDevice1}, "
	
	}
	if(activeState1 == "inactive"){
		if(state.activeDeviceList != " " && state.activeDeviceList.contains("${childDevice1}")){
			state.activeDeviceList -= "<br>${childDevice1}, "}}
	if(state.activeDeviceList == null){ 
		state.activeDeviceList = " "
	log.info "There are no currently active devices"

	}
	if(state.activeDeviceList != " "){
	log.info "Currently Active Devices = $state.activeDeviceList"
		vDevice1.activeDevices(state.activeDeviceList)

		
	}
}




def stopAll(){
	
	if(state.allDisabled1 == true) {
	log.debug "state.allDisabled1 = TRUE"
	state.msg2 = "Disabled by parent"
	childApps.each { child ->
	child.stopAllChildren(state.allDisabled1, state.msg2)
	log.warn "Disabling ChildApp: $child.label"
	}
	}	
	
	if(state.allDisabled1 == false){
	log.debug "state.allDisabled1 = FALSE"
	state.msg3 = "Enabled by parent"
	childApps.each { child ->
	child.stopAllChildren(state.allDisabled1, state.msg3)	
//	log.trace "Enabling ChildApp: $child.label "
	}
	}
}

def stopAllParent(stopNowCobra, msgCobra){
	state.allDisabled1 = stopNowCobra
	def msgNowCobra = msgCobra
	log.info " Message from Cobra Apps -  Disable = $stopNowCobra"
	childApps.each { child ->
	child.stopAllChildren(state.allDisabled1, msgNowCobra)
	//	if(stopNowCobra == true){log.warn "Disabling ChildApp: $child.label"}
	//	if(stopNowCobra == false){log.trace "Enabling ChildApp: $child.label "}
		
		
		
	}
}	


def checkButtons(){
//    log.debug "Running checkButtons"
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
    
}  
 
def resetBtnName(){
//    log.info "Resetting Button"	
	if(state.status != "Current"){
	state.btnName = state.newBtn
	    }
	else{
 	state.btnName = "Check For Update" 
	}
}    
    


def updateCheck(){
    setVersion()
    def paramsUD = [uri: "http://update.hubitat.uk/json/${state.CobraAppCheck}"]
    
       	try {
        httpGet(paramsUD) { respUD ->
//  log.warn " Version Checking - Response Data: ${respUD.data}"   // Troubleshooting Debug Code 
       		def copyrightRead = (respUD.data.copyright)
       		state.Copyright = copyrightRead
            def commentRead = (respUD.data.Comment)
       		state.Comment = commentRead

            def updateUri = (respUD.data.versions.UpdateInfo.GithubFiles.(state.InternalName))
            state.updateURI = updateUri   
            
            def newVerRaw = (respUD.data.versions.Application.(state.InternalName))
            state.newver = newVerRaw
            def newVer = (respUD.data.versions.Application.(state.InternalName).replace(".", ""))
       		def currentVer = state.version.replace(".", "")
      		state.UpdateInfo = (respUD.data.versions.UpdateInfo.Application.(state.InternalName))
            state.author = (respUD.data.author)
           
		if(newVer == "NLS"){
            state.status = "<b>** This app is no longer supported by $state.author  **</b>"  
             log.warn "** This app is no longer supported by $state.author **" 
            
      		}           
		else if(currentVer < newVer){
        	state.status = "<b>New Version Available ($newVerRaw)</b>"
        	log.warn "** There is a newer version of this app available  (Version: $newVerRaw) **"
        	log.warn " Update: $state.UpdateInfo "
             state.newBtn = state.status
            state.updateMsg = "There is a new version of '$state.ExternalName' available (Version: $newVerRaw)"
            pushOverUpdate(state.updateMsg)
       		} 
		else{ 
      		state.status = "Current"
       		log.info("You are using the current version of this app")
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

def childUpdate(set, msg){
	if(state.msgDone == false){
	state.childUpdate = set.value
	state.upMsg = msg.toString()
	if(state.childUpdate == true){
	pushOverUpdate(state.upMsg)	
	state.msgDone = true	
			}	
		}
	else{
//		log.info "Message already sent - Not able to send again today"
	    }		
}
def resetMsg(){state.msgDone = false}
def pushOverUpdate(inMsg){
    if(updateNotification == true){  
    newMessage = inMsg
   log.debug"PushOver Message = $newMessage "  
    state.msg1 = '[L]' + newMessage
    speakerUpdate.speak(state.msg1)
    }
}


def setVersion(){
		state.version = "1.3.0"	 
		state.InternalName = "SpeakerCentralParent" 
    	state.ExternalName = "Speaker Central Parent"
    	state.CobraAppCheck = "speakercentral.json"
}





