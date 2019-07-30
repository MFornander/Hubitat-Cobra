/*
 *  Aeon HEM V2 (Gen5)
 *
 *  This was originally an ST DTH and was subsiquently worked on by @vjv to bring it to Hubitat
 *  I have reworked it by adding  calculations for cummulative reading and sending a Pushover summary
 *
 *  I have to credit @ogiewon for his excellent Pushover code which I have used here.
 *  My thanks also goes to @stephack for the code amendments after Hubitat update to httpPost
 *	
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Last Update 12/08/2018
 *
 *
 *  V1.0.0 POC
 */

// metadata
metadata {
	definition (name: "Aeon HEM 2nd Edition", namespace: "Cobra", author: "A.J Parker") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		command "manualReset"
        
        
	    attribute "PowerDisplay", "string"  
		attribute "DriverAuthor", "string"
        attribute "DriverVersion", "string"
        attribute "DriverStatus", "string"
		attribute "DriverUpdate", "string"
        
        attribute "LastReset", "string"
        attribute "NextReset", "string"
        attribute "voltage", "string"
        attribute "current", "string"
        attribute "cost", "string" 
    //    attribute "power1", "string"
     //   attribute "current1", "string" 
    //    attribute "power2", "string"
    //    attribute "current2", "string"
    //    attribute "power3", "string"
     //   attribute "current3", "string"
        
		fingerprint deviceId: "0x3101", inClusters: "0x98"
		fingerprint inClusters: "0x5E,0x86,0x72,0x32,0x56,0x60,0x70,0x59,0x85,0x7A,0x73,0xEF,0x5A", outClusters: "0x82"
        fingerprint deviceId: "0xC1", mfr: "0x86", inClusters: "0x70,0x32,0x60,0x85,0x56,0x72,0x86"
        fingerprint deviceId: "0x1C", mfr: "0x86", inClusters: "0x70,0x32,0x60,0x85,0x72,0x86"
        fingerprint deviceId: "0x5F", mfr: "0x86", inClusters: "0x5E,0x86,0x72,0x32,0x56,0x70,0x59,0x85,0x7A,0x73,0x98", outClusters: "0x5A"
	}





	preferences {
		
		input "debugMode", "bool", title: "Enable debug logging", required: true, defaultValue: false
		
		input "kWhCost", "string",
			title: "Cost per kWh",
			description: "Your Electric Bill Cost Per kWh",
			defaultValue: "0.13" as String,
			required: false,
			displayDuringSetup: true
		input "monitorInterval1", "integer",
			title: "Volts & kWh Report (Seconds)",
			description: "Interval for Volts & kWh Report",
			defaultValue: 120,
			range: "1..4294967295?",
			required: false,
			displayDuringSetup: true
		input "monitorInterval2", "integer",
			title: "Amps Report (Seconds)",
			description: "Interval for Amps Report",
			defaultValue: 120,
			range: "1..4294967295?",
			required: false,
			displayDuringSetup: true
		input "monitorInterval3", "integer",
			title: "Watts Report (Seconds)",
			description: "Interval for Watts Report",
			defaultValue: 5,
			range: "1..4294967295?",
			required: false,
			displayDuringSetup: true
        
/*      
        
        input "clampNum", "enum", title: "Number Of Clamps", required: true, options: [ "1", "2", "3"]
        if(clampNum == "1"){
        input "totalThresh", "enum", title: "Overall Wattage Threshold to Report Change", required: true, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        input "c1Thresh", "enum", title: "Clamp1 Wattage Threshold to Report Change", required: false, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        }
         if(clampNum == "2"){
        input "totalThresh", "enum", title: "Overall Wattage Threshold to Report Change", required: true, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        input "c1Thresh", "enum", title: "Clamp1 Wattage Threshold to Report Change", required: false, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        input "c2Thresh", "enum", title: "Clamp2 Wattage Threshold to Report Change", required: false, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
         }     
           if(clampNum == "3"){
        input "totalThresh", "enum", title: "Overall Wattage Threshold to Report Change", required: true, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        input "c1Thresh", "enum", title: "Clamp1 Wattage Threshold to Report Change", required: false, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        input "c2Thresh", "enum", title: "Clamp2 Wattage Threshold to Report Change", required: false, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
        input "c3Thresh", "enum", title: "Clamp3 Wattage Threshold to Report Change", required: false, options: [ "1", "5", "10", "20", "30", "40", "50", "100"]
           }
        
*/        
  //      input "useMode", "enum", title: "Recording Mode", required: true, options: ["Production", "Consumption"]
        input "unitSet", "bool", title: "Display Data Units (Not for use with dashboards or apps)", required: true, defaultValue: false
        input "currencyFormat", "enum", title: "Currency", required: true, options: ["£", "\$","€"]
        input "resetInterval", "enum", title: "When to Automatically Reset?", required: true, submitOnChange: true, defaultValue: "None", 
            options: ["No Auto Reset", "Daily (00:01)", "Weekly (Monday 00:01)", "Monthly (First day 00:01)", "Monthly (Custom Date)", "Annually (First Day 00:01)"]
        if(resetInterval == "Monthly (Custom Date)"){
            input "date1", "enum", title: "What Date Every Month", required: true, options: [ "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"]
        }
        input "pushOver", "bool", title: "Send Pushover Message (before reset)", required: true, defaultValue: false
         if(pushOver == true){
         input("apiKey", "text", title: "API Key:", description: "Pushover API Key")
  		input("userKey", "text", title: "User Key:", description: "Pushover User Key")
  		input("deviceName", "text", title: "Optional Device Name:", description: "If blank, all devices get notified")
        input("priority", "enum", title: "Default Message Priority", defaultValue: "NORMAL", options:["LOW", "NORMAL", "HIGH"])
         }
        input "refreshInterval", "enum", title: "Auto Refresh Interval", required: true, defaultValue: "5 Minutes", options: ["Manual Refresh Only", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
	
	}

}

