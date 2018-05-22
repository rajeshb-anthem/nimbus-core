/**
 *  Copyright 2016-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.antheminc.oss.nimbus.channel.web;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.antheminc.oss.nimbus.domain.cmd.Action;
import com.antheminc.oss.nimbus.domain.cmd.Behavior;
import com.antheminc.oss.nimbus.domain.cmd.Command;
import com.antheminc.oss.nimbus.domain.cmd.CommandBuilder;
import com.antheminc.oss.nimbus.domain.cmd.exec.CommandExecution.Output;
import com.antheminc.oss.nimbus.domain.cmd.exec.ExecutionContext;
import com.antheminc.oss.nimbus.domain.cmd.exec.ExecutionContextLoader;
import com.antheminc.oss.nimbus.domain.model.state.ModelEvent;
import com.antheminc.oss.nimbus.support.Holder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * STEPS to follow with examples. <br>
 * 
 * <p>EXAMPLES:</p>
 * Ex 1:  /anthem/icr/member/_search - POST operation which contains criteria <br>
 * Ex 2:  /anthem/icr/member/{id} - GET operation which has member id as path variable <br>
 * Ex 3:  /anthem/acmp/member/{id}/_info - GET operation which contains member id as path variable, but also contains an action 'info' to return less data <br>
 * Ex 4:  /anthem/acmp/um/case/{cid}/member/address/{aid} - GET operation to return one address from the collection <br>
 * Ex 5:  /anthem/acmp/um/case/{cid}/member/address		- POST operation to create new address <br>
 * Ex 6:  /anthem/icr/um/case/{cid}/member/address/{aid}?version=n - PUT operation to update a given address. This would be a complete model update <br>
 * Ex 7:  /anthem/icr/um/case/{cid}/member/address/{aid}?version=n - PATCH operation to update a given address. This would be a partial model update <br>
 * Ex 8:  /anthem/icr/member/_search$validate - POST operation which contains criteria which will be validated and response provided per field <br>
 * Ex 9:  /anthem/hrs/member/_search$config	 - GET operation that returns static meta data created for 'member/_search' process <br>
 * Ex 10: /anthem/hrs/um/case/{cid}/member/address$validate - POST operation that contains address field populated which will be validated at each field level <br>
 * Ex 11: /anthem/hrs/address$validate - POST operation
 * <br>
 * <br>
 * <p>STEPS:</p>
 * 1. Inject a list of available domain-aliases to consider
 * 2. Inject a list of available actions to consider
 * 3. Inject any rule to explicitly allow or prohibit a certain combination of: client-code + app-code + /p + domain-alias + action + behavior + RequestMethod.Type
 * 4. Inject patterns to follow<ol>
 * 	<li>/{app}/{domain-alias}/{id}<ul>
 * 		<li>a. domain-alias would be identified from injected list</li>
 * 		<li>b. {id} would be unrecognized and considered as refId for identified domain-alias</li>
 * 		<li>c. If the HTTP method is GET, then action would be "_get"</li>
 * 		<li>d. If the HTTP method is POST, then action would be "_create" and any RequestBody JSON object would be used</li>
 * 		<li>e. For DELETE - '_delete', PUT - '_replace', PATCH - '_update'. All these operations require mandatory 'version' parameter</li></ul></li>
 * 
 * 	<li>/{app}/{domain-alias-A}/{domain-alias-B}/{B_id}/.../{domain-alias-N}/{N_id}<ul>
 * 		<li>a. Detect all domain-aliases from the url and corresponding refId, if available</li>
 * 		<li>b. Follow same steps as above in '/{domain-alias}/{id}'</li></ul></li>
 * 
 * 	<li>/{app}/{domain-alias}/{_action}  -OR-  /{app}/{domain=alias}/{refId}/{_action}<ul>
 * 		<li>a. domain-alias would be identified from injected list</li>
 * 		<li>b. action would be identified from injected list</li>
 * 		<li>c. Follow same steps as above for recurring domain-alias identification</li></ul></li>
 * 
 * 	<li>/{app}/{domain-alias}/{_action}.{$behavior} OR /{app}/{domain-alias}{$behavior}<ul>
 * 		<li></li></ul></li></ol>
 * 
 * 5. Action can appear only once and at the end of the url but prior to Behavior, if any
 * 6. Action if explicitly provided, overrides inferred action. Ex: /{app}/{domain-alias}/{id}/_delete GET : would be treated as '_delete' action and not '_get' inferred from GET methods
 * 7. GET/_get and DELETE/_delete only accepts {refId} as argument and not any RequestBody
 * 8. Behavior can appear only once at the end or url prefixed by '.' Ex: .../../../_search$config OR ../../_search$validate
 * 			
 * @author Soham Chakravarti
 *
 */
