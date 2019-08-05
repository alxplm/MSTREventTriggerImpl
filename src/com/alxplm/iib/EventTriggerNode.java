package com.alxplm.iib;

import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputTerminal;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbNode;
import com.ibm.broker.plugin.MbNodeInterface;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbPolicy;
import com.ibm.broker.plugin.MbUserException;
import com.microstrategy.web.objects.WebFolder;
import com.microstrategy.web.objects.WebIServerSession;
import com.microstrategy.web.objects.WebObjectSource;
import com.microstrategy.web.objects.WebObjectsException;
import com.microstrategy.web.objects.WebObjectsFactory;
import com.microstrategy.web.objects.WebScheduleEvent;
import com.microstrategy.web.objects.WebSearch;
import com.microstrategy.webapi.EnumDSSXMLApplicationType;
import com.microstrategy.webapi.EnumDSSXMLObjectTypes;
import com.microstrategy.webapi.EnumDSSXMLSearchDomain;

public class EventTriggerNode extends MbNode implements MbNodeInterface {

	private String eventName = null;
	private String policyName = null;
	private String policyProject = null;
	

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	
	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	public String getPolicyProject() {
		return policyProject;
	}

	public void setPolicyProject(String policyProject) {
		this.policyProject = policyProject;
	}

	public static String getNodeName() {
		return "com_alxplm_iib_EventTriggerNode";
	}
	
	public EventTriggerNode() throws MbException {
		createInputTerminal("in");
		createOutputTerminal("failure");
		createOutputTerminal("out");
	}

	@Override
	public void evaluate(MbMessageAssembly assembly, MbInputTerminal arg1)
			throws MbException {
		
		MbMessage newMsg = new MbMessage(assembly.getMessage());
		MbMessageAssembly newAssembly = new MbMessageAssembly(assembly, newMsg);
		
		String policy = "{DefaultPolicies}:" + policyName;
		if(policyProject != null) {
			policy = "{" + policyProject + "}:" + policyName;
		} 
		
		MbPolicy myUserPolicy = getPolicy("UserDefined", policy);
		String serverName = myUserPolicy.getPropertyValueAsString("ServerName");
		String projectName = myUserPolicy.getPropertyValueAsString("ProjectName");
		String login = myUserPolicy.getPropertyValueAsString("Login");
		String password = myUserPolicy.getPropertyValueAsString("Password");
		
		
		WebIServerSession sessionInfo = null;
		
		try {
			
			WebObjectsFactory factory = WebObjectsFactory.getInstance();
			sessionInfo = factory.getIServerSession();
	        sessionInfo.setServerName(serverName);
	        sessionInfo.setProjectName(projectName);
	        sessionInfo.setLogin(login);
	        sessionInfo.setPassword(password);
	        sessionInfo.setApplicationType(EnumDSSXMLApplicationType.DssXmlApplicationCustomApp);
	        sessionInfo.getSessionID();
	        WebObjectSource source = factory.getObjectSource();
	        
	        WebSearch search = null;
	        search = source.getNewSearchObject();
	        search.setNamePattern(eventName);
	        search.setAsync(false);
	        search.types().add(EnumDSSXMLObjectTypes.DssXmlTypeScheduleEvent);
	        search.setDomain(EnumDSSXMLSearchDomain.DssXmlSearchDomainConfiguration);
	        search.submit();
	        WebFolder folder = search.getResults();
	        
	        WebScheduleEvent event = null;
	        if (folder.size() > 0) {
	            event = (WebScheduleEvent) folder.get(0);
	        } else {
	        	throw new Exception("Unknown event");
	        }
	        event.trigger();
	        
	        MbOutputTerminal outOutputTerminal = getOutputTerminal("out");
	        outOutputTerminal.propagate(newAssembly);
			
		} catch (Exception e) {
			throw new MbUserException(this, "evaluate()", "", "", e.toString(), null);
		} finally {
			if(sessionInfo != null) {
				try {
					sessionInfo.closeSession();
				} catch (WebObjectsException e) {
					throw new MbUserException(this, "evaluate()", "", "", e.toString(), null);
				}
			}
		}
	}
}