def initialize(){
	updated()
	
}

def updated(){
    version()
	if (state.sec && !isConfigured()) {
		response(configure())
	}
    def refreshIntervalCmd = (settings?.refreshInterval ?: "5 Minutes").replace(" ", "")
    if(refreshInterval == "Manual Refresh Only"){log.info "MANUAL REFRESH ONLY"}
    else{ "runEvery${refreshIntervalCmd}"(refresh)}
    setReset()
    state.Currency = currencyFormat
    state.DisplayUnits = unitSet
    refresh()
}



def setReset(){
   def reset1 = resetInterval 
if(reset1) 
    if(reset1 == "No Reset"){
        log.info " NO reset interval configured"
    }
    else if (reset1 == "Daily (00:01)"){
        log.info "Reset configured for every day"
        schedule("0 1 0 1/1 * ? *", reset) 
    }
    else if (reset1 == "Weekly (Monday 00:01)"){
        log.info "Reset configured for every week"
        schedule("0 1 0 ? * MON *", reset)  
        
    }
    else if (reset1 == "Monthly (First day 00:01)"){
        log.info "Reset configured for every month"
        schedule("0 1 0 1 1/1 ? *", reset) 
    }
    else if (reset1 == "Annually (First Day 00:01)"){
        log.info "Reset configured for every year"
        schedule("0 1 0 1 1 ? *", reset) 
    }

    else if(reset1 ==  "Monthly (Custom Date)"){
state.ResetDate = date1
        state.CronSchedule = "0 1 0 ${state.ResetDate} 1/1 ? *"
       log.info "Custom reset configured for every month on: $state.ResetDate" 						
        schedule(state.CronSchedule, reset)
    }
}