@RestController
//@EnableResourceServer
public class WebActionController {
	
	public static final String URI_PATTERN_P = "/{clientCode}/**/p";
	public static final String URI_PATTERN_P_OPEN = URI_PATTERN_P + "/**";

	@Autowired WebCommandDispatcher dispatcher;
	
	@Autowired ExecutionContextLoader ctxLoader;
	
	@RequestMapping(value=URI_PATTERN_P+"/clear", produces="application/json", method=RequestMethod.GET)
	public void clear() {
		ctxLoader.clear();
	}
	
	/**
	 * Usage Example: /client/org/p/loglevel?level=debug&package=com.antheminc
	 * 
	 * @param level
	 * @param packageName
	 * @return
	 */
	@RequestMapping(value=URI_PATTERN_P+"/loglevel", produces="application/json", method=RequestMethod.GET)
	public Output<String> updateLogLevel(@RequestParam(value="level") String level, @RequestParam(value="package") String packageName) {
		String status = "Log Level updated to - " + level;
		String uri = URI_PATTERN_P+"/loglevel";
		Command cmd = CommandBuilder.withUri(uri).getCommand();
		
		Output<String> returnStatus = new Output<>(uri, new ExecutionContext(cmd), Action._get, Behavior.$execute);
		returnStatus.setValue(status);
		
		LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        if (level.equalsIgnoreCase("DEBUG")) {
            loggerContext.getLogger(packageName).setLevel(Level.DEBUG);
            return returnStatus; 
        } else if (level.equalsIgnoreCase("INFO")) {
            loggerContext.getLogger(packageName).setLevel(Level.INFO);
            return returnStatus; 
        } else if (level.equalsIgnoreCase("TRACE")) {
            loggerContext.getLogger(packageName).setLevel(Level.TRACE);
            return returnStatus; 
        } else if (level.equalsIgnoreCase("WARN")) {
            loggerContext.getLogger(packageName).setLevel(Level.WARN);
            return returnStatus; 
        } else if (level.equalsIgnoreCase("ERROR")) {
            loggerContext.getLogger(packageName).setLevel(Level.ERROR);
            return returnStatus; 
        } else if (level.equalsIgnoreCase("OFF")) {
            loggerContext.getLogger(packageName).setLevel(Level.OFF);
            return returnStatus; 
        } else if (level.equalsIgnoreCase("ALL")) {
            loggerContext.getLogger(packageName).setLevel(Level.ALL);
            return returnStatus; 
        } else {
        	returnStatus.setValue("Invalid Log Level - " + level);
            return returnStatus;
        }
	}
	
	@RequestMapping(value=URI_PATTERN_P_OPEN, produces="application/json", method=RequestMethod.GET)
	public Object handleGet(HttpServletRequest req, @RequestParam(required=false) String a) {
		return handleInternal(req, RequestMethod.GET, null, a);
	}
	
	@RequestMapping(value=URI_PATTERN_P_OPEN, produces="application/json", method=RequestMethod.DELETE)
	public Object handleDelete(HttpServletRequest req, @RequestParam String v) {
		return handleInternal(req, RequestMethod.DELETE, v, null);
	}
	
	@RequestMapping(value=URI_PATTERN_P_OPEN, produces="application/json", method=RequestMethod.POST)
	public Object handlePost(HttpServletRequest req, @RequestBody String json) { 
		return handleInternal(req, RequestMethod.POST, null, json);
	}
	
	@RequestMapping(value=URI_PATTERN_P_OPEN, produces="application/json", method=RequestMethod.PUT)
	public Object handlePut(HttpServletRequest req, @RequestParam String v, @RequestBody String json) {  
		return handleInternal(req, RequestMethod.PUT, v, json);
	}
	
	@RequestMapping(value=URI_PATTERN_P_OPEN, produces="application/json", method=RequestMethod.PATCH)
	public Object handlePatch(HttpServletRequest req, @RequestParam String v, @RequestBody String json) {  
		return handleInternal(req, RequestMethod.PATCH, v, json);
	}
	
	@RequestMapping(value=URI_PATTERN_P+"/event/notify", produces="application/json", method=RequestMethod.POST)
	public Object handleEventNotify(HttpServletRequest req, @RequestBody ModelEvent<String> event) {
		Object obj = dispatcher.handle(req, event);
		
		Holder<Object> output = new Holder<>(obj);
		return output;
	}
	
	@RequestMapping({"/login/*"})
	public ResponseEntity<?> login(){
		return ResponseEntity.status(HttpStatus.OK).body(null);
	}
	
	
	protected Object handleInternal(HttpServletRequest req, RequestMethod httpMethod, String v, String json) {
		Object obj = dispatcher.handle(req, json);
		Holder<Object> output = new Holder<>(obj);
		return output;
	}
}