def parse(String description){
	def result = null
	if (description.startsWith("Err 106")) {
		state.sec = 0
		result = createEvent( name: "secureInclusion", value: "failed", isStateChange: true,
			descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.")
	} else if (description != "updated") {
		def cmd = zwave.parse(description, [0x32: 3, 0x56: 1, 0x59: 1, 0x5A: 1, 0x60: 3, 0x70: 1, 0x72: 2, 0x73: 1, 0x82: 1, 0x85: 2, 0x86: 2, 0x8E: 2, 0xEF: 1])
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
LOGDEBUG( "Parsed '${description}' to ${result.inspect()}")
	return result
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x56: 1, 0x59: 1, 0x5A: 1, 0x60: 3, 0x70: 1, 0x72: 2, 0x73: 1, 0x82: 1, 0x85: 2, 0x86: 2, 0x8E: 2, 0xEF: 1])
	state.sec = 1
LOGDEBUG( "encapsulated: ${encapsulatedCommand}")
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
	response(configure())
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    LOGDEBUG( "---CONFIGURATION REPORT V1--- ${device.displayName} parameter ${cmd.parameterNumber} with a byte size of ${cmd.size} is set to ${cmd.configurationValue}")
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    LOGDEBUG( "---ASSOCIATION REPORT V2--- ${device.displayName} groupingIdentifier: ${cmd.groupingIdentifier}, maxNodesSupported: ${cmd.maxNodesSupported}, nodeId: ${cmd.nodeId}, reportsToFollow: ${cmd.reportsToFollow}")
}

def zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd) {
    def meterTypes = ["Unknown", "Electric", "Gas", "Water"]
    def electricNames = ["energy", "energy", "power", "count",  "voltage", "current", "powerFactor", "volt", "unknown"]
    def electricUnits = ["kWh",    "kVAh",   "W",     "pulses", "V",       "A",       "Power Factor", ""]

    //NOTE ScaledPreviousMeterValue does not always contain a value
    def previousValue = cmd.scaledPreviousMeterValue ?: 0

    //Here is where all HEM polled values are defined. Scale(0-7) is in reference to the Aeon Labs HEM Gen5 data for kWh, kVAh, W, V, A, and M.S.T. respectively.
    //If scale 7 (M.S.T.) is polled, you would receive Scale2(0-1) which is kVar, and kVarh respectively. We are ignoring the Scale2 ranges in this device handler.
    def map = [ name: electricNames[cmd.scale], unit: electricUnits[cmd.scale], displayed: state.display]
    switch(cmd.scale) {
        case 0: //kWh
						previousValue = device.currentValue("energy") ?: cmd.scaledPreviousMeterValue ?: 0
						BigDecimal costDecimal = cmd.scaledMeterValue * (kWhCost as BigDecimal)
						def costDisplay = String.format("%5.2f",costDecimal)
        state.TotalCost = costDisplay
        if(state.DisplayUnits == false){
        sendEvent(name: "cost", value: costDisplay, unit: "${state.Currency}", isStateChange: true) 
        }
        if(state.DisplayUnits == true){
        sendEvent(name: "cost", value: state.Currency +costDisplay, isStateChange: true)
        }    
            if(state.DisplayUnits == true){
                map.value = (cmd.scaledMeterValue + " kwh")}
        	 if(state.DisplayUnits == false){
                 map.value =  cmd.scaledMeterValue}
            
        state.TotalKwh = map.value
            break;
        case 1: //kVAh (not used in the U.S.)
            map.value = cmd.scaledMeterValue
            break;
        case 2: //Watts
            previousValue = device.currentValue("power") ?: cmd.scaledPreviousMeterValue ?: 0
         if(state.DisplayUnits == true){
             map.value = (Math.round(cmd.scaledMeterValue) +" watts")}
         if(state.DisplayUnits == false){
             map.value = Math.round(cmd.scaledMeterValue)}
        
        
            break;
        case 3: //pulses
						map.value = Math.round(cmd.scaledMeterValue)
            break;
        case 4: //Volts
            previousValue = device.currentValue("voltage") ?: cmd.scaledPreviousMeterValue ?: 0
        	map1 = (cmd.scaledMeterValue)
        	map2 = map1.toDouble()
        if(state.DisplayUnits == true){
            map.value = (map2.round(2) +" volts")}
        if(state.DisplayUnits == false){
            map.value = map2.round(2)}
            break;
        case 5: //Amps
            previousValue = device.currentValue("current") ?: cmd.scaledPreviousMeterValue ?: 0
         if(state.DisplayUnits == true){
             map.value = (cmd.scaledMeterValue +" amps")}
         if(state.DisplayUnits == false){
             map.value = cmd.scaledMeterValue}
        
            break;
        case 6: //Power Factor
        case 7: //Scale2 values (not currently implimented or needed)
            map.value = cmd.scaledMeterValue
            break;
        default:
            break;
    }
createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	//This is where the HEM clamp1 and clamp2 (subdevice) report values are defined. Scale(2,5) is in reference to the Aeon Labs HEM Gen5 (subdevice) data for W, and A respectively.
	//Z-Wave Command Class 0x60 (multichannelv3) is necessary to interpret the subdevice data from the HEM clamps.
	//In addition, "cmd.commandClass == 50" and "encapsulatedCommand([0x30: 1, 0x31: 1])" below is necessary to properly receive and inturpret the encasulated subdevice data sent to the hub by the HEM.
	//The numbered "command class" references: 50, 0x30v1, and 0x31v1 do not seem to be true Z-Wave Command Classes and any correlation is seemingly coincidental.
	//It should also be noted that without the above, the data received will not be processed here under the 0x60 (multichannelv3) command class and you will see unhandled messages from the HEM along with references to command class 50 as well as Meter Types 33, and 161.
	//sourceEndPoint 1, and 2 are the Clamps 1, and 2.
	def dispValue
	def newValue
	def formattedValue
	def MAX_AMPS = 60
	def MAX_WATTS = 12000
	if (cmd.commandClass == 50) { //50 is likely a manufacturer specific code, Z-Wave specifies this as a "Basic Window Covering" so it's not a true Z-Wave Command Class.   
		def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1]) // The documentation on working with Z-Wave subdevices and the technical specs from Aeon Labs do not explain this adequately, but it's necessary.
	LOGDEBUG( ("Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}"))
		if (encapsulatedCommand) {
			if (cmd.sourceEndPoint == 1) {
				if (encapsulatedCommand.scale == 4 ) {
						newValue = Math.round(encapsulatedCommand.scaledMeterValue)
	                    if (newValue > MAX_WATTS) { return }
						formattedValue = newValue
						dispValue = "${formattedValue}"
			//			sendEvent(name: "power1", value: dispValue as String, unit: "", descriptionText: "L1 Power: ${formattedValue} Watts", isStateChange: true)
				}
				if (encapsulatedCommand.scale == 5 ) {
						newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
	                    if (newValue > MAX_AMPS) { return }
						formattedValue = String.format("%5.2f", newValue)
						dispValue = "${formattedValue}"
			//			sendEvent(name: "current1", value: dispValue as String, unit: "", descriptionText: "L1 Current: ${formattedValue} Amps", isStateChange: true)
				}
			}
			else if (cmd.sourceEndPoint == 2) {
				if (encapsulatedCommand.scale == 4 ) {
						newValue = Math.round(encapsulatedCommand.scaledMeterValue)
	                    if (newValue > MAX_WATTS) { return }
						formattedValue = newValue
						dispValue = "${formattedValue}"
			//			sendEvent(name: "power2", value: dispValue as String, unit: "", descriptionText: "L2 Power: ${formattedValue} Watts", isStateChange: true)
				}
				if (encapsulatedCommand.scale == 5 ) {
						newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
	                    if (newValue > MAX_AMPS) { return }
						formattedValue = String.format("%5.2f", newValue)
						dispValue = "${formattedValue}"
			//			sendEvent(name: "current2", value: dispValue as String, unit: "", descriptionText: "L2 Current: ${formattedValue} Amps", isStateChange: true)
				}
			}
            
 /*            
           else if (cmd.sourceEndPoint == 3) {
				if (encapsulatedCommand.scale == 4 ) {
						newValue = Math.round(encapsulatedCommand.scaledMeterValue)
	                    if (newValue > MAX_WATTS) { return }
						formattedValue = newValue
						dispValue = "${formattedValue}"
						sendEvent(name: "power3", value: dispValue as String, unit: "", descriptionText: "L3 Power: ${formattedValue} Watts", isStateChange: true)
				}
				if (encapsulatedCommand.scale == 5 ) {
						newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
	                    if (newValue > MAX_AMPS) { return }
						formattedValue = String.format("%5.2f", newValue)
						dispValue = "${formattedValue}"
						sendEvent(name: "current3", value: dispValue as String, unit: "", descriptionText: "L3 Current: ${formattedValue} Amps", isStateChange: true)
				}
			}
 */
		}
	}
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	//This will log any unhandled command output to the debug window.
	LOGDEBUG( "Unhandled command output: $cmd")
    createEvent(descriptionText: cmd.toString(), isStateChange: true)
}

def refresh() {
	def request = [
	//This is where the action "refresh" is defined. Refresh is very basic. It simply gets and displays the latest values from the HEM exclusive of the clamp subdevices.
		zwave.meterV3.meterGet(scale: 0),	//kWh
       	zwave.meterV3.meterGet(scale: 2),	//Wattage
		zwave.meterV3.meterGet(scale: 4),	//Volts
		zwave.meterV3.meterGet(scale: 5),	//Amps
 /*        
        zwave.configurationV1.configurationGet(parameterNumber: 2),
        zwave.configurationV1.configurationGet(parameterNumber: 3),
		zwave.configurationV1.configurationGet(parameterNumber: 4),
        zwave.configurationV1.configurationGet(parameterNumber: 5),
        zwave.configurationV1.configurationGet(parameterNumber: 6),
        zwave.configurationV1.configurationGet(parameterNumber: 7),
		zwave.configurationV1.configurationGet(parameterNumber: 8),
		zwave.configurationV1.configurationGet(parameterNumber: 101),
		zwave.configurationV1.configurationGet(parameterNumber: 102),
		zwave.configurationV1.configurationGet(parameterNumber: 103),
		zwave.configurationV1.configurationGet(parameterNumber: 111),
		zwave.configurationV1.configurationGet(parameterNumber: 112),
		zwave.configurationV1.configurationGet(parameterNumber: 113),
		zwave.associationV2.associationGet(groupingIdentifier: 1)

 */

	]
	commands(request)
}

def manualReset(){
  //  sendEvent(name:"Manual Reset", value: "",  descriptionText: "***** Manual Reset Initiated *****", isStateChange: false)
    
    reset()
        }


def reset() {
	
	//Tapping reset will send the meter reset command to HEM and zero out the kWh data so you can start fresh.
	//This will also clear the cost data and reset the last reset timestamp. Finally it will poll for latest values from the HEM.
	//This has no impact on Pole1 or Pole2 (clamp1 and clamp2 subdevice) data as that is sent via reports from the HEM.
  
    
 //   Define the date format (Default is d/M/YY - UK)
    
 //    def dateString = new Date().format("M/d/YY", location.timeZone)
  
    def dateString = new Date().format("d/M/YY", location.timeZone)

	def timeString = new Date().format("h:mm a", location.timeZone)    
	state.LastReset = dateString+" @ "+timeString
	sendEvent(name: "LastReset", value: state.LastReset, descriptionText: "***** Latest Reset *****", isStateChange: true)
     if(pushOver == true){summarise() }
	def request = [
		zwave.meterV3.meterReset(),
		zwave.meterV3.meterGet(scale: 0),	//kWh
		zwave.meterV3.meterGet(scale: 2),	//Wattage
		zwave.meterV3.meterGet(scale: 4),	//Volts
		zwave.meterV3.meterGet(scale: 5),	//Amps
    ]
	commands(request)
}

def summarise(){
    if(state.DisplayUnits == true){
 state.msg = "${device.displayName}"  +"\r\n" + "Summary Report"  +"\r\n" + "Generated: $state.LastReset  " +"\r\n" + "Cost this period: $state.Currency$state.TotalCost" +"\r\n" + "Energy this period: $state.TotalKwh."
    }
    if(state.DisplayUnits == false){
 state.msg = "${device.displayName}"  +"\r\n" + "Summary Report"  +"\r\n" + "Generated: $state.LastReset  " +"\r\n" + "Cost this period: $state.Currency$state.TotalCost" +"\r\n" + "Energy this period: $state.TotalKwh"+ " kwh"
    } 
    
 log.info "$state.msg"   
 speak(state.msg)   
}

def configure() {
	//This is where the tile action "configure" is defined. Configure resends the configuration commands below (using the variables set by the preferences section above) to the HEM Gen5 device.
	//If you're watching the debug log when you tap configure, you should see the full configuration report come back slowly over about a minute.
	//If you don't see the full configuration report (seven messages) followed by the association report, tap configure again.
    
   
     def totalWatts = 25
    if(totalThresh){
    	totalWatts =totalThresh.toInteger()
    }
    
    def clamp1Watts = 25
    if(c1Thresh){
    	clamp1Watts =c1Thresh.toInteger()
    }
     def clamp2Watts = 25
    if(c2Thresh){
    	clamp2Watts =c2Thresh.toInteger()
    }
     def clamp3Watts = 25
    if(c3Thresh){
    	clamp3Watts =c3Thresh.toInteger()
    }
    
	def monitorInt1 = 60
		if (monitorInterval1) {
			monitorInt1=monitorInterval1.toInteger()
		}
	def monitorInt2 = 30
		if (monitorInterval2) {
			monitorInt2=monitorInterval2.toInteger()
		}
	def monitorInt3 = 6
		if (monitorInterval3) {
			monitorInt3=monitorInterval3.toInteger()
	}
	LOGDEBUG( "Sending configure commands")
	def request = [
        
	//	zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: 1),
		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 0),
		// (DISABLED by first option) Don't send unless watts have changed by 50 <default>.
		zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: totalWatts),
        
    //    zwave.configurationV1.configurationSet(parameterNumber: 5, size: 2, scaledConfigurationValue: clamp1Watts),
    //    zwave.configurationV1.configurationSet(parameterNumber: 6, size: 2, scaledConfigurationValue: clamp2Watts),
    //    zwave.configurationV1.configurationSet(parameterNumber: 7, size: 2, scaledConfigurationValue: clamp3Watts),
		// (DISABLED by first option) Or by 10% <default>.
	//	zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: 5),

		// Which reports need to send in Report group 1.
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4144911),
		// Which reports need to send in Report group 2.
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 0),
		// Which reports need to send in Report group 3.
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0),
		// Interval to send Report group 1.
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: monitorInt1),
		// Interval to send Report group 2.
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: monitorInt2),
		// Interval to send Report group 3.
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: monitorInt3),

		// Report which configuration commands were sent to and received by the HEM Gen5 successfully.
        
  //      zwave.configurationV1.configurationGet(parameterNumber: 2),
//		zwave.configurationV1.configurationGet(parameterNumber: 3),
//		zwave.configurationV1.configurationGet(parameterNumber: 4),
 //       zwave.configurationV1.configurationGet(parameterNumber: 5),
 //       zwave.configurationV1.configurationGet(parameterNumber: 6),
 //       zwave.configurationV1.configurationGet(parameterNumber: 7),
//		zwave.configurationV1.configurationGet(parameterNumber: 8),
//		zwave.configurationV1.configurationGet(parameterNumber: 101),
//		zwave.configurationV1.configurationGet(parameterNumber: 102),
//		zwave.configurationV1.configurationGet(parameterNumber: 103),
//		zwave.configurationV1.configurationGet(parameterNumber: 111),
//		zwave.configurationV1.configurationGet(parameterNumber: 112),
//		zwave.configurationV1.configurationGet(parameterNumber: 113),
		zwave.associationV2.associationGet(groupingIdentifier: 1)
	]
	commands(request)
}

private setConfigured() {
	updateDataValue("configured", "true")
}

private isConfigured() {
	getDataValue("configured") == "true"
}

private command(hubitat.zwave.Command cmd) {
	if (state.sec) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private commands(commands, delay=500) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def speak(msg) {
    deviceNotification(msg)
    state.msg = ""
}

def getPriority(){
    [
    "LOW":-1,
    "NORMAL":0,
    "HIGH":1,
	]
    
}

def deviceNotification(message) {
    if(message.startsWith("[L]")){ 
        customPriority = "LOW"
        message = message.minus("[L]")
    }
    if(message.startsWith("[N]")){ 
        customPriority = "NORMAL"
        message = message.minus("[N]")
    }
    if(message.startsWith("[H]")){
        customPriority = "HIGH"
        message = message.minus("[H]")
    }
    if(customPriority){ priority = customPriority}
                       
    LOGDEBUG( "Sending Message: ${message} Priority: ${priority}")

  // Define the initial postBody keys and values for all messages
  def postBody = [
    token: "$apiKey",
    user: "$userKey",
    message: "${message}",
    priority: getPriority()[priority]
  ]

  // We only have to define the device if we are sending to a single device
  if (deviceName)
  {
    LOGDEBUG( "Sending Pushover to Device: $deviceName")
    postBody['device'] = "$deviceName"
  }
  else
  {
    LOGDEBUG( "Sending Pushover to All Devices")
  }

  // Prepare the package to be sent
  def params = [
    uri: "https://api.pushover.net/1/messages.json",
    contentType: "application/json",
	requestContentType: "application/x-www-form-urlencoded",
    body: postBody
  ]

  LOGDEBUG( postBody)

  if ((apiKey =~ /[A-Za-z0-9]{30}/) && (userKey =~ /[A-Za-z0-9]{30}/))
  {
    LOGDEBUG( "Sending Pushover: API key '${apiKey}' | User key '${userKey}'")
    httpPost(params){response ->
      if(response.status != 200)
      {
        sendPush("ERROR: 'Pushover Me When' received HTTP error ${response.status}. Check your keys!")
        log.error "Received HTTP error ${response.status}. Check your keys!"
      }
      else
      {
        LOGDEBUG( "HTTP response received [$response.status]")
      }
    }
  }
  else {
    // Do not sendPush() here, the user may have intentionally set up bad keys for testing.
    log.error "API key '${apiKey}' or User key '${userKey}' is not properly formatted!"
  }
    
}
def LOGDEBUG(txt){
    try {
    	if (settings.debugMode) { log.debug "Driver Version: ${state.Version} - ${txt}" }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}

def version(){
    unschedule()
    schedule("0 0 8 ? * FRI *", updateCheck)  
    updateCheck()
}

def updateCheck(){
    setVersion()
	def paramsUD = [uri: "http://update.hubitat.uk/cobra.json"]
       	try {
        httpGet(paramsUD) { respUD ->
 //  log.warn " Version Checking - Response Data: ${respUD.data}"   // Troubleshooting Debug Code 
       		def copyrightRead = (respUD.data.copyright)
       		state.Copyright = copyrightRead
            def newVerRaw = (respUD.data.versions.Driver.(state.InternalName))
            def newVer = (respUD.data.versions.Driver.(state.InternalName).replace(".", ""))
       		def currentVer = state.Version.replace(".", "")
      		state.UpdateInfo = (respUD.data.versions.UpdateInfo.Driver.(state.InternalName))
                state.author = (respUD.data.author)
           
		if(newVer == "NLS"){
            state.status = "<b>** This driver is no longer supported by $state.author  **</b>"       
            log.warn "** This driver is no longer supported by $state.author **"      
      		}           
		else if(currentVer < newVer){
        	state.status = "<b>New Version Available (Version: $newVerRaw)</b>"
        	log.warn "** There is a newer version of this driver available  (Version: $newVerRaw) **"
        	log.warn "** $state.UpdateInfo **"
       		} 
		else{ 
      		state.status = "Current"
      		log.info "You are using the current version of this driver"
       		}
      					}
        	} 
        catch (e) {
        	log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI -  $e"
    		}
    
   		if(state.status == "Current"){
			state.UpdateInfo = "N/A"
		    sendEvent(name: "DriverUpdate", value: state.UpdateInfo, isStateChange: true)
	 	    sendEvent(name: "DriverStatus", value: state.status, isStateChange: true)
			}
    	else{
	    	sendEvent(name: "DriverUpdate", value: state.UpdateInfo, isStateChange: true)
	     	sendEvent(name: "DriverStatus", value: state.status, isStateChange: true)
	    }   
 			sendEvent(name: "DriverAuthor", value: state.author, isStateChange: true)
    		sendEvent(name: "DriverVersion", value: state.Version, isStateChange: true)
    
    
    	//	
}

def setVersion(){
		state.Version = "1.2.0"
     state.InternalName = "AeonHEMV2"
}




